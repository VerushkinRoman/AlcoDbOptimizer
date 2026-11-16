package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.domain.utils.onError
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Поиск и удаление записей реестра без пользовательской коллекции.
 */
internal class RegistryCleanupService(
    private val scanDataSource: ScanDataSource,
    private val dispatchers: Dispatchers,
    private val collections: CollectionsProvider,
    private val backups: BackupService,
    private val json: Json,
) {
    suspend fun findOrphanedRegistryUsers(): Result<Pair<Int, List<OrphanedRegistryUser>>, Error> =
        withContext(dispatchers.io) {
            try {
                println("[Actualize] Reading collections...")
                val allCollections = when (val fetched = collections.getCollections()) {
                    is Result.Error -> return@withContext Result.Error(ScanError.FailedToReadDatabase)
                    is Result.Success -> fetched.data
                }
                val collectionIds = allCollections.toSet()
                println("[Actualize] Total collections: ${allCollections.size}")

                println("[Actualize] Reading registry users...")
                val registryIds = when (val fetched = scanDataSource.getRegistryUserIds()) {
                    is Result.Error -> return@withContext Result.Error(ScanError.FailedToReadDatabase)
                    is Result.Success -> fetched.data
                }
                println("[Actualize] Registry users: ${registryIds.size}")

                val orphaned = registryIds.filter { email -> email !in collectionIds }
                println("[Actualize] Orphaned (in registry, no collection): ${orphaned.size}")

                Result.Success(
                    registryIds.size to orphaned.map { OrphanedRegistryUser(email = it) },
                )
            } catch (e: Exception) {
                println("[Actualize] Error: ${e.message}")
                Result.Error(ScanError.FailedToReadDatabase)
            }
        }

    suspend fun removeRegistryUser(email: String): EmptyResult<Error> = withContext(dispatchers.io) {
        when (val result = scanDataSource.removeUserFromGlobalRegistry(email)) {
            is Result.Error -> {
                println("[Actualize] Failed to remove $email: ${result.error}")
                Result.Error(ScanError.FailedToDeleteUser)
            }
            is Result.Success -> {
                println("[Actualize] Removed from registry: $email")
                EmptySuccessResult
            }
        }
    }

    suspend fun removeAllOrphanedRegistryUsers(
        users: List<OrphanedRegistryUser>,
    ): EmptyResult<Error> = withContext(dispatchers.io) {
        try {
            if (users.isEmpty()) return@withContext EmptySuccessResult

            backups.write("registry_orphaned", json.encodeToString(users))

            for (user in users) {
                scanDataSource.removeUserFromGlobalRegistry(user.email)
                    .onError { println("[Actualize] Failed to remove ${user.email}: $it") }
            }
            println("[Actualize] Removed ${users.size} orphaned registry users")
            EmptySuccessResult
        } catch (e: Exception) {
            println("[Actualize] removeAllOrphanedRegistryUsers ERROR: ${e.message}")
            Result.Error(ScanError.FailedToDeleteUser)
        }
    }
}
