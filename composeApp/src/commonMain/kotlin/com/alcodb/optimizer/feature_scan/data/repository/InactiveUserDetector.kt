package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.models.DataDayData
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Определяет, является ли пользователь неактивным, по датам последнего употребления.
 */
internal class InactiveUserDetector {
    fun detect(
        collectionId: String,
        dates: Map<String, DataDayData>,
        isAnonymous: Boolean,
    ): InactiveUser? {
        if (dates.isEmpty()) {
            return InactiveUser(
                id = collectionId,
                isAnonymous = isAnonymous,
                lastDrinkDate = 0L,
                allDates = emptyMap(),
                totalDays = 0,
                drinkDays = 0,
            )
        }

        val drinkDates = dates.filter { it.value.drinkType != null }
        if (drinkDates.isEmpty()) return null

        val lastDrinkEntry = drinkDates.maxByOrNull { it.value.date } ?: return null
        val lastDrinkEpochDays = lastDrinkEntry.value.date

        val todayEpochDays = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toEpochDays()
        val daysSinceLastDrink = todayEpochDays - lastDrinkEpochDays

        val thresholdDays = if (isAnonymous) ANONYMOUS_THRESHOLD_DAYS else REGISTERED_THRESHOLD_DAYS
        if (daysSinceLastDrink <= thresholdDays) return null

        return InactiveUser(
            id = collectionId,
            isAnonymous = isAnonymous,
            lastDrinkDate = lastDrinkEpochDays,
            allDates = drinkDates.mapValues { it.value.date },
            totalDays = dates.size,
            drinkDays = drinkDates.size,
        )
    }

    companion object {
        private const val ANONYMOUS_THRESHOLD_DAYS = 183L
        private const val REGISTERED_THRESHOLD_DAYS = 730L
    }
}
