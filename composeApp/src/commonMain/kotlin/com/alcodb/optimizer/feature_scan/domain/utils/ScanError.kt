package com.alcodb.optimizer.feature_scan.domain.utils

import com.alcodb.optimizer.common.domain.utils.Error

enum class ScanError : Error {
    FirebaseNotInitialized,
    FailedToReadDatabase,
    FailedToDeleteUser,
    FailedToLoadProgress,
    FailedToSaveProgress,
    DailyLimitReached,
    ScanStopped,
    NoDataAvailable,
    FailedToExportUsers,
}
