package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.data.models.DataDayData
import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.ExpectedDrinkType
import com.alcodb.optimizer.feature_scan.domain.models.FoundCollection
import com.alcodb.optimizer.feature_scan.domain.models.LowActivityPeriod
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchCriteria
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchResult
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/**
 * Поиск коллекций, подходящих под критерии потерянных данных.
 */
internal class LostDataSearchService(
    private val scanDataSource: ScanDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val dispatchers: Dispatchers,
    private val collections: CollectionsProvider,
) {
    suspend fun searchLostData(
        criteria: LostDataSearchCriteria,
        maxReads: Int,
    ): Result<LostDataSearchResult, Error> = withContext(dispatchers.io) {
        try {
            if (!firestoreDataSource.isInitialized()) {
                return@withContext Result.Error(ScanError.FirebaseNotInitialized)
            }

            val userCollections = when (val fetched = collections.getCollections()) {
                is Result.Error -> return@withContext Result.Error(ScanError.FailedToReadDatabase)
                is Result.Success -> fetched.data
            }
            val filter = criteria.collectionFilter.trim()
            val candidates = if (filter.isEmpty()) {
                userCollections
            } else {
                userCollections.filter { it.contains(filter, ignoreCase = true) }
            }
            val limit = maxReads.coerceAtLeast(1)
            val found = mutableListOf<FoundCollection>()
            var checked = 0
            var stoppedByLimit = false

            println("[LostSearch] Collections to check: ${candidates.size}, limit: $limit")

            for (collectionId in candidates) {
                if (checked >= limit) {
                    stoppedByLimit = true
                    break
                }

                val datesResult = withTimeoutOrNull(15_000L) {
                    scanDataSource.getUserDates(collectionId)
                }
                checked++
                when (datesResult) {
                    is Result.Success -> matchCollection(collectionId, datesResult.data, criteria)
                        ?.let(found::add)
                    is Result.Error ->
                        println("[LostSearch] $collectionId — ERROR: ${datesResult.error}")
                    null -> println("[LostSearch] $collectionId — timeout")
                }

                delay(5)
            }

            println("[LostSearch] Checked $checked/${candidates.size}, found: ${found.size}")

            Result.Success(
                LostDataSearchResult(
                    collections = found,
                    checkedCollections = checked,
                    totalCollections = candidates.size,
                    stoppedByLimit = stoppedByLimit,
                ),
            )
        } catch (e: Exception) {
            println("[LostSearch] FATAL ERROR: ${e.message}")
            e.printStackTrace()
            Result.Error(ScanError.FailedToReadDatabase)
        }
    }

    private fun matchCollection(
        collectionId: String,
        dates: Map<String, DataDayData>,
        criteria: LostDataSearchCriteria,
    ): FoundCollection? {
        val markedDays = dates.mapNotNull { (key, value) ->
            val drinkType = value.drinkType
                ?.takeIf { it.equals("Full", ignoreCase = true) || it.equals("Half", ignoreCase = true) }
                ?: return@mapNotNull null
            val date = epochDayToDate(value.date)
                ?: runCatching { LocalDate.parse(key) }.getOrNull()
                ?: return@mapNotNull null
            date to drinkType
        }
        if (markedDays.isEmpty()) return null

        val sortedDays = markedDays.distinctBy { it.first }.sortedBy { it.first }
        val firstDate = sortedDays.first().first
        val lastDate = sortedDays.last().first

        if (!matchesBoundary(
                actual = firstDate,
                expected = criteria.startDate,
                exact = criteria.startExact,
                monthTolerance = criteria.startMonthTolerance,
            )
        ) return null

        if (!matchesBoundary(
                actual = lastDate,
                expected = criteria.endDate,
                exact = criteria.endExact,
                monthTolerance = criteria.endMonthTolerance,
            )
        ) return null

        for (expected in criteria.expectedDays) {
            val matched = sortedDays.any { (date, drinkType) ->
                matchesDrinkType(drinkType, expected.drinkType) &&
                    matchesDay(date, expected.date, expected.exact, expected.dayTolerance)
            }
            if (!matched) return null
        }

        var peakStreak = 0
        val peakDays = criteria.peakMonth?.let { peak ->
            val peakKey = monthIndex(peak.month)
            val monthDays = sortedDays.filter { monthIndex(it.first) == peakKey }
            if (monthDays.size < peak.minDrinkDays) return null
            if (peak.maxDrinkDays > 0 && monthDays.size > peak.maxDrinkDays) return null
            peakStreak = longestStreak(monthDays.map { it.first })
            if (peakStreak < peak.minConsecutiveDays) return null
            monthDays.size
        } ?: 0

        val lowActivity = criteria.lowActivityPeriod
        if (lowActivity != null && !matchesLowActivity(sortedDays, lowActivity)) return null

        return FoundCollection(
            id = collectionId,
            totalDays = dates.size,
            drinkDays = sortedDays.size,
            firstDate = firstDate,
            lastDate = lastDate,
            matchedExpectedDays = criteria.expectedDays.size,
            peakDrinkDays = peakDays,
            maxConsecutiveDays = peakStreak,
        )
    }

    private fun matchesDrinkType(actual: String, expected: ExpectedDrinkType): Boolean =
        expected == ExpectedDrinkType.Any || actual.equals(expected.name, ignoreCase = true)

    private fun matchesLowActivity(
        markedDays: List<Pair<LocalDate, String>>,
        period: LowActivityPeriod,
    ): Boolean {
        val from = monthIndex(period.fromMonth)
        val to = monthIndex(period.toMonth)
        if (from > to) return false
        val perMonth = markedDays.groupingBy { monthIndex(it.first) }.eachCount()
        return (from..to).all { (perMonth[it] ?: 0) <= period.maxDrinkDaysPerMonth }
    }

    private fun monthIndex(date: LocalDate): Int = date.year * 12 + date.month.ordinal

    private fun longestStreak(dates: List<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.map { it.toEpochDays() }.distinct().sorted()
        var best = 1
        var current = 1
        for (index in 1 until sorted.size) {
            if (sorted[index] - sorted[index - 1] == 1L) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }

    private fun matchesBoundary(
        actual: LocalDate,
        expected: LocalDate?,
        exact: Boolean,
        monthTolerance: Int,
    ): Boolean {
        if (expected == null) return true
        if (exact || monthTolerance <= 0) return actual == expected
        val actualIndex = actual.year * 12 + actual.month.ordinal
        val expectedIndex = expected.year * 12 + expected.month.ordinal
        return actualIndex - expectedIndex in -monthTolerance..monthTolerance
    }

    private fun matchesDay(
        actual: LocalDate,
        expected: LocalDate,
        exact: Boolean,
        dayTolerance: Int,
    ): Boolean {
        if (exact || dayTolerance <= 0) return actual == expected
        return abs(actual.toEpochDays() - expected.toEpochDays()) <= dayTolerance
    }

    private fun epochDayToDate(epochDays: Long): LocalDate? = try {
        if (epochDays <= 0L) null else LocalDate.fromEpochDays(epochDays.toInt())
    } catch (_: Exception) {
        null
    }
}
