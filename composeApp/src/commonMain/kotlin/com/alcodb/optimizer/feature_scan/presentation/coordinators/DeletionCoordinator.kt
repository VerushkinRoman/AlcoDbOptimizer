package com.alcodb.optimizer.feature_scan.presentation.coordinators

import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.DeleteAllResult
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.repository.UserDeletionRepository
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState

/**
 * Состояние массового удаления, список неудалённых записей и вызовы репозитория.
 */
internal class DeletionCoordinator(
    private val repository: UserDeletionRepository,
) {
    fun startDeleteAll(state: ScanScreenState): ScanScreenState = state.copy(
        isDeleteAllConfirmVisible = false,
        isDeleteFailedDialogVisible = false,
        failedDeletionUsers = emptyList(),
        exportedFailedUsersPath = null,
        failedUsersExportError = false,
    )

    fun startRetry(state: ScanScreenState): ScanScreenState = state.copy(
        isDeleteFailedDialogVisible = false,
        exportedFailedUsersPath = null,
        failedUsersExportError = false,
    )

    fun applyResult(state: ScanScreenState, result: DeleteAllResult): ScanScreenState = state.copy(
        failedDeletionUsers = result.failedUsers,
        isDeleteFailedDialogVisible = result.failedUsers.isNotEmpty(),
    )

    fun dismissFailed(state: ScanScreenState): ScanScreenState = state.copy(
        isDeleteFailedDialogVisible = false,
        exportedFailedUsersPath = null,
        failedUsersExportError = false,
    )

    fun applyExport(state: ScanScreenState, path: String): ScanScreenState = state.copy(
        exportedFailedUsersPath = path,
        failedUsersExportError = false,
    )

    fun applyExportError(state: ScanScreenState): ScanScreenState = state.copy(
        exportedFailedUsersPath = null,
        failedUsersExportError = true,
    )

    suspend fun deleteAll(): Result<DeleteAllResult, Error> = repository.deleteAllInactiveUsers()

    suspend fun exportFailedUsers(users: List<InactiveUser>): Result<String, Error> =
        repository.exportFailedUsers(users)
}
