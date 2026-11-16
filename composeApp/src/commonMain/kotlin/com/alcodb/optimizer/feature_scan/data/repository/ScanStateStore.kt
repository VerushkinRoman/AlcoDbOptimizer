package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.LocalStorageDataSource
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.domain.utils.onError
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.repository.ScanProgressState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Общее состояние сканирования: прогресс, найденные пользователи и прогресс удаления.
 * Роли читают и обновляют его через этот класс, а не через отдельные копии.
 */
internal class ScanStateStore(
    private val localStorageDataSource: LocalStorageDataSource,
) {
    val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _progress = MutableStateFlow(ScanProgressState())
    private val _inactiveUsers = MutableStateFlow<List<InactiveUser>>(emptyList())
    private val _deletionProgress = MutableStateFlow(0 to 0)

    val progressFlow = _progress.asStateFlow()
    val inactiveUsersFlow = _inactiveUsers.asStateFlow()
    val deletionProgressFlow = _deletionProgress.asStateFlow()

    var progress: ScanProgressState
        get() = _progress.value
        set(value) {
            _progress.value = value
        }

    var inactiveUsers: List<InactiveUser>
        get() = _inactiveUsers.value
        set(value) {
            _inactiveUsers.value = value
        }

    var deletionProgress: Pair<Int, Int>
        get() = _deletionProgress.value
        set(value) {
            _deletionProgress.value = value
        }

    fun loadSavedUsers(): List<InactiveUser> {
        val savedJson = when (val saved = localStorageDataSource.getInactiveUsers()) {
            is Result.Error -> return emptyList()
            is Result.Success -> saved.data
        }
        return try {
            json.decodeFromString(savedJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun persistInactiveUsers(users: List<InactiveUser>) {
        inactiveUsers = users
        localStorageDataSource.setInactiveUsers(json.encodeToString(users))
            .onError { println("[Scan] Failed to persist inactive users: $it") }
    }

    fun persistProgress(
        processed: Set<String>,
        users: List<InactiveUser>,
        readsUsed: Int,
        today: Long,
    ) {
        localStorageDataSource.setProcessedCollections(json.encodeToString(processed.toList()))
            .onError { println("[Scan] Failed to persist processed collections: $it") }
        localStorageDataSource.setInactiveUsers(json.encodeToString(users))
            .onError { println("[Scan] Failed to persist inactive users: $it") }
        localStorageDataSource.setReadsUsedToday(readsUsed)
            .onError { println("[Scan] Failed to persist reads: $it") }
        localStorageDataSource.setLastScanDate(today)
            .onError { println("[Scan] Failed to persist scan date: $it") }
    }
}
