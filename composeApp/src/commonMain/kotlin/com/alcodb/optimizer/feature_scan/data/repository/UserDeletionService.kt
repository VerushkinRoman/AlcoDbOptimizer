package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.ServiceCollections
import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.utils.getBackupDir
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.DeleteAllResult
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Clock

/**
 * Удаление коллекций, auth-записей и записей реестра с бэкапом.
 */
internal class UserDeletionService(
    private val scanDataSource: ScanDataSource,
    private val dispatchers: Dispatchers,
    private val state: ScanStateStore,
    private val backups: BackupService,
    private val json: Json,
) {
    suspend fun deleteAllInactiveUsers(): Result<DeleteAllResult, Error> =
        withContext(dispatchers.io) {
            try {
                val users = state.inactiveUsers.filterNot { ServiceCollections.isService(it.id) }
                if (users.isEmpty()) {
                    return@withContext Result.Success(
                        DeleteAllResult(deleted = 0, total = 0, failedUsers = emptyList()),
                    )
                }

                val registeredUsers = users.filter { !it.isAnonymous }
                if (registeredUsers.isNotEmpty()) {
                    backups.write("registered", json.encodeToString(registeredUsers))
                }

                val total = users.size
                state.deletionProgress = 0 to total
                var deleted = 0
                val deletedIds = mutableSetOf<String>()
                val failedUsers = mutableListOf<InactiveUser>()

                for ((index, user) in users.withIndex()) {
                    when (val deleteResult = deleteRemoteUser(user.id, user.isAnonymous)) {
                        is Result.Success -> {
                            deleted++
                            deletedIds += user.id
                        }
                        is Result.Error -> {
                            failedUsers += user
                            println("[Delete] Failed to delete ${user.id}: ${deleteResult.error}")
                        }
                    }
                    state.deletionProgress = (index + 1) to total
                    delay(100)
                }

                val remainingUsers = state.inactiveUsers.filterNot { it.id in deletedIds }
                state.persistInactiveUsers(remainingUsers)
                state.deletionProgress = 0 to 0

                Result.Success(
                    DeleteAllResult(
                        deleted = deleted,
                        total = total,
                        failedUsers = failedUsers,
                    ),
                )
            } catch (e: Exception) {
                println("[Delete] deleteAllInactiveUsers ERROR: ${e.message}")
                e.printStackTrace()
                state.deletionProgress = 0 to 0
                Result.Error(ScanError.FailedToDeleteUser)
            }
        }

    suspend fun exportFailedUsers(users: List<InactiveUser>): Result<String, Error> =
        withContext(dispatchers.io) {
            try {
                val directory = File(getBackupDir()).apply { mkdirs() }
                val file = File(
                    directory,
                    "failed_users_${Clock.System.now().toEpochMilliseconds()}.csv",
                )
                file.writeText(buildFailedUsersCsv(users))
                Result.Success(file.absolutePath)
            } catch (e: Exception) {
                println("[Delete] exportFailedUsers ERROR: ${e.message}")
                Result.Error(ScanError.FailedToExportUsers)
            }
        }

    suspend fun deleteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error> =
        withContext(dispatchers.io) {
            if (ServiceCollections.isService(userId)) {
                println("[Delete] Refusing to delete service collection: $userId")
                return@withContext Result.Error(ScanError.FailedToDeleteUser)
            }
            try {
                val users = state.inactiveUsers
                val user = users.find { it.id == userId }
                if (user != null && !user.isAnonymous) {
                    backups.write(userId, json.encodeToString(user))
                }

                val deleteResult = deleteRemoteUser(userId, isAnonymous)
                if (deleteResult is Result.Error) {
                    return@withContext Result.Error(ScanError.FailedToDeleteUser)
                }
                state.persistInactiveUsers(users.filter { it.id != userId })

                EmptySuccessResult
            } catch (e: Exception) {
                println("[Delete] deleteUser($userId) ERROR: ${e.message}")
                e.printStackTrace()
                Result.Error(ScanError.FailedToDeleteUser)
            }
        }

    private suspend fun deleteRemoteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error> {
        val collectionResult = scanDataSource.deleteUserCollection(userId)
        if (collectionResult is Result.Error) return collectionResult

        return if (isAnonymous) {
            scanDataSource.deleteAuthUser(userId)
        } else {
            val registryResult = scanDataSource.removeUserFromGlobalRegistry(userId)
            if (registryResult is Result.Error) return registryResult
            scanDataSource.deleteAuthUserByEmail(userId)
        }
    }
}

internal fun buildFailedUsersCsv(users: List<InactiveUser>): String = buildString {
    appendLine("user_id")
    users.forEach { user -> appendLine(csvCell(user.id)) }
}

private fun csvCell(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }
