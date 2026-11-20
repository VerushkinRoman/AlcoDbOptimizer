package com.alcodb.optimizer.feature_scan.presentation.models

import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.ExpectedDrinkType
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser
import kotlinx.datetime.LocalDate

sealed interface ScanScreenEvent {
    data object ClearAction : ScanScreenEvent
    data object OnInitFirebase : ScanScreenEvent
    data object OnStartScan : ScanScreenEvent
    data object OnStopScan : ScanScreenEvent
    data object OnDeleteAllClick : ScanScreenEvent
    data object OnDeleteAllConfirm : ScanScreenEvent
    data object OnDeleteAllDismiss : ScanScreenEvent
    data object OnDeleteFailedDismiss : ScanScreenEvent
    data object OnDeleteFailedRetry : ScanScreenEvent
    data object OnDeleteFailedExport : ScanScreenEvent
    data class OnUserClick(val user: InactiveUser) : ScanScreenEvent
    data object OnUserDetailDismiss : ScanScreenEvent
    data object OnUserDetailToggleAllDates : ScanScreenEvent
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
    data object OnToggleRegisteredOnly : ScanScreenEvent
    data object OnAuthCheckClick : ScanScreenEvent
    data object OnAuthCheckDismiss : ScanScreenEvent
    data class OnAuthCheckFilterChanged(val filter: String) : ScanScreenEvent
    data object OnAuthCheckDeleteAll : ScanScreenEvent
    data class OnAuthCheckDeleteSingle(val email: String) : ScanScreenEvent
    data object OnLostSearchClick : ScanScreenEvent
    data object OnLostSearchDismiss : ScanScreenEvent
    data class OnLostSearchDatePickerOpen(val target: LostSearchDateTarget) : ScanScreenEvent
    data object OnLostSearchDatePickerDismiss : ScanScreenEvent
    data class OnLostSearchStartDateChanged(val date: LocalDate) : ScanScreenEvent
    data class OnLostSearchEndDateChanged(val date: LocalDate) : ScanScreenEvent
    data class OnLostSearchCollectionFilterChanged(val filter: String) : ScanScreenEvent
    data class OnLostSearchPeakMonthChanged(val date: LocalDate) : ScanScreenEvent
    data class OnLostSearchPeakMinDaysChanged(val days: Int) : ScanScreenEvent
    data class OnLostSearchPeakMaxDaysChanged(val days: Int) : ScanScreenEvent
    data class OnLostSearchPeakMinConsecutiveChanged(val days: Int) : ScanScreenEvent
    data class OnLostSearchQuietToggle(val enabled: Boolean) : ScanScreenEvent
    data class OnLostSearchQuietFromChanged(val date: LocalDate) : ScanScreenEvent
    data class OnLostSearchQuietToChanged(val date: LocalDate) : ScanScreenEvent
    data class OnLostSearchQuietMaxChanged(val days: Int) : ScanScreenEvent
    data class OnLostSearchStartExactChanged(val exact: Boolean) : ScanScreenEvent
    data class OnLostSearchEndExactChanged(val exact: Boolean) : ScanScreenEvent
    data class OnLostSearchMonthsDialogOpen(val forStart: Boolean) : ScanScreenEvent
    data class OnLostSearchMonthsInputChanged(val value: String) : ScanScreenEvent
    data object OnLostSearchMonthsConfirm : ScanScreenEvent
    data object OnLostSearchMonthsDismiss : ScanScreenEvent
    data class OnLostSearchAddDay(val date: LocalDate) : ScanScreenEvent
    data class OnLostSearchRemoveDay(val date: LocalDate) : ScanScreenEvent
    data class OnLostSearchDayExactChanged(val date: LocalDate, val exact: Boolean) : ScanScreenEvent
    data class OnLostSearchDayToleranceChanged(val date: LocalDate, val tolerance: Int) : ScanScreenEvent
    data class OnLostSearchDayDrinkTypeChanged(
        val date: LocalDate,
        val drinkType: ExpectedDrinkType,
    ) : ScanScreenEvent
    data object OnLostSearchRun : ScanScreenEvent
}
