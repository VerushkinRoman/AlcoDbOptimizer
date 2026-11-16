package com.alcodb.optimizer.feature_scan.presentation.coordinators

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser
import com.alcodb.optimizer.feature_scan.domain.repository.RegistryCleanupRepository
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState

/**
 * Состояние диалога актуализации реестра и вызовы репозитория.
 */
internal class RegistryCleanupCoordinator(
    private val repository: RegistryCleanupRepository,
) {
    fun show(state: ScanScreenState): ScanScreenState =
        state.copy(isActualizeDialogVisible = true, isActualizing = true)

    fun applyLoaded(
        state: ScanScreenState,
        total: Int,
        users: List<OrphanedRegistryUser>,
    ): ScanScreenState = state.copy(
        isActualizing = false,
        orphanedRegistryUsers = users,
        totalRegistryUsers = total,
        actualizeFilter = "",
    )

    fun dismiss(state: ScanScreenState): ScanScreenState = state.copy(
        isActualizeDialogVisible = false,
        orphanedRegistryUsers = emptyList(),
        actualizeFilter = "",
    )

    fun filterChanged(state: ScanScreenState, filter: String): ScanScreenState =
        state.copy(actualizeFilter = filter)

    fun stopLoading(state: ScanScreenState): ScanScreenState =
        state.copy(isActualizing = false)

    fun startRemoveAll(state: ScanScreenState): ScanScreenState =
        state.copy(isRemovingOrphans = true)

    fun applyRemoveAllSuccess(state: ScanScreenState): ScanScreenState = state.copy(
        isRemovingOrphans = false,
        orphanedRegistryUsers = emptyList(),
        isActualizeDialogVisible = false,
    )

    fun stopRemoving(state: ScanScreenState): ScanScreenState =
        state.copy(isRemovingOrphans = false)

    fun applyRemoveSingle(state: ScanScreenState, email: String): ScanScreenState =
        state.copy(orphanedRegistryUsers = state.orphanedRegistryUsers.filter { it.email != email })

    suspend fun findOrphaned(): Result<Pair<Int, List<OrphanedRegistryUser>>, Error> =
        repository.findOrphanedRegistryUsers()

    suspend fun removeAll(users: List<OrphanedRegistryUser>): EmptyResult<Error> =
        repository.removeAllOrphanedRegistryUsers(users)

    suspend fun removeSingle(email: String): EmptyResult<Error> =
        repository.removeRegistryUser(email)
}
