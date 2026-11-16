package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.utils.getBackupDir
import com.alcodb.optimizer.common.utils.writeToFile
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Clock

/**
 * Файловые бэкапы: запись перед удалением, чтение списка и восстановление.
 */
internal class BackupService(
    private val scanDataSource: ScanDataSource,
    private val dispatchers: Dispatchers,
    private val json: Json,
) {
    fun write(prefix: String, payload: String) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        writeToFile("${getBackupDir()}/backup_${prefix}_$timestamp.json", payload)
    }

    suspend fun getBackupEntries(): Result<List<BackupEntry>, Error> = withContext(dispatchers.io) {
        val entries = mutableListOf<BackupEntry>()
        val dir = File(getBackupDir())
        if (!dir.exists()) return@withContext Result.Success(emptyList())

        val files = dir.listFiles { file ->
            file.extension == "json" && file.name.startsWith("backup_")
        } ?: emptyArray()

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
                        ),
                    )
                }
            } catch (_: Exception) {
            }
        }
        Result.Success(entries.sortedByDescending { it.deletedAt })
    }

    suspend fun restoreUser(entry: BackupEntry): EmptyResult<Error> = withContext(dispatchers.io) {
        try {
            val datesData = entry.user.allDates.mapValues { (_, epochDays) ->
                mapOf("date" to epochDays)
            }
            val datesResult = scanDataSource.restoreUserDates(entry.user.id, datesData)
            if (datesResult is Result.Error) {
                return@withContext Result.Error(ScanError.FailedToDeleteUser)
            }
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
                val registryResult = scanDataSource.restoreUserRegistryData(regData.email, registryMap)
                if (registryResult is Result.Error) {
                    return@withContext Result.Error(ScanError.FailedToDeleteUser)
                }
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
}
