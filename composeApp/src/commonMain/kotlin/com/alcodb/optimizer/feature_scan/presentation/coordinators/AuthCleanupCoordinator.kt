package com.alcodb.optimizer.feature_scan.presentation.coordinators

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.domain.utils.onError
import com.alcodb.optimizer.common.domain.utils.onSuccess
import com.alcodb.optimizer.feature_scan.domain.repository.AuthCleanupRepository
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState

/**
 * Состояние диалога проверки Auth и вызовы репозитория.
 */
internal class AuthCleanupCoordinator(
    private val repository: AuthCleanupRepository,
) {
    fun show(state: ScanScreenState): ScanScreenState =
        state.copy(isAuthOrphanDialogVisible = true, isCheckingAuthOrphans = true)

    fun applyLoaded(state: ScanScreenState, emails: List<String>): ScanScreenState = state.copy(
        isCheckingAuthOrphans = false,
        authOrphanEmails = emails,
        authOrphanFilter = "",
    )

    fun stopChecking(state: ScanScreenState): ScanScreenState =
        state.copy(isCheckingAuthOrphans = false)

    fun dismiss(state: ScanScreenState): ScanScreenState = state.copy(
        isAuthOrphanDialogVisible = false,
        authOrphanEmails = emptyList(),
        authOrphanFilter = "",
    )

    fun filterChanged(state: ScanScreenState, filter: String): ScanScreenState =
        state.copy(authOrphanFilter = filter)

    fun startDeleteAll(state: ScanScreenState): ScanScreenState =
        state.copy(isDeletingAuthOrphans = true)

    fun applyDeleteAllSuccess(state: ScanScreenState): ScanScreenState = state.copy(
        isDeletingAuthOrphans = false,
        authOrphanEmails = emptyList(),
        isAuthOrphanDialogVisible = false,
    )

    fun applyDeleteSingle(state: ScanScreenState, email: String): ScanScreenState =
        state.copy(authOrphanEmails = state.authOrphanEmails.filter { it != email })

    suspend fun findOrphaned(): Result<List<String>, Error> = repository.findOrphanedAuthUsers()

    suspend fun deleteAll(emails: List<String>): Int {
        var deleted = 0
        for (email in emails) {
            repository.deleteAuthUserByEmail(email)
                .onSuccess { deleted++ }
                .onError { println("[AuthCheck] Failed to delete $email") }
        }
        return deleted
    }

    suspend fun deleteSingle(email: String): EmptyResult<Error> =
        repository.deleteAuthUserByEmail(email)
}
