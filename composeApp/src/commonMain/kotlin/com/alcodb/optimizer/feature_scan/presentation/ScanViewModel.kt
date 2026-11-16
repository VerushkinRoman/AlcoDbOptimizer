package com.alcodb.optimizer.feature_scan.presentation

import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.onError
import com.alcodb.optimizer.common.domain.utils.onSuccess
import com.alcodb.optimizer.common.presentation.utils.BaseSharedViewModel
import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.repository.ScanRepository
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import com.alcodb.optimizer.feature_scan.presentation.coordinators.AuthCleanupCoordinator
import com.alcodb.optimizer.feature_scan.presentation.coordinators.LostSearchCoordinator
import com.alcodb.optimizer.feature_scan.presentation.coordinators.RegistryCleanupCoordinator
import com.alcodb.optimizer.feature_scan.presentation.coordinators.RestoreCoordinator
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenAction
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenEvent
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState
import kotlinx.coroutines.flow.collectLatest

class ScanViewModel(
    private val scanRepository: ScanRepository,
    private val initFirebase: suspend () -> Unit,
) : BaseSharedViewModel<ScanScreenState, ScanScreenAction, ScanScreenEvent>(
    initialState = ScanScreenState()
) {
    private val lostSearch = LostSearchCoordinator(scanRepository)
    private val restore = RestoreCoordinator(scanRepository)
    private val registryCleanup = RegistryCleanupCoordinator(scanRepository)
    private val authCleanup = AuthCleanupCoordinator(scanRepository)

    init {
        loadSavedData()
    }

    override fun obtainEvent(viewEvent: ScanScreenEvent) {
        when (viewEvent) {
            ScanScreenEvent.ClearAction -> viewAction = null
            ScanScreenEvent.OnInitFirebase -> onInitFirebase()
            ScanScreenEvent.OnStartScan -> onStartScan()
            ScanScreenEvent.OnStopScan -> onStopScan()
            ScanScreenEvent.OnDeleteAllClick ->
                viewState = viewState.copy(isDeleteAllConfirmVisible = true)
            ScanScreenEvent.OnDeleteAllConfirm -> onDeleteAllConfirm()
            ScanScreenEvent.OnDeleteAllDismiss ->
                viewState = viewState.copy(isDeleteAllConfirmVisible = false)
            is ScanScreenEvent.OnUserClick -> viewState = viewState.copy(
                selectedUser = viewEvent.user,
                showAllDatesInUserDetail = false,
            )
            ScanScreenEvent.OnUserDetailDismiss -> viewState = viewState.copy(
                selectedUser = null,
                showAllDatesInUserDetail = false,
            )
            ScanScreenEvent.OnUserDetailToggleAllDates -> viewState = viewState.copy(
                showAllDatesInUserDetail = !viewState.showAllDatesInUserDetail,
            )
            ScanScreenEvent.OnDeleteSingleUserClick ->
                viewState = viewState.copy(isDeleteSingleConfirmVisible = true)
            ScanScreenEvent.OnDeleteSingleConfirm -> onDeleteSingleConfirm()
            ScanScreenEvent.OnDeleteSingleDismiss ->
                viewState = viewState.copy(isDeleteSingleConfirmVisible = false)
            is ScanScreenEvent.OnReadLimitChanged ->
                viewState = viewState.copy(readLimit = viewEvent.limit.coerceAtLeast(1))
            ScanScreenEvent.OnResetCounter -> onResetCounter()
            ScanScreenEvent.OnResetAll -> onResetAll()
            ScanScreenEvent.OnToggleRegisteredOnly ->
                viewState = viewState.copy(showOnlyRegistered = !viewState.showOnlyRegistered)

            ScanScreenEvent.OnRestoreClick -> loadBackupEntries()
            ScanScreenEvent.OnRestoreDismiss -> viewState = restore.dismiss(viewState)
            is ScanScreenEvent.OnRestoreFilterChanged ->
                viewState = restore.filterChanged(viewState, viewEvent.filter)
            is ScanScreenEvent.OnRestoreSelectEntry ->
                viewState = restore.selectEntry(viewState, viewEvent.entry)
            ScanScreenEvent.OnRestoreConfirm -> confirmRestore()

            ScanScreenEvent.OnActualizeClick -> loadOrphanedRegistryUsers()
            ScanScreenEvent.OnActualizeDismiss -> viewState = registryCleanup.dismiss(viewState)
            is ScanScreenEvent.OnActualizeFilterChanged ->
                viewState = registryCleanup.filterChanged(viewState, viewEvent.filter)
            ScanScreenEvent.OnActualizeRemoveAll -> removeAllOrphanedUsers()
            is ScanScreenEvent.OnActualizeRemoveSingle ->
                removeSingleOrphanedUser(viewEvent.user.email)

            ScanScreenEvent.OnAuthCheckClick -> loadAuthOrphans()
            ScanScreenEvent.OnAuthCheckDismiss -> viewState = authCleanup.dismiss(viewState)
            is ScanScreenEvent.OnAuthCheckFilterChanged ->
                viewState = authCleanup.filterChanged(viewState, viewEvent.filter)
            ScanScreenEvent.OnAuthCheckDeleteAll -> deleteAllAuthOrphans()
            is ScanScreenEvent.OnAuthCheckDeleteSingle -> deleteSingleAuthOrphan(viewEvent.email)

            ScanScreenEvent.OnLostSearchClick -> viewState = lostSearch.open(viewState)
            ScanScreenEvent.OnLostSearchDismiss -> viewState = lostSearch.dismiss(viewState)
            is ScanScreenEvent.OnLostSearchDatePickerOpen ->
                viewState = lostSearch.datePickerOpen(viewState, viewEvent.target)
            ScanScreenEvent.OnLostSearchDatePickerDismiss ->
                viewState = lostSearch.datePickerDismiss(viewState)
            is ScanScreenEvent.OnLostSearchStartDateChanged ->
                viewState = viewState.copy(lostSearchStartDate = viewEvent.date)
            is ScanScreenEvent.OnLostSearchEndDateChanged ->
                viewState = viewState.copy(lostSearchEndDate = viewEvent.date)
            is ScanScreenEvent.OnLostSearchStartExactChanged ->
                viewState = lostSearch.exactChanged(viewState, true, viewEvent.exact)
            is ScanScreenEvent.OnLostSearchEndExactChanged ->
                viewState = lostSearch.exactChanged(viewState, false, viewEvent.exact)
            is ScanScreenEvent.OnLostSearchMonthsDialogOpen ->
                viewState = lostSearch.monthsDialogOpen(viewState, viewEvent.forStart)
            is ScanScreenEvent.OnLostSearchMonthsInputChanged ->
                viewState = lostSearch.monthsInputChanged(viewState, viewEvent.value)
            ScanScreenEvent.OnLostSearchMonthsConfirm ->
                viewState = lostSearch.monthsConfirm(viewState)
            ScanScreenEvent.OnLostSearchMonthsDismiss ->
                viewState = lostSearch.monthsDismiss(viewState)
            is ScanScreenEvent.OnLostSearchAddDay ->
                viewState = lostSearch.addDay(viewState, viewEvent.date)
            is ScanScreenEvent.OnLostSearchRemoveDay -> viewState = viewState.copy(
                lostSearchExpectedDays = viewState.lostSearchExpectedDays.filterNot {
                    it.date == viewEvent.date
                },
            )
            is ScanScreenEvent.OnLostSearchDayExactChanged ->
                viewState = lostSearch.dayExactChanged(viewState, viewEvent.date, viewEvent.exact)
            is ScanScreenEvent.OnLostSearchDayToleranceChanged ->
                viewState = lostSearch.dayToleranceChanged(
                    viewState,
                    viewEvent.date,
                    viewEvent.tolerance,
                )
            is ScanScreenEvent.OnLostSearchDayDrinkTypeChanged ->
                viewState = lostSearch.dayDrinkTypeChanged(
                    viewState,
                    viewEvent.date,
                    viewEvent.drinkType,
                )
            ScanScreenEvent.OnLostSearchRun -> runLostSearch()
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
                    errorMessage = e.message ?: "Failed to initialize Firebase",
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
                    viewAction = scanActionFor(error)
                }
        }
    }

    private fun scanActionFor(error: Error): ScanScreenAction = when (error) {
        is ScanError -> when (error) {
            ScanError.DailyLimitReached -> ScanScreenAction.ShowDailyLimitReached(viewState.maxReads)
            ScanError.FirebaseNotInitialized -> ScanScreenAction.FirebaseNotInitialized
            else -> ScanScreenAction.ShowError(error.name)
        }
        else -> ScanScreenAction.UnknownError
    }

    private fun onStopScan() {
        withViewModelScope {
            scanRepository.stopScan()
        }
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

    private fun loadBackupEntries() {
        withViewModelScope {
            restore.loadEntries()
                .onSuccess { entries -> viewState = restore.show(viewState, entries) }
                .onError { error ->
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun confirmRestore() {
        val entry: BackupEntry = viewState.selectedBackupEntry ?: return
        viewState = restore.startRestore(viewState)
        withViewModelScope {
            restore.restore(entry)
                .onSuccess {
                    viewState = restore.applySuccess(viewState)
                    viewAction = ScanScreenAction.ShowRestoreComplete(entry.user.id)
                }
                .onError { error ->
                    viewState = restore.stopRestoring(viewState)
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun loadOrphanedRegistryUsers() {
        viewState = registryCleanup.show(viewState)
        withViewModelScope {
            registryCleanup.findOrphaned()
                .onSuccess { (total, users) ->
                    viewState = registryCleanup.applyLoaded(viewState, total, users)
                }
                .onError { error ->
                    viewState = registryCleanup.stopLoading(viewState)
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun removeAllOrphanedUsers() {
        val users = viewState.orphanedRegistryUsers
        if (users.isEmpty()) return
        viewState = registryCleanup.startRemoveAll(viewState)
        withViewModelScope {
            registryCleanup.removeAll(users)
                .onSuccess {
                    viewState = registryCleanup.applyRemoveAllSuccess(viewState)
                    viewAction = ScanScreenAction.ShowActualizeComplete(users.size)
                }
                .onError { error ->
                    viewState = registryCleanup.stopRemoving(viewState)
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun removeSingleOrphanedUser(email: String) {
        withViewModelScope {
            registryCleanup.removeSingle(email)
                .onSuccess {
                    viewState = registryCleanup.applyRemoveSingle(viewState, email)
                    viewAction = ScanScreenAction.ShowActualizeComplete(1)
                }
                .onError { error ->
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun loadAuthOrphans() {
        viewState = authCleanup.show(viewState)
        withViewModelScope {
            authCleanup.findOrphaned()
                .onSuccess { emails ->
                    viewState = authCleanup.applyLoaded(viewState, emails)
                }
                .onError { error ->
                    viewState = authCleanup.stopChecking(viewState)
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun deleteAllAuthOrphans() {
        viewState = authCleanup.startDeleteAll(viewState)
        withViewModelScope {
            val deleted = authCleanup.deleteAll(viewState.authOrphanEmails.toList())
            viewState = authCleanup.applyDeleteAllSuccess(viewState)
            viewAction = ScanScreenAction.ShowAuthCheckComplete(deleted)
        }
    }

    private fun deleteSingleAuthOrphan(email: String) {
        withViewModelScope {
            authCleanup.deleteSingle(email)
                .onSuccess {
                    viewState = authCleanup.applyDeleteSingle(viewState, email)
                    viewAction = ScanScreenAction.ShowAuthCheckComplete(1)
                }
                .onError { error ->
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }

    private fun runLostSearch() {
        if (viewState.isLostSearchRunning) return
        val (prepared, criteria) = lostSearch.prepareRun(viewState)
        viewState = prepared
        withViewModelScope {
            lostSearch.search(criteria, viewState.readLimit)
                .onSuccess { result ->
                    viewState = lostSearch.applyResult(viewState, result)
                }
                .onError { error ->
                    viewState = lostSearch.stopRunning(viewState)
                    viewAction = ScanScreenAction.ShowError(error.toString())
                }
        }
    }
}
