package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.data.datasource.LocalStorageDataSource
import com.alcodb.optimizer.common.data.models.DataDayData
import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.utils.getBackupDir
import com.alcodb.optimizer.common.utils.readFromFile
import com.alcodb.optimizer.common.utils.writeToFile
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.repository.ScanProgressState
import com.alcodb.optimizer.feature_scan.domain.repository.ScanRepository
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class ScanRepositoryImpl(
    private val scanDataSource: ScanDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val localStorageDataSource: LocalStorageDataSource,
    private val dispatchers: Dispatchers,
) : ScanRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val _progress = MutableStateFlow(ScanProgressState())
    private val _inactiveUsers = MutableStateFlow<List<InactiveUser>>(emptyList())
    private val _deletionProgress = MutableStateFlow<Pair<Int, Int>>(0 to 0)

    override fun getDeletionProgress(): Flow<Pair<Int, Int>> = _deletionProgress.asStateFlow()

    override fun getInactiveUsers(): Flow<Result<List<InactiveUser>, Error>> = flow {
        val savedUsers = loadSavedUsers()
        if (savedUsers.isNotEmpty()) {
            _inactiveUsers.value = savedUsers
            emit(Result.Success(savedUsers))
        }
        _inactiveUsers.collect { users ->
            emit(Result.Success(users))
        }
    }

    override fun getScanProgress(): Flow<ScanProgressState> = _progress.asStateFlow()

    override suspend fun startScan(maxReads: Int): EmptyResult<Error> = withContext(dispatchers.io) {
        val effectiveMaxReads = maxReads.coerceAtLeast(1)
        try {
            if (!firestoreDataSource.isInitialized()) {
                return@withContext Result.Error(ScanError.FirebaseNotInitialized)
            }

            val today = getTodayStart()
            val lastScanDate = localStorageDataSource.getLastScanDate()

            if (lastScanDate >= today) {
                val readsUsed = localStorageDataSource.getReadsUsedToday()
                if (readsUsed >= effectiveMaxReads) {
                    return@withContext Result.Error(ScanError.DailyLimitReached)
                }
            } else {
                localStorageDataSource.setReadsUsedToday(0)
            }

            val processedJson = localStorageDataSource.getProcessedCollections()
            val processed = try {
                json.decodeFromString<List<String>>(processedJson)
            } catch (_: Exception) {
                emptyList()
            }

            val userCollections = try {
                val allCollections = scanDataSource.getAllCollections()
                println("[Scan] Total collections found: ${allCollections.size}")
                println("[Scan] Collections: ${allCollections.take(30).joinToString()}")
                val systemCollections = setOf(COLLECTION_USERS)
                allCollections
                    .filter { it !in systemCollections }
                    .sorted()
            } catch (e: Exception) {
                println("[Scan] Failed to list collections: ${e.message}")
                return@withContext Result.Error(ScanError.FailedToReadDatabase)
            }

            println("[Scan] User collections to scan: ${userCollections.size}")

            var readsUsed = if (lastScanDate >= today) localStorageDataSource.getReadsUsedToday() else 0
            val results = loadSavedUsers().toMutableList()
            val processedSet = processed.toMutableSet()

            _progress.value = ScanProgressState(
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
                    println("[Scan] Daily limit reached: $readsUsed/$effectiveMaxReads after processing ${processedSet.size} collections, inactive found: ${results.size}")
                    localStorageDataSource.setLastScanDate(today)
                    localStorageDataSource.setReadsUsedToday(readsUsed)
                    localStorageDataSource.setProcessedCollections(json.encodeToString(processedSet.toList()))
                    localStorageDataSource.setInactiveUsers(json.encodeToString(results))
                    _inactiveUsers.value = results
                    _progress.value = _progress.value.copy(
                        isScanning = false,
                        readsUsed = readsUsed,
                        collectionsProcessed = processedSet.size,
                    )
                    return@withContext Result.Error(ScanError.DailyLimitReached)
                }

                try {
                    val dates = withTimeoutOrNull(15_000L) {
                        scanDataSource.getUserDates(collectionId)
                    }
                    readsUsed++

                    if (dates != null) {
                        val isAnonymous = !collectionId.contains("@")
                        val inactiveUser = checkInactiveUser(collectionId, dates, isAnonymous)
                        if (inactiveUser != null) {
                            val userWithRegistry = if (!isAnonymous) {
                                val regData = scanDataSource.getRegistryUserData(collectionId)
                                if (regData != null) {
                                    inactiveUser.copy(
                                        registryData = com.alcodb.optimizer.feature_scan.domain.models.RegistryData(
                                            nickname = regData["nickname"] as? String ?: "",
                                            email = regData["email"] as? String ?: collectionId,
                                            locale = regData["locale"] as? String ?: "",
                                            token = regData["token"] as? String ?: "",
                                            donations = (regData["donations"] as? Number)?.toDouble() ?: 0.0,
                                        )
                                    )
                                } else inactiveUser
                            } else inactiveUser
                            results.add(userWithRegistry)
                        }
                        if (index < 5 || index % 100 == 0) {
                            println("[Scan] [$index/${userCollections.size}] $collectionId — ${dates.size} dates, inactive=${inactiveUser != null}, reads=$readsUsed")
                        }
                    } else {
                        val isAnonymous = !collectionId.contains("@")
                        val inactiveUser = checkInactiveUser(collectionId, emptyMap(), isAnonymous)
                        if (inactiveUser != null) {
                            val userWithRegistry = if (!isAnonymous) {
                                val regData = scanDataSource.getRegistryUserData(collectionId)
                                if (regData != null) {
                                    inactiveUser.copy(
                                        registryData = com.alcodb.optimizer.feature_scan.domain.models.RegistryData(
                                            nickname = regData["nickname"] as? String ?: "",
                                            email = regData["email"] as? String ?: collectionId,
                                            locale = regData["locale"] as? String ?: "",
                                            token = regData["token"] as? String ?: "",
                                            donations = (regData["donations"] as? Number)?.toDouble() ?: 0.0,
                                        )
                                    )
                                } else inactiveUser
                            } else inactiveUser
                            results.add(userWithRegistry)
                        }
                        if (index < 5 || index % 100 == 0) {
                            println("[Scan] [$index/${userCollections.size}] $collectionId — no dates/timeout, inactive=true, reads=$readsUsed")
                        }
                    }
                } catch (e: Exception) {
                    readsUsed++
                    println("[Scan] [$index/${userCollections.size}] $collectionId — ERROR: ${e.message}")
                    if (index < 5 || index % 100 == 0) {
                        e.printStackTrace()
                    }
                }

                processedSet.add(collectionId)

                _progress.value = _progress.value.copy(
                    collectionsProcessed = processedSet.size,
                    readsUsed = readsUsed,
                )

                if (processedSet.size % 10 == 0) {
                    localStorageDataSource.setProcessedCollections(json.encodeToString(processedSet.toList()))
                    localStorageDataSource.setInactiveUsers(json.encodeToString(results))
                    localStorageDataSource.setReadsUsedToday(readsUsed)
                    localStorageDataSource.setLastScanDate(today)
                }

                delay(10)
            }

            println("[Scan] Scan loop finished. Processed: ${processedSet.size}/${userCollections.size}, reads: $readsUsed, inactive: ${results.size}")

            val allDone = processedSet.size >= userCollections.size

            localStorageDataSource.setProcessedCollections(json.encodeToString(processedSet.toList()))
            localStorageDataSource.setInactiveUsers(json.encodeToString(results))
            localStorageDataSource.setReadsUsedToday(readsUsed)
            localStorageDataSource.setLastScanDate(today)

            _inactiveUsers.value = results

            if (allDone) {
                println("[Scan] All collections checked. Clearing processed list for next cycle.")
                localStorageDataSource.clearProcessedCollections()
                localStorageDataSource.setReadsUsedToday(0)
                localStorageDataSource.setLastScanDate(0L)
            }

            _progress.value = _progress.value.copy(
                isScanning = false,
                collectionsProcessed = processedSet.size,
                readsUsed = readsUsed,
                isComplete = allDone,
            )

            EmptySuccessResult
        } catch (e: Exception) {
            println("[Scan] FATAL ERROR: ${e.message}")
            e.printStackTrace()
            _progress.value = _progress.value.copy(isScanning = false)
            Result.Error(ScanError.FailedToReadDatabase)
        }
    }

    override suspend fun stopScan(): EmptyResult<Error> = withContext(dispatchers.io) {
        _progress.value = _progress.value.copy(isScanning = false)
        EmptySuccessResult
    }

    override suspend fun deleteAllInactiveUsers(): EmptyResult<Error> = withContext(dispatchers.io) {
        try {
            val users = _inactiveUsers.value
            if (users.isEmpty()) return@withContext EmptySuccessResult

            val registeredUsers = users.filter { !it.isAnonymous }
            if (registeredUsers.isNotEmpty()) {
                val backupJson = json.encodeToString(registeredUsers)
                val backupDir = getBackupDir()
                val timestamp = Clock.System.now().toEpochMilliseconds()
                val backupFile = "$backupDir/backup_registered_$timestamp.json"
                writeToFile(backupFile, backupJson)
            }

            val total = users.size
            _deletionProgress.value = 0 to total

            for ((index, user) in users.withIndex()) {
                try {
                    scanDataSource.deleteUserCollection(user.id)
                    if (user.isAnonymous) {
                        scanDataSource.deleteAuthUser(user.id)
                    } else {
                        scanDataSource.removeUserFromGlobalRegistry(user.id)
                        scanDataSource.deleteAuthUserByEmail(user.id)
                    }
                } catch (e: Exception) {
                    println("[Delete] Failed to delete ${user.id}: ${e.message}")
                }
                _deletionProgress.value = (index + 1) to total
                delay(100)
            }

            _inactiveUsers.value = emptyList()
            localStorageDataSource.setInactiveUsers("[]")
            _deletionProgress.value = 0 to 0

            EmptySuccessResult
        } catch (e: Exception) {
            println("[Delete] deleteAllInactiveUsers ERROR: ${e.message}")
            e.printStackTrace()
            _deletionProgress.value = 0 to 0
            Result.Error(ScanError.FailedToDeleteUser)
        }
    }

    override suspend fun deleteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error> = withContext(dispatchers.io) {
        try {
            val users = _inactiveUsers.value
            val user = users.find { it.id == userId }
            if (user != null && !user.isAnonymous) {
                val backupJson = json.encodeToString(user)
                val backupDir = getBackupDir()
                val timestamp = Clock.System.now().toEpochMilliseconds()
                val backupFile = "$backupDir/backup_${userId}_$timestamp.json"
                writeToFile(backupFile, backupJson)
            }

            scanDataSource.deleteUserCollection(userId)
            if (isAnonymous) {
                scanDataSource.deleteAuthUser(userId)
            } else {
                scanDataSource.removeUserFromGlobalRegistry(userId)
                scanDataSource.deleteAuthUserByEmail(userId)
            }

            val updatedUsers = users.filter { it.id != userId }
            _inactiveUsers.value = updatedUsers
            localStorageDataSource.setInactiveUsers(json.encodeToString(updatedUsers))

            EmptySuccessResult
        } catch (e: Exception) {
            println("[Delete] deleteUser($userId) ERROR: ${e.message}")
            e.printStackTrace()
            Result.Error(ScanError.FailedToDeleteUser)
        }
    }

    override suspend fun refreshInactiveUsers(): EmptyResult<Error> = withContext(dispatchers.io) {
        val users = loadSavedUsers()
        _inactiveUsers.value = users
        EmptySuccessResult
    }

    override suspend fun resetCounter(): EmptyResult<Error> = withContext(dispatchers.io) {
        localStorageDataSource.setReadsUsedToday(0)
        localStorageDataSource.setLastScanDate(0L)
        _progress.value = ScanProgressState()
        EmptySuccessResult
    }

    override suspend fun resetAll(): EmptyResult<Error> = withContext(dispatchers.io) {
        localStorageDataSource.clearProgress()
        _inactiveUsers.value = emptyList()
        _progress.value = ScanProgressState()
        EmptySuccessResult
    }

    private fun checkInactiveUser(
        collectionId: String,
        dates: Map<String, DataDayData>,
        isAnonymous: Boolean,
    ): InactiveUser? {
        if (dates.isEmpty()) {
            return InactiveUser(
                id = collectionId,
                isAnonymous = isAnonymous,
                lastDrinkDate = 0L,
                allDates = emptyMap(),
                totalDays = 0,
                drinkDays = 0,
            )
        }

        val drinkDates = dates.filter { it.value.drinkType != null }
        if (drinkDates.isEmpty()) return null

        val lastDrinkEntry = drinkDates.maxByOrNull { it.value.date } ?: return null
        val lastDrinkEpochDays = lastDrinkEntry.value.date

        val now = Clock.System.now()
        val todayEpochDays = now.toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays()
        val daysSinceLastDrink = todayEpochDays - lastDrinkEpochDays

        val thresholdDays = if (isAnonymous) ANONYMOUS_THRESHOLD_DAYS else REGISTERED_THRESHOLD_DAYS

        return if (daysSinceLastDrink > thresholdDays) {
            val drinkDays = drinkDates.size
            InactiveUser(
                id = collectionId,
                isAnonymous = isAnonymous,
                lastDrinkDate = lastDrinkEpochDays,
                allDates = drinkDates.mapValues { it.value.date },
                totalDays = dates.size,
                drinkDays = drinkDays,
            )
        } else {
            null
        }
    }

    private fun loadSavedUsers(): List<InactiveUser> {
        return try {
            val savedJson = localStorageDataSource.getInactiveUsers()
            json.decodeFromString(savedJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getTodayStart(): Long {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return localDate.toEpochDays() * 86_400_000L
    }

    override suspend fun getBackupEntries(): List<BackupEntry> = withContext(dispatchers.io) {
        val entries = mutableListOf<BackupEntry>()
        val backupDir = getBackupDir()
        try {
            val dir = java.io.File(backupDir)
            if (!dir.exists()) return@withContext emptyList()

            val files = dir.listFiles { file -> file.extension == "json" && file.name.startsWith("backup_") } ?: emptyArray()
            for (file in files) {
                try {
                    val content = file.readText()
                    val timestamp = extractTimestampFromFileName(file.name)

                    val userList = try {
                        json.decodeFromString<List<InactiveUser>>(content)
                    } catch (_: Exception) {
                        try {
                            listOf(json.decodeFromString<InactiveUser>(content))
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }

                    for (user in userList) {
                        entries.add(
                            BackupEntry(
                                fileName = file.name,
                                deletedAt = timestamp,
                                user = user,
                            )
                        )
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        entries.sortedByDescending { it.deletedAt }
    }

    override suspend fun restoreUser(entry: BackupEntry): EmptyResult<Error> = withContext(dispatchers.io) {
        try {
            val datesData = entry.user.allDates.mapValues { (_, epochDays) ->
                mapOf("date" to epochDays)
            }
            scanDataSource.restoreUserDates(entry.user.id, datesData)
            println("[Restore] Restored dates for ${entry.user.id}: ${datesData.size} entries")

            val regData = entry.user.registryData
            if (regData != null) {
                val registryMap = mapOf(
                    "nickname" to regData.nickname,
                    "email" to regData.email,
                    "locale" to regData.locale,
                    "token" to regData.token,
                    "donations" to regData.donations,
                )
                scanDataSource.restoreUserRegistryData(regData.email, registryMap)
                println("[Restore] Restored registry data for ${regData.email}")
            }

            EmptySuccessResult
        } catch (e: Exception) {
            println("[Restore] Failed to restore ${entry.user.id}: ${e.message}")
            Result.Error(ScanError.FailedToDeleteUser)
        }
    }

    private fun extractTimestampFromFileName(fileName: String): Long {
        val regex = Regex("""backup_.*?_(\d+)\.json""")
        return regex.find(fileName)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    override suspend fun findOrphanedRegistryUsers(): Result<Pair<Int, List<com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser>>, Error> = withContext(dispatchers.io) {
        try {
            println("[Actualize] Reading collections...")
            val allCollections = scanDataSource.getAllCollections()
            val collectionIds = allCollections.toSet()
            println("[Actualize] Total collections: ${allCollections.size}")

            println("[Actualize] Reading registry users...")
            val registryIds = scanDataSource.getRegistryUserIds()
            println("[Actualize] Registry users: ${registryIds.size}")

            val orphaned = registryIds.filter { email -> email !in collectionIds }
            println("[Actualize] Orphaned (in registry, no collection): ${orphaned.size}")

            Result.Success(registryIds.size to orphaned.map { com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser(email = it) })
        } catch (e: Exception) {
            println("[Actualize] Error: ${e.message}")
            Result.Error(ScanError.FailedToReadDatabase)
        }
    }

    override suspend fun removeRegistryUser(email: String): EmptyResult<Error> = withContext(dispatchers.io) {
        try {
            scanDataSource.removeUserFromGlobalRegistry(email)
            println("[Actualize] Removed from registry: $email")
            EmptySuccessResult
        } catch (e: Exception) {
            println("[Actualize] Failed to remove $email: ${e.message}")
            Result.Error(ScanError.FailedToDeleteUser)
        }
    }

    override suspend fun removeAllOrphanedRegistryUsers(users: List<com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser>): EmptyResult<Error> = withContext(dispatchers.io) {
        try {
            if (users.isEmpty()) return@withContext EmptySuccessResult

            val backupJson = json.encodeToString(users)
            val backupDir = getBackupDir()
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val backupFile = "$backupDir/backup_registry_orphaned_$timestamp.json"
            writeToFile(backupFile, backupJson)

            for (user in users) {
                try {
                    scanDataSource.removeUserFromGlobalRegistry(user.email)
                } catch (e: Exception) {
                    println("[Actualize] Failed to remove ${user.email}: ${e.message}")
                }
            }
            println("[Actualize] Removed ${users.size} orphaned registry users")
            EmptySuccessResult
        } catch (e: Exception) {
            println("[Actualize] removeAllOrphanedRegistryUsers ERROR: ${e.message}")
            Result.Error(ScanError.FailedToDeleteUser)
        }
    }

    companion object {
        private const val ANONYMOUS_THRESHOLD_DAYS = 183L
        private const val REGISTERED_THRESHOLD_DAYS = 730L
        private const val COLLECTION_USERS = "Collection_of_all_users"
    }
}
