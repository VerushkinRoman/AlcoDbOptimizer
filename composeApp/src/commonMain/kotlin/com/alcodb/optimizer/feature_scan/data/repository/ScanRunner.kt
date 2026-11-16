package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.data.datasource.LocalStorageDataSource
import com.alcodb.optimizer.common.data.models.CalendarData
import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.domain.utils.getOrNull
import com.alcodb.optimizer.common.domain.utils.onError
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.models.RegistryData
import com.alcodb.optimizer.feature_scan.domain.repository.ScanProgressState
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Проход по коллекциям: накопление прогресса, поиск неактивных пользователей и
 * сохранение промежуточного состояния.
 */
internal class ScanRunner(
    private val scanDataSource: ScanDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val localStorageDataSource: LocalStorageDataSource,
    private val dispatchers: Dispatchers,
    private val state: ScanStateStore,
    private val collections: CollectionsProvider,
    private val detector: InactiveUserDetector,
) {
    suspend fun startScan(maxReads: Int): EmptyResult<Error> = withContext(dispatchers.io) {
        val effectiveMaxReads = maxReads.coerceAtLeast(1)
        try {
            if (!firestoreDataSource.isInitialized()) {
                return@withContext Result.Error(ScanError.FirebaseNotInitialized)
            }

            val today = getTodayStart()
            val lastScanDate = localStorageDataSource.getLastScanDate().getOrNull() ?: 0L

            if (lastScanDate >= today) {
                val readsUsed = localStorageDataSource.getReadsUsedToday().getOrNull() ?: 0
                if (readsUsed >= effectiveMaxReads) {
                    return@withContext Result.Error(ScanError.DailyLimitReached)
                }
            } else {
                localStorageDataSource.setReadsUsedToday(0).onError { logPersistenceFailure(it) }
            }

            val processedJson = localStorageDataSource.getProcessedCollections().getOrNull() ?: "[]"
            val processed = try {
                state.json.decodeFromString<List<String>>(processedJson)
            } catch (_: Exception) {
                emptyList()
            }

            val userCollections = when (val fetched = collections.getCollections()) {
                is Result.Error -> {
                    println("[Scan] Failed to list collections")
                    return@withContext Result.Error(ScanError.FailedToReadDatabase)
                }
                is Result.Success -> fetched.data
            }

            println("[Scan] User collections to scan: ${userCollections.size}")

            var readsUsed = if (lastScanDate >= today) {
                localStorageDataSource.getReadsUsedToday().getOrNull() ?: 0
            } else {
                0
            }
            val results = state.loadSavedUsers().toMutableList()
            val processedSet = processed.toMutableSet()

            state.progress = ScanProgressState(
                isScanning = true,
                collectionsTotal = userCollections.size,
                collectionsProcessed = processedSet.size,
                readsUsed = readsUsed,
                maxReads = effectiveMaxReads,
                lastScanDate = lastScanDate,
                canScanToday = readsUsed < effectiveMaxReads,
            )

            for ((index, collectionId) in userCollections.withIndex()) {
                if (collectionId in processedSet) continue

                if (readsUsed >= effectiveMaxReads) {
                    println(
                        "[Scan] Daily limit reached: $readsUsed/$effectiveMaxReads after processing " +
                            "${processedSet.size} collections, inactive found: ${results.size}",
                    )
                    state.persistProgress(processedSet, results, readsUsed, today)
                    state.progress = state.progress.copy(
                        isScanning = false,
                        readsUsed = readsUsed,
                        collectionsProcessed = processedSet.size,
                    )
                    return@withContext Result.Error(ScanError.DailyLimitReached)
                }

                val datesResult = try {
                    withTimeoutOrNull(15_000L) {
                        scanDataSource.getUserDates(collectionId)
                    }
                } catch (e: Exception) {
                    println("[Scan] [$index/${userCollections.size}] $collectionId — ERROR: ${e.message}")
                    null
                }
                readsUsed++

                val datesError = datesResult as? Result.Error
                if (datesError != null) {
                    println(
                        "[Scan] [$index/${userCollections.size}] $collectionId — " +
                            "ERROR: ${datesError.error}",
                    )
                } else {
                    val dates: CalendarData? = (datesResult as? Result.Success)?.data
                    val isAnonymous = !collectionId.contains("@")
                    val inactiveUser = detector.detect(collectionId, dates ?: emptyMap(), isAnonymous)
                    if (inactiveUser != null) {
                        results.add(hydrateRegistry(collectionId, inactiveUser, isAnonymous))
                    }

                    if (index < 5 || index % 100 == 0) {
                        val suffix = if (dates != null) {
                            "${dates.size} dates, inactive=${inactiveUser != null}"
                        } else {
                            "no dates/timeout, inactive=true"
                        }
                        println("[Scan] [$index/${userCollections.size}] $collectionId — $suffix, reads=$readsUsed")
                    }
                }

                processedSet.add(collectionId)
                state.progress = state.progress.copy(
                    collectionsProcessed = processedSet.size,
                    readsUsed = readsUsed,
                )

                if (processedSet.size % 10 == 0) {
                    state.persistProgress(processedSet, results, readsUsed, today)
                }

                delay(10)
            }

            println(
                "[Scan] Scan loop finished. Processed: ${processedSet.size}/${userCollections.size}, " +
                    "reads: $readsUsed, inactive: ${results.size}",
            )

            val allDone = processedSet.size >= userCollections.size
            state.persistProgress(processedSet, results, readsUsed, today)

            if (allDone) {
                println("[Scan] All collections checked. Clearing processed list for next cycle.")
                localStorageDataSource.clearProcessedCollections().onError { logPersistenceFailure(it) }
                localStorageDataSource.setReadsUsedToday(0).onError { logPersistenceFailure(it) }
                localStorageDataSource.setLastScanDate(0L).onError { logPersistenceFailure(it) }
                localStorageDataSource.setCachedCollections("[]").onError { logPersistenceFailure(it) }
            }

            state.progress = state.progress.copy(
                isScanning = false,
                collectionsProcessed = processedSet.size,
                readsUsed = readsUsed,
                isComplete = allDone,
            )

            EmptySuccessResult
        } catch (e: Exception) {
            println("[Scan] FATAL ERROR: ${e.message}")
            e.printStackTrace()
            state.progress = state.progress.copy(isScanning = false)
            Result.Error(ScanError.FailedToReadDatabase)
        }
    }

    suspend fun stopScan(): EmptyResult<Error> = withContext(dispatchers.io) {
        state.progress = state.progress.copy(isScanning = false)
        EmptySuccessResult
    }

    suspend fun refreshInactiveUsers(): EmptyResult<Error> = withContext(dispatchers.io) {
        state.inactiveUsers = state.loadSavedUsers()
        EmptySuccessResult
    }

    suspend fun resetCounter(): EmptyResult<Error> = withContext(dispatchers.io) {
        localStorageDataSource.setReadsUsedToday(0).onError { logPersistenceFailure(it) }
        localStorageDataSource.setLastScanDate(0L).onError { logPersistenceFailure(it) }
        state.progress = ScanProgressState()
        EmptySuccessResult
    }

    suspend fun resetAll(): EmptyResult<Error> = withContext(dispatchers.io) {
        localStorageDataSource.clearProgress().onError { logPersistenceFailure(it) }
        state.inactiveUsers = emptyList()
        state.progress = ScanProgressState()
        EmptySuccessResult
    }

    private suspend fun hydrateRegistry(
        collectionId: String,
        user: InactiveUser,
        isAnonymous: Boolean,
    ): InactiveUser {
        if (isAnonymous) return user
        val regData = scanDataSource.getRegistryUserData(collectionId).getOrNull() ?: return user
        return user.copy(
            registryData = RegistryData(
                nickname = regData["nickname"] as? String ?: "",
                email = regData["email"] as? String ?: collectionId,
                locale = regData["locale"] as? String ?: "",
                token = regData["token"] as? String ?: "",
                donations = (regData["donations"] as? Number)?.toDouble() ?: 0.0,
            ),
        )
    }

    private fun logPersistenceFailure(error: Error) {
        println("[Scan] Failed to persist scan state: $error")
    }

    private fun getTodayStart(): Long {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        return today.toEpochDays() * 86_400_000L
    }
}
