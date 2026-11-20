package com.alcodb.optimizer.feature_scan.presentation.models

import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.ExpectedDay
import com.alcodb.optimizer.feature_scan.domain.models.FoundCollection
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser
import kotlinx.datetime.LocalDate

enum class LostSearchDateTarget { Start, End, New, PeakMonth, QuietFrom, QuietTo }

fun ScanScreenState.canRunLostSearch(): Boolean {
    val quietReady = !lostSearchQuietEnabled ||
        (
            lostSearchQuietFrom != null &&
                lostSearchQuietTo != null &&
                lostSearchQuietFrom <= lostSearchQuietTo
            )
    val hasCriteria = lostSearchStartDate != null ||
        lostSearchEndDate != null ||
        lostSearchExpectedDays.isNotEmpty() ||
        lostSearchCollectionFilter.isNotBlank() ||
        lostSearchPeakMonth != null ||
        lostSearchQuietEnabled
    return hasCriteria && quietReady
}

data class ScanScreenState(
    val inactiveUsers: List<InactiveUser> = emptyList(),
    val isScanning: Boolean = false,
    val isDeleting: Boolean = false,
    val deletedCount: Int = 0,
    val deletionTotal: Int = 0,
    val collectionsTotal: Int = 0,
    val collectionsProcessed: Int = 0,
    val readsUsed: Int = 0,
    val maxReads: Int = 1000,
    val readLimit: Int = 1000,
    val selectedUser: InactiveUser? = null,
    val showAllDatesInUserDetail: Boolean = false,
    val isDeleteAllConfirmVisible: Boolean = false,
    val isDeleteFailedDialogVisible: Boolean = false,
    val failedDeletionUsers: List<InactiveUser> = emptyList(),
    val exportedFailedUsersPath: String? = null,
    val failedUsersExportError: Boolean = false,
    val isDeleteSingleConfirmVisible: Boolean = false,
    val isInitializingFirebase: Boolean = true,
    val errorMessage: String? = null,
    val lastScanDate: Long = 0L,
    val canScanToday: Boolean = true,
    val isComplete: Boolean = false,
    val isRestoreDialogVisible: Boolean = false,
    val backupEntries: List<BackupEntry> = emptyList(),
    val backupFilter: String = "",
    val selectedBackupEntry: BackupEntry? = null,
    val isRestoring: Boolean = false,
    val isActualizeDialogVisible: Boolean = false,
    val isActualizing: Boolean = false,
    val orphanedRegistryUsers: List<OrphanedRegistryUser> = emptyList(),
    val totalRegistryUsers: Int = 0,
    val actualizeFilter: String = "",
    val isRemovingOrphans: Boolean = false,
    val showOnlyRegistered: Boolean = false,
    val isAuthOrphanDialogVisible: Boolean = false,
    val isCheckingAuthOrphans: Boolean = false,
    val authOrphanEmails: List<String> = emptyList(),
    val authOrphanFilter: String = "",
    val isDeletingAuthOrphans: Boolean = false,
    val isLostSearchDialogVisible: Boolean = false,
    val lostSearchStartDate: LocalDate? = null,
    val lostSearchStartExact: Boolean = true,
    val lostSearchStartMonths: Int = 1,
    val lostSearchEndDate: LocalDate? = null,
    val lostSearchEndExact: Boolean = true,
    val lostSearchEndMonths: Int = 1,
    val lostSearchExpectedDays: List<ExpectedDay> = emptyList(),
    val lostSearchCollectionFilter: String = "",
    val lostSearchPeakMonth: LocalDate? = null,
    val lostSearchPeakMinDays: Int = 0,
    val lostSearchPeakMaxDays: Int = 0,
    val lostSearchPeakMinConsecutive: Int = 0,
    val lostSearchQuietEnabled: Boolean = false,
    val lostSearchQuietFrom: LocalDate? = null,
    val lostSearchQuietTo: LocalDate? = null,
    val lostSearchQuietMaxPerMonth: Int = 8,
    val isLostSearchRunning: Boolean = false,
    val lostSearchChecked: Int = 0,
    val lostSearchTotal: Int = 0,
    val lostSearchResults: List<FoundCollection> = emptyList(),
    val lostSearchStoppedByLimit: Boolean = false,
    val lostSearchHasRun: Boolean = false,
    val isLostSearchMonthsDialogVisible: Boolean = false,
    val lostSearchMonthsDialogForStart: Boolean = true,
    val lostSearchMonthsInput: String = "",
    val lostSearchDatePickerTarget: LostSearchDateTarget? = null,
)
