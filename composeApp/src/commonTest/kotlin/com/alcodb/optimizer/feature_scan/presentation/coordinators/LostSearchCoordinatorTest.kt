package com.alcodb.optimizer.feature_scan.presentation.coordinators

import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.ExpectedDay
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchCriteria
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchResult
import com.alcodb.optimizer.feature_scan.domain.models.LowActivityPeriod
import com.alcodb.optimizer.feature_scan.domain.models.PeakMonthCriteria
import com.alcodb.optimizer.feature_scan.domain.repository.LostDataSearchRepository
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LostSearchCoordinatorTest {
    private val coordinator = LostSearchCoordinator(FakeLostDataSearchRepository())

    @Test
    fun emptyCriteriaCannotRun() {
        assertFalse(coordinator.canRun(ScanScreenState()))
    }

    @Test
    fun dateCriteriaAllowsRun() {
        val state = ScanScreenState(lostSearchStartDate = LocalDate(2024, 1, 1))

        assertTrue(coordinator.canRun(state))
    }

    @Test
    fun invalidQuietPeriodBlocksRun() {
        val state = ScanScreenState(
            lostSearchQuietEnabled = true,
            lostSearchQuietFrom = LocalDate(2024, 8, 1),
            lostSearchQuietTo = LocalDate(2024, 6, 1),
        )

        assertFalse(coordinator.canRun(state))
    }

    @Test
    fun prepareRunBuildsCriteriaAndResetsResults() {
        val start = LocalDate(2023, 1, 15)
        val end = LocalDate(2024, 2, 20)
        val peakMonth = LocalDate(2023, 7, 1)
        val quietFrom = LocalDate(2023, 6, 1)
        val quietTo = LocalDate(2023, 8, 1)
        val state = ScanScreenState(
            lostSearchStartDate = start,
            lostSearchStartExact = false,
            lostSearchStartMonths = 2,
            lostSearchEndDate = end,
            lostSearchEndExact = true,
            lostSearchEndMonths = 99,
            lostSearchCollectionFilter = "  user@example.com  ",
            lostSearchPeakMonth = peakMonth,
            lostSearchPeakMinDays = 3,
            lostSearchPeakMaxDays = 10,
            lostSearchPeakMinConsecutive = 4,
            lostSearchQuietEnabled = true,
            lostSearchQuietFrom = quietFrom,
            lostSearchQuietTo = quietTo,
            lostSearchQuietMaxPerMonth = 2,
            lostSearchResults = listOf(),
            lostSearchHasRun = true,
            lostSearchStoppedByLimit = true,
        )

        val (prepared, criteria) = coordinator.prepareRun(state)

        assertEquals(
            LostDataSearchCriteria(
                startDate = start,
                startExact = false,
                startMonthTolerance = 2,
                endDate = end,
                endExact = true,
                endMonthTolerance = 0,
                collectionFilter = "user@example.com",
                peakMonth = PeakMonthCriteria(
                    month = peakMonth,
                    minDrinkDays = 3,
                    maxDrinkDays = 10,
                    minConsecutiveDays = 4,
                ),
                lowActivityPeriod = LowActivityPeriod(
                    fromMonth = quietFrom,
                    toMonth = quietTo,
                    maxDrinkDaysPerMonth = 2,
                ),
            ),
            criteria,
        )
        assertTrue(prepared.isLostSearchRunning)
        assertFalse(prepared.lostSearchHasRun)
        assertEquals(emptyList(), prepared.lostSearchResults)
        assertEquals(0, prepared.lostSearchChecked)
        assertEquals(0, prepared.lostSearchTotal)
        assertFalse(prepared.lostSearchStoppedByLimit)
    }

    @Test
    fun invalidQuietPeriodIsNotAddedToCriteria() {
        val state = ScanScreenState(
            lostSearchQuietEnabled = true,
            lostSearchQuietFrom = LocalDate(2024, 8, 1),
            lostSearchQuietTo = LocalDate(2024, 6, 1),
        )

        val (_, criteria) = coordinator.prepareRun(state)

        assertEquals(null, criteria.lowActivityPeriod)
    }

    @Test
    fun addDayDeduplicatesAndSortsDates() {
        val later = LocalDate(2024, 5, 10)
        val earlier = LocalDate(2024, 4, 10)
        val state = ScanScreenState(
            lostSearchExpectedDays = listOf(ExpectedDay(later)),
        )

        val result = coordinator.addDay(coordinator.addDay(state, earlier), later)

        assertEquals(listOf(ExpectedDay(earlier), ExpectedDay(later)), result.lostSearchExpectedDays)
    }

    @Test
    fun monthsInputKeepsOnlyThreeDigits() {
        val result = coordinator.monthsInputChanged(
            ScanScreenState(),
            value = "a1b2c3d4",
        )

        assertEquals("123", result.lostSearchMonthsInput)
    }

    @Test
    fun monthsConfirmClampsAndFallsBack() {
        val tooLarge = coordinator.monthsConfirm(
            ScanScreenState(isLostSearchMonthsDialogVisible = true, lostSearchMonthsInput = "999"),
        )
        val empty = coordinator.monthsConfirm(
            ScanScreenState(isLostSearchMonthsDialogVisible = true, lostSearchMonthsInput = ""),
        )

        assertEquals(120, tooLarge.lostSearchStartMonths)
        assertEquals(1, empty.lostSearchStartMonths)
        assertFalse(tooLarge.isLostSearchMonthsDialogVisible)
    }

    @Test
    fun switchingToApproximateOpensMonthsDialog() {
        val result = coordinator.exactChanged(
            state = ScanScreenState(lostSearchStartExact = true, lostSearchStartMonths = 3),
            forStart = true,
            exact = false,
        )

        assertFalse(result.lostSearchStartExact)
        assertTrue(result.isLostSearchMonthsDialogVisible)
        assertTrue(result.lostSearchMonthsDialogForStart)
        assertEquals("3", result.lostSearchMonthsInput)
    }

    @Test
    fun dayToleranceIsClampedAndOnlyTargetDateChanges() {
        val target = LocalDate(2024, 1, 1)
        val other = LocalDate(2024, 2, 1)
        val state = ScanScreenState(
            lostSearchExpectedDays = listOf(ExpectedDay(target), ExpectedDay(other)),
        )

        val result = coordinator.dayToleranceChanged(state, target, tolerance = 500)

        assertEquals(365, result.lostSearchExpectedDays.first { it.date == target }.dayTolerance)
        assertEquals(0, result.lostSearchExpectedDays.first { it.date == other }.dayTolerance)
    }

    private class FakeLostDataSearchRepository : LostDataSearchRepository {
        override suspend fun searchLostData(
            criteria: LostDataSearchCriteria,
            maxReads: Int,
        ): Result<LostDataSearchResult, Error> = Result.Success(
            LostDataSearchResult(
                collections = emptyList(),
                checkedCollections = 0,
                totalCollections = 0,
                stoppedByLimit = false,
            ),
        )
    }
}
