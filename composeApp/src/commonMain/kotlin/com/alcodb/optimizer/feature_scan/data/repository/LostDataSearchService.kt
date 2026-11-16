package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.data.models.DataDayData
import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.FoundCollection
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
            val limit = maxReads.coerceAtLeast(1)
            val found = mutableListOf<FoundCollection>()
            var checked = 0
            var stoppedByLimit = false

            println("[LostSearch] Collections to check: ${userCollections.size}, limit: $limit")

            for (collectionId in userCollections) {
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

            println("[LostSearch] Checked $checked/${userCollections.size}, found: ${found.size}")

            Result.Success(
                LostDataSearchResult(
                    collections = found,
                    checkedCollections = checked,
                    totalCollections = userCollections.size,
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

        val firstDate = markedDays.minOf { it.first }
        val lastDate = markedDays.maxOf { it.first }

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
            val matched = markedDays.any { (date, drinkType) ->
                drinkType.equals(expected.drinkType.name, ignoreCase = true) &&
                    matchesDay(date, expected.date, expected.exact, expected.dayTolerance)
            }
            if (!matched) return null
        }

        return FoundCollection(
            id = collectionId,
            totalDays = dates.size,
            drinkDays = markedDays.size,
            firstDate = firstDate,
            lastDate = lastDate,
            matchedExpectedDays = criteria.expectedDays.size,
        )
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
