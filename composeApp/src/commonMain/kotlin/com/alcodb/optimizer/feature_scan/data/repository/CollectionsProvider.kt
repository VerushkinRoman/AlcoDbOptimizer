package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.LocalStorageDataSource
import com.alcodb.optimizer.common.data.datasource.ServiceCollections
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.domain.utils.onError
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import kotlinx.serialization.json.Json

/**
 * Отдаёт список пользовательских коллекций: сначала из кэша, затем из Firebase.
 */
internal class CollectionsProvider(
    private val scanDataSource: ScanDataSource,
    private val localStorageDataSource: LocalStorageDataSource,
    private val json: Json,
) {
    suspend fun getCollections(): Result<List<String>, Error> {
        val cached = when (val stored = localStorageDataSource.getCachedCollections()) {
            is Result.Error -> null
            is Result.Success -> try {
                json.decodeFromString<List<String>>(stored.data)
            } catch (_: Exception) {
                null
            }
        }

        val raw = if (!cached.isNullOrEmpty()) {
            println("[Scan] Using cached collections: ${cached.size}")
            cached
        } else {
            when (val fresh = scanDataSource.getAllCollections()) {
                is Result.Error -> return fresh
                is Result.Success -> {
                    println("[Scan] Fetched collections from Firebase: ${fresh.data.size}")
                    localStorageDataSource.setCachedCollections(json.encodeToString(fresh.data))
                        .onError { println("[Scan] Failed to cache collections: $it") }
                    fresh.data
                }
            }
        }
        return Result.Success(raw.filterNot(ServiceCollections::isService).sorted())
    }
}
