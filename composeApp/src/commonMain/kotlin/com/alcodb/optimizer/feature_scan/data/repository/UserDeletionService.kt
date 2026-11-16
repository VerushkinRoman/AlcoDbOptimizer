package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.ServiceCollections
import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.domain.utils.onError
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

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
    suspend fun deleteAllInactiveUsers(): EmptyResult<Error> = withContext(dispatchers.io) {
        try {
            val users = state.inactiveUsers.filterNot { ServiceCollections.isService(it.id) }
            if (users.isEmpty()) return@withContext EmptySuccessResult

            val registeredUsers = users.filter { !it.isAnonymous }
            if (registeredUsers.isNotEmpty()) {
                backups.write("registered", json.encodeToString(registeredUsers))
            }

            val total = users.size
            state.deletionProgress = 0 to total

            for ((index, user) in users.withIndex()) {
                deleteRemoteUser(user.id, user.isAnonymous)
                    .onError { println("[Delete] Failed to delete ${user.id}: $it") }
                state.deletionProgress = (index + 1) to total
                delay(100)
            }

            state.persistInactiveUsers(emptyList())
            state.deletionProgress = 0 to 0

            EmptySuccessResult
        } catch (e: Exception) {
            println("[Delete] deleteAllInactiveUsers ERROR: ${e.message}")
            e.printStackTrace()
            state.deletionProgress = 0 to 0
            Result.Error(ScanError.FailedToDeleteUser)
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
