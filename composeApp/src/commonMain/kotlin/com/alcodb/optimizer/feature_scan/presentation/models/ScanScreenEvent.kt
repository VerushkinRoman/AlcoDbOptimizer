package com.alcodb.optimizer.feature_scan.presentation.models

import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser

sealed interface ScanScreenEvent {
    data object ClearAction : ScanScreenEvent
    data object OnInitFirebase : ScanScreenEvent
    data object OnStartScan : ScanScreenEvent
    data object OnStopScan : ScanScreenEvent
    data object OnDeleteAllClick : ScanScreenEvent
    data object OnDeleteAllConfirm : ScanScreenEvent
    data object OnDeleteAllDismiss : ScanScreenEvent
    data class OnUserClick(val user: InactiveUser) : ScanScreenEvent
    data object OnUserDetailDismiss : ScanScreenEvent
    data object OnDeleteSingleUserClick : ScanScreenEvent
    data object OnDeleteSingleConfirm : ScanScreenEvent
    data object OnDeleteSingleDismiss : ScanScreenEvent
    data class OnReadLimitChanged(val limit: Int) : ScanScreenEvent
    data object OnResetCounter : ScanScreenEvent
    data object OnResetAll : ScanScreenEvent
    data object OnRestoreClick : ScanScreenEvent
    data object OnRestoreDismiss : ScanScreenEvent
    data class OnRestoreFilterChanged(val filter: String) : ScanScreenEvent
    data class OnRestoreSelectEntry(val entry: BackupEntry) : ScanScreenEvent
    data object OnRestoreConfirm : ScanScreenEvent
    data object OnActualizeClick : ScanScreenEvent
    data object OnActualizeDismiss : ScanScreenEvent
    data class OnActualizeFilterChanged(val filter: String) : ScanScreenEvent
    data object OnActualizeRemoveAll : ScanScreenEvent
    data class OnActualizeRemoveSingle(val user: OrphanedRegistryUser) : ScanScreenEvent
}
