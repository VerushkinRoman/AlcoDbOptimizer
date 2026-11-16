package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.withContext

/**
 * Поиск и удаление auth-пользователей без коллекции в базе.
 */
internal class AuthCleanupService(
    private val scanDataSource: ScanDataSource,
    private val dispatchers: Dispatchers,
    private val collections: CollectionsProvider,
) {
    suspend fun findOrphanedAuthUsers(): Result<List<String>, Error> = withContext(dispatchers.io) {
        try {
            println("[AuthCheck] Reading collections...")
            val allCollections = when (val fetched = collections.getCollections()) {
                is Result.Error -> return@withContext Result.Error(ScanError.FailedToReadDatabase)
                is Result.Success -> fetched.data
            }
            val collectionIds = allCollections.toSet()
            println("[AuthCheck] Total collections: ${allCollections.size}")

            println("[AuthCheck] Listing Firebase Auth users...")
            val authEmails = when (val fetched = scanDataSource.listAllAuthUsers()) {
                is Result.Error -> return@withContext Result.Error(ScanError.FailedToReadDatabase)
                is Result.Success -> fetched.data
            }
            println("[AuthCheck] Auth users: ${authEmails.size}")

            val orphaned = authEmails.filter { email -> email !in collectionIds }
            println("[AuthCheck] Auth users without collection: ${orphaned.size}")
            Result.Success(orphaned)
        } catch (e: Exception) {
            println("[AuthCheck] Error: ${e.message}")
            Result.Error(ScanError.FailedToReadDatabase)
        }
    }

    suspend fun deleteAuthUserByEmail(email: String): EmptyResult<Error> =
        withContext(dispatchers.io) {
            when (val result = scanDataSource.deleteAuthUserByEmail(email)) {
                is Result.Error -> {
                    println("[AuthCheck] Failed to delete $email: ${result.error}")
                    Result.Error(ScanError.FailedToDeleteUser)
                }
                is Result.Success -> {
                    println("[AuthCheck] Deleted auth user: $email")
                    EmptySuccessResult
                }
            }
        }
}
