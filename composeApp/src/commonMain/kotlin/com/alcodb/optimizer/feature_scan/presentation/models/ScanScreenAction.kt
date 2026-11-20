package com.alcodb.optimizer.feature_scan.presentation.models

sealed interface ScanScreenAction {
    data class ShowError(val message: String) : ScanScreenAction
    data object ShowScanComplete : ScanScreenAction
    data object ShowScanStopped : ScanScreenAction
    data class ShowDailyLimitReached(val limit: Int) : ScanScreenAction
    data class ShowDeleteComplete(
        val deleted: Int,
        val total: Int,
        val failed: Int,
    ) : ScanScreenAction
    data class ShowUserDeleted(val userId: String) : ScanScreenAction
    data object FirebaseInitialized : ScanScreenAction
    data object FirebaseInitFailed : ScanScreenAction
    data object FirebaseNotInitialized : ScanScreenAction
    data object UnknownError : ScanScreenAction
    data class ShowRestoreComplete(val userId: String) : ScanScreenAction
    data class ShowActualizeComplete(val removedCount: Int) : ScanScreenAction
    data class ShowAuthCheckComplete(val removedCount: Int) : ScanScreenAction
}
