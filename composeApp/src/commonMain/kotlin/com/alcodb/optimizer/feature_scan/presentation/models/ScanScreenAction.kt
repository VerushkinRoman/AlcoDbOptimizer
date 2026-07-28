package com.alcodb.optimizer.feature_scan.presentation.models

sealed interface ScanScreenAction {
    data class ShowError(val message: String) : ScanScreenAction
    data object ShowScanComplete : ScanScreenAction
    data class ShowDailyLimitReached(val limit: Int) : ScanScreenAction
    data class ShowDeleteComplete(val count: Int) : ScanScreenAction
    data class ShowUserDeleted(val userId: String) : ScanScreenAction
    data object FirebaseInitialized : ScanScreenAction
    data object FirebaseInitFailed : ScanScreenAction
    data class ShowRestoreComplete(val userId: String) : ScanScreenAction
    data class ShowActualizeComplete(val removedCount: Int) : ScanScreenAction
}
