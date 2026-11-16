package com.alcodb.optimizer.feature_scan.presentation.coordinators

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.repository.BackupRepository
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState

/**
 * Состояние диалога восстановления и вызов репозитория бэкапов.
 */
internal class RestoreCoordinator(
    private val repository: BackupRepository,
) {
    suspend fun loadEntries(): Result<List<BackupEntry>, Error> = repository.getBackupEntries()

    fun show(state: ScanScreenState, entries: List<BackupEntry>): ScanScreenState = state.copy(
        isRestoreDialogVisible = true,
        backupEntries = entries,
        backupFilter = "",
        selectedBackupEntry = null,
    )

    fun dismiss(state: ScanScreenState): ScanScreenState = state.copy(
        isRestoreDialogVisible = false,
        selectedBackupEntry = null,
    )

    fun filterChanged(state: ScanScreenState, filter: String): ScanScreenState =
        state.copy(backupFilter = filter)

    fun selectEntry(state: ScanScreenState, entry: BackupEntry): ScanScreenState =
        state.copy(selectedBackupEntry = entry)

    fun startRestore(state: ScanScreenState): ScanScreenState = state.copy(isRestoring = true)

    fun applySuccess(state: ScanScreenState): ScanScreenState = state.copy(
        isRestoring = false,
        isRestoreDialogVisible = false,
        selectedBackupEntry = null,
    )

    fun stopRestoring(state: ScanScreenState): ScanScreenState =
        state.copy(isRestoring = false)

    suspend fun restore(entry: BackupEntry): EmptyResult<Error> = repository.restoreUser(entry)
}
