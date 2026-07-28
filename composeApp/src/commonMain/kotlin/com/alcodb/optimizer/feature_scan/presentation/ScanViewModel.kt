package com.alcodb.optimizer.feature_scan.presentation

import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.onError
import com.alcodb.optimizer.common.domain.utils.onSuccess
import com.alcodb.optimizer.common.presentation.utils.BaseSharedViewModel
import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser
import com.alcodb.optimizer.feature_scan.domain.repository.ScanRepository
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenAction
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenEvent
import kotlinx.coroutines.flow.collectLatest

class ScanViewModel(
    private val scanRepository: ScanRepository,
    private val dispatchers: Dispatchers,
    private val initFirebase: suspend () -> Unit,
) : BaseSharedViewModel<ScanScreenState, ScanScreenAction, ScanScreenEvent>(
    initialState = ScanScreenState()
) {
    init {
        loadSavedData()
    }

    override fun obtainEvent(viewEvent: ScanScreenEvent) {
        when (viewEvent) {
            ScanScreenEvent.ClearAction -> viewAction = null
            ScanScreenEvent.OnInitFirebase -> onInitFirebase()
            ScanScreenEvent.OnStartScan -> onStartScan()
            ScanScreenEvent.OnStopScan -> onStopScan()
            ScanScreenEvent.OnDeleteAllClick -> onDeleteAllClick()
            ScanScreenEvent.OnDeleteAllConfirm -> onDeleteAllConfirm()
            ScanScreenEvent.OnDeleteAllDismiss -> onDeleteAllDismiss()
            is ScanScreenEvent.OnUserClick -> onUserClick(viewEvent.user)
            ScanScreenEvent.OnUserDetailDismiss -> onUserDetailDismiss()
            ScanScreenEvent.OnDeleteSingleUserClick -> onDeleteSingleUserClick()
            ScanScreenEvent.OnDeleteSingleConfirm -> onDeleteSingleConfirm()
            ScanScreenEvent.OnDeleteSingleDismiss -> onDeleteSingleDismiss()
            is ScanScreenEvent.OnReadLimitChanged -> onReadLimitChanged(viewEvent.limit)
            ScanScreenEvent.OnResetCounter -> onResetCounter()
            ScanScreenEvent.OnResetAll -> onResetAll()
            ScanScreenEvent.OnRestoreClick -> onRestoreClick()
            ScanScreenEvent.OnRestoreDismiss -> onRestoreDismiss()
            is ScanScreenEvent.OnRestoreFilterChanged -> onRestoreFilterChanged(viewEvent.filter)
            is ScanScreenEvent.OnRestoreSelectEntry -> onRestoreSelectEntry(viewEvent.entry)
            ScanScreenEvent.OnRestoreConfirm -> onRestoreConfirm()
            ScanScreenEvent.OnActualizeClick -> onActualizeClick()
            ScanScreenEvent.OnActualizeDismiss -> onActualizeDismiss()
            is ScanScreenEvent.OnActualizeFilterChanged -> onActualizeFilterChanged(viewEvent.filter)
            ScanScreenEvent.OnActualizeRemoveAll -> onActualizeRemoveAll()
            is ScanScreenEvent.OnActualizeRemoveSingle -> onActualizeRemoveSingle(viewEvent.user)
        }
    }

    private fun loadSavedData() {
        withViewModelScope {
            scanRepository.getInactiveUsers().collectLatest { result ->
                result.onSuccess { users ->
                    viewState = viewState.copy(inactiveUsers = users)
                }
            }
        }

        withViewModelScope {
            scanRepository.getScanProgress().collectLatest { progress ->
                viewState = viewState.copy(
                    isScanning = progress.isScanning,
                    collectionsTotal = progress.collectionsTotal,
                    collectionsProcessed = progress.collectionsProcessed,
                    readsUsed = progress.readsUsed,
                    maxReads = progress.maxReads,
                    lastScanDate = progress.lastScanDate,
                    canScanToday = progress.canScanToday,
                    isComplete = progress.isComplete,
                )
            }
        }

        withViewModelScope {
            scanRepository.getDeletionProgress().collectLatest { (deleted, total) ->
                viewState = viewState.copy(
                    isDeleting = total > 0,
                    deletedCount = deleted,
                    deletionTotal = total,
                )
            }
        }
    }

    private fun onInitFirebase() {
        viewState = viewState.copy(isInitializingFirebase = true)
        withViewModelScope {
            try {
                initFirebase()
                viewState = viewState.copy(isInitializingFirebase = false)
                viewAction = ScanScreenAction.FirebaseInitialized
            } catch (e: Exception) {
                viewState = viewState.copy(
                    isInitializingFirebase = false,
                    errorMessage = e.message ?: "Failed to initialize Firebase"
                )
                viewAction = ScanScreenAction.FirebaseInitFailed
            }
        }
    }

    private fun onStartScan() {
        viewState = viewState.copy(
            isScanning = true,
            errorMessage = null,
            maxReads = viewState.readLimit,
        )
        withViewModelScope {
            scanRepository.startScan(maxReads = viewState.readLimit)
                .onSuccess {
                    viewState = viewState.copy(isScanning = false)
                    viewAction = ScanScreenAction.ShowScanComplete
                }
                .onError { error ->
                    println("[ViewModel] Scan error: $error")
                    viewState = viewState.copy(isScanning = false)
                    when (error) {
                        is ScanError -> {
                            when (error) {
                                ScanError.DailyLimitReached ->
                                    viewAction = ScanScreenAction.ShowDailyLimitReached(viewState.maxReads)
                                ScanError.FirebaseNotInitialized ->
                                    viewAction = ScanScreenAction.ShowError("Firebase not initialized")
                                else ->
                                    viewAction = ScanScreenAction.ShowError(error.name)
                            }
                        }
                        else -> viewAction = ScanScreenAction.ShowError("Unknown error")
                    }
                }
        }
    }

    private fun onStopScan() {
        withViewModelScope {
            scanRepository.stopScan()
        }
    }

    private fun onDeleteAllClick() {
        viewState = viewState.copy(isDeleteAllConfirmVisible = true)
    }

    private fun onDeleteAllConfirm() {
        viewState = viewState.copy(isDeleteAllConfirmVisible = false)
        withViewModelScope {
            scanRepository.deleteAllInactiveUsers()
                .onSuccess {
                    viewAction = ScanScreenAction.ShowDeleteComplete(0)
                }
                .onError { error ->
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun onDeleteAllDismiss() {
        viewState = viewState.copy(isDeleteAllConfirmVisible = false)
    }

    private fun onUserClick(user: InactiveUser) {
        viewState = viewState.copy(selectedUser = user)
    }

    private fun onUserDetailDismiss() {
        viewState = viewState.copy(selectedUser = null)
    }

    private fun onDeleteSingleUserClick() {
        viewState = viewState.copy(isDeleteSingleConfirmVisible = true)
    }

    private fun onDeleteSingleConfirm() {
        val user = viewState.selectedUser ?: return
        viewState = viewState.copy(isDeleteSingleConfirmVisible = false)
        withViewModelScope {
            scanRepository.deleteUser(user.id, user.isAnonymous)
                .onSuccess {
                    viewState = viewState.copy(selectedUser = null)
                    viewAction = ScanScreenAction.ShowUserDeleted(user.id)
                }
                .onError { error ->
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun onDeleteSingleDismiss() {
        viewState = viewState.copy(isDeleteSingleConfirmVisible = false)
    }

    private fun onReadLimitChanged(limit: Int) {
        viewState = viewState.copy(readLimit = limit.coerceAtLeast(1))
    }

    private fun onResetCounter() {
        withViewModelScope {
            scanRepository.resetCounter()
                .onSuccess {
                    viewState = viewState.copy(
                        readsUsed = 0,
                        collectionsProcessed = 0,
                        collectionsTotal = 0,
                        maxReads = 1000,
                        isComplete = false,
                        lastScanDate = 0L,
                    )
                }
        }
    }

    private fun onResetAll() {
        withViewModelScope {
            scanRepository.resetAll()
                .onSuccess {
                    viewState = ScanScreenState(
                        readLimit = viewState.readLimit,
                        isInitializingFirebase = false,
                    )
                }
        }
    }

    private fun onRestoreClick() {
        withViewModelScope {
            val entries = scanRepository.getBackupEntries()
            viewState = viewState.copy(
                isRestoreDialogVisible = true,
                backupEntries = entries,
                backupFilter = "",
                selectedBackupEntry = null,
            )
        }
    }

    private fun onRestoreDismiss() {
        viewState = viewState.copy(
            isRestoreDialogVisible = false,
            selectedBackupEntry = null,
        )
    }

    private fun onRestoreFilterChanged(filter: String) {
        viewState = viewState.copy(backupFilter = filter)
    }

    private fun onRestoreSelectEntry(entry: BackupEntry) {
        viewState = viewState.copy(selectedBackupEntry = entry)
    }

    private fun onRestoreConfirm() {
        val entry = viewState.selectedBackupEntry ?: return
        viewState = viewState.copy(isRestoring = true)
        withViewModelScope {
            scanRepository.restoreUser(entry)
                .onSuccess {
                    viewState = viewState.copy(
                        isRestoring = false,
                        isRestoreDialogVisible = false,
                        selectedBackupEntry = null,
                    )
                    viewAction = ScanScreenAction.ShowRestoreComplete(entry.user.id)
                }
                .onError { error ->
                    viewState = viewState.copy(isRestoring = false)
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun onActualizeClick() {
        viewState = viewState.copy(isActualizeDialogVisible = true, isActualizing = true)
        withViewModelScope {
            scanRepository.findOrphanedRegistryUsers()
                .onSuccess { (total, users) ->
                    viewState = viewState.copy(
                        isActualizing = false,
                        orphanedRegistryUsers = users,
                        totalRegistryUsers = total,
                        actualizeFilter = "",
                    )
                }
                .onError { error ->
                    viewState = viewState.copy(isActualizing = false)
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun onActualizeDismiss() {
        viewState = viewState.copy(
            isActualizeDialogVisible = false,
            orphanedRegistryUsers = emptyList(),
            actualizeFilter = "",
        )
    }

    private fun onActualizeFilterChanged(filter: String) {
        viewState = viewState.copy(actualizeFilter = filter)
    }

    private fun onActualizeRemoveAll() {
        val users = viewState.orphanedRegistryUsers
        if (users.isEmpty()) return
        viewState = viewState.copy(isRemovingOrphans = true)
        withViewModelScope {
            scanRepository.removeAllOrphanedRegistryUsers(users)
                .onSuccess {
                    viewState = viewState.copy(
                        isRemovingOrphans = false,
                        orphanedRegistryUsers = emptyList(),
                        isActualizeDialogVisible = false,
                    )
                    viewAction = ScanScreenAction.ShowActualizeComplete(users.size)
                }
                .onError { error ->
                    viewState = viewState.copy(isRemovingOrphans = false)
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun onActualizeRemoveSingle(user: com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser) {
        withViewModelScope {
            scanRepository.removeRegistryUser(user.email)
                .onSuccess {
                    val updated = viewState.orphanedRegistryUsers.filter { it.email != user.email }
                    viewState = viewState.copy(orphanedRegistryUsers = updated)
                    viewAction = ScanScreenAction.ShowActualizeComplete(1)
                }
                .onError { error ->
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }
}
