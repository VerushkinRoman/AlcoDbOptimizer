package com.alcodb.optimizer.feature_scan.presentation.coordinators

import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.ExpectedDay
import com.alcodb.optimizer.feature_scan.domain.models.ExpectedDrinkType
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchCriteria
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchResult
import com.alcodb.optimizer.feature_scan.domain.repository.LostDataSearchRepository
import com.alcodb.optimizer.feature_scan.presentation.models.LostSearchDateTarget
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState
import kotlinx.datetime.LocalDate

/**
 * Состояние диалога поиска потерянных данных и запуск самого поиска.
 */
internal class LostSearchCoordinator(
    private val repository: LostDataSearchRepository,
) {
    fun open(state: ScanScreenState): ScanScreenState = state.copy(
        isLostSearchDialogVisible = true,
        lostSearchHasRun = false,
        lostSearchResults = emptyList(),
        lostSearchChecked = 0,
        lostSearchTotal = 0,
        lostSearchStoppedByLimit = false,
    )

    fun dismiss(state: ScanScreenState): ScanScreenState = state.copy(
        isLostSearchDialogVisible = false,
        isLostSearchMonthsDialogVisible = false,
        lostSearchDatePickerTarget = null,
    )

    fun datePickerOpen(state: ScanScreenState, target: LostSearchDateTarget): ScanScreenState =
        state.copy(lostSearchDatePickerTarget = target)

    fun datePickerDismiss(state: ScanScreenState): ScanScreenState =
        state.copy(lostSearchDatePickerTarget = null)

    fun exactChanged(state: ScanScreenState, forStart: Boolean, exact: Boolean): ScanScreenState {
        val updated = if (forStart) {
            state.copy(lostSearchStartExact = exact)
        } else {
            state.copy(lostSearchEndExact = exact)
        }
        return if (exact) updated else monthsDialogOpen(updated, forStart)
    }

    fun monthsDialogOpen(state: ScanScreenState, forStart: Boolean): ScanScreenState {
        val current = if (forStart) state.lostSearchStartMonths else state.lostSearchEndMonths
        return state.copy(
            isLostSearchMonthsDialogVisible = true,
            lostSearchMonthsDialogForStart = forStart,
            lostSearchMonthsInput = current.toString(),
        )
    }

    fun monthsInputChanged(state: ScanScreenState, value: String): ScanScreenState =
        state.copy(lostSearchMonthsInput = value.filter { it.isDigit() }.take(3))

    fun monthsConfirm(state: ScanScreenState): ScanScreenState {
        val months = state.lostSearchMonthsInput.toIntOrNull()?.coerceIn(0, 120) ?: 1
        return if (state.lostSearchMonthsDialogForStart) {
            state.copy(
                lostSearchStartMonths = months,
                isLostSearchMonthsDialogVisible = false,
            )
        } else {
            state.copy(
                lostSearchEndMonths = months,
                isLostSearchMonthsDialogVisible = false,
            )
        }
    }

    fun monthsDismiss(state: ScanScreenState): ScanScreenState =
        state.copy(isLostSearchMonthsDialogVisible = false)

    fun addDay(state: ScanScreenState, date: LocalDate): ScanScreenState {
        if (state.lostSearchExpectedDays.any { it.date == date }) return state
        return state.copy(
            lostSearchExpectedDays = (state.lostSearchExpectedDays + ExpectedDay(date))
                .sortedBy { it.date },
        )
    }

    fun dayExactChanged(state: ScanScreenState, date: LocalDate, exact: Boolean): ScanScreenState =
        state.copy(
            lostSearchExpectedDays = state.lostSearchExpectedDays.map { day ->
                if (day.date == date) day.copy(exact = exact) else day
            },
        )

    fun dayToleranceChanged(state: ScanScreenState, date: LocalDate, tolerance: Int): ScanScreenState =
        state.copy(
            lostSearchExpectedDays = state.lostSearchExpectedDays.map { day ->
                if (day.date == date) day.copy(dayTolerance = tolerance.coerceIn(0, 365)) else day
            },
        )

    fun dayDrinkTypeChanged(
        state: ScanScreenState,
        date: LocalDate,
        drinkType: ExpectedDrinkType,
    ): ScanScreenState = state.copy(
        lostSearchExpectedDays = state.lostSearchExpectedDays.map { day ->
            if (day.date == date) day.copy(drinkType = drinkType) else day
        },
    )

    fun prepareRun(state: ScanScreenState): Pair<ScanScreenState, LostDataSearchCriteria> {
        val criteria = LostDataSearchCriteria(
            startDate = state.lostSearchStartDate,
            startExact = state.lostSearchStartExact,
            startMonthTolerance = if (state.lostSearchStartExact) 0 else state.lostSearchStartMonths,
            endDate = state.lostSearchEndDate,
            endExact = state.lostSearchEndExact,
            endMonthTolerance = if (state.lostSearchEndExact) 0 else state.lostSearchEndMonths,
            expectedDays = state.lostSearchExpectedDays,
        )
        return state.copy(
            isLostSearchRunning = true,
            lostSearchHasRun = false,
            lostSearchResults = emptyList(),
            lostSearchChecked = 0,
            lostSearchTotal = 0,
            lostSearchStoppedByLimit = false,
        ) to criteria
    }

    suspend fun search(
        criteria: LostDataSearchCriteria,
        maxReads: Int,
    ): Result<LostDataSearchResult, Error> = repository.searchLostData(criteria, maxReads)

    fun applyResult(
        state: ScanScreenState,
        result: LostDataSearchResult,
    ): ScanScreenState = state.copy(
        isLostSearchRunning = false,
        lostSearchHasRun = true,
        lostSearchResults = result.collections,
        lostSearchChecked = result.checkedCollections,
        lostSearchTotal = result.totalCollections,
        lostSearchStoppedByLimit = result.stoppedByLimit,
    )

    fun stopRunning(state: ScanScreenState): ScanScreenState =
        state.copy(isLostSearchRunning = false)
}
