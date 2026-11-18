package com.alcodb.optimizer.feature_scan.domain.models

import kotlinx.datetime.LocalDate

enum class ExpectedDrinkType {
    Any,
    Full,
    Half,
}

/**
 * A date that must be present in a matching calendar.
 *
 * [exact] = false means the date may be off by up to [dayTolerance] days.
 */
data class ExpectedDay(
    val date: LocalDate,
    val exact: Boolean = true,
    val dayTolerance: Int = 0,
    val drinkType: ExpectedDrinkType = ExpectedDrinkType.Any,
)

/**
 * Requirement for the busiest month of a calendar.
 *
 * [month] is any date of the target month; [maxDrinkDays] = 0 means no upper bound.
 */
data class PeakMonthCriteria(
    val month: LocalDate,
    val minDrinkDays: Int = 0,
    val maxDrinkDays: Int = 0,
    val minConsecutiveDays: Int = 0,
)

/**
 * Requirement for months with few entries (e.g. summer holidays).
 *
 * Every month from [fromMonth] to [toMonth] must contain no more than
 * [maxDrinkDaysPerMonth] marked drink days.
 */
data class LowActivityPeriod(
    val fromMonth: LocalDate,
    val toMonth: LocalDate,
    val maxDrinkDaysPerMonth: Int,
)

/**
 * Criteria of the lost-data search.
 *
 * [startDate]/[endDate] are compared with the earliest/latest marked day of a calendar.
 * [startExact]/[endExact] = false allows a discrepancy of [startMonthTolerance]/[endMonthTolerance]
 * months.
 */
data class LostDataSearchCriteria(
    val startDate: LocalDate? = null,
    val startExact: Boolean = true,
    val startMonthTolerance: Int = 0,
    val endDate: LocalDate? = null,
    val endExact: Boolean = true,
    val endMonthTolerance: Int = 0,
    val expectedDays: List<ExpectedDay> = emptyList(),
    val collectionFilter: String = "",
    val peakMonth: PeakMonthCriteria? = null,
    val lowActivityPeriod: LowActivityPeriod? = null,
)

data class FoundCollection(
    val id: String,
    val totalDays: Int,
    val drinkDays: Int,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val matchedExpectedDays: Int,
    val peakDrinkDays: Int = 0,
    val maxConsecutiveDays: Int = 0,
)

data class LostDataSearchResult(
    val collections: List<FoundCollection>,
    val checkedCollections: Int,
    val totalCollections: Int,
    val stoppedByLimit: Boolean,
)
