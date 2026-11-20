package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.models.DataDayData
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock

class InactiveUserDetectorTest {
    private val detector = InactiveUserDetector()

    @Test
    fun emptyCalendarIsInactiveWithZeroDates() {
        val user = detector.detect(
            collectionId = "anonymous-id",
            dates = emptyMap(),
            isAnonymous = true,
        )

        assertNotNull(user)
        assertEquals(0L, user.lastDrinkDate)
        assertEquals(emptyMap(), user.allDates)
        assertEquals(0, user.totalDays)
        assertEquals(0, user.drinkDays)
    }

    @Test
    fun calendarWithoutDrinkDaysIsNotInactive() {
        val dates = mapOf(
            "2024-01-01" to DataDayData(date = daysAgo(800), drinkType = null),
        )

        assertNull(detector.detect("anonymous-id", dates, isAnonymous = true))
    }

    @Test
    fun anonymousUserWithinThresholdIsActive() {
        val dates = mapOf("day" to DataDayData(date = daysAgo(100), drinkType = "beer"))

        assertNull(detector.detect("anonymous-id", dates, isAnonymous = true))
    }

    @Test
    fun anonymousUserBeyondThresholdIsInactive() {
        val lastDrink = daysAgo(200)
        val dates = mapOf("day" to DataDayData(date = lastDrink, drinkType = "beer"))

        val user = detector.detect("anonymous-id", dates, isAnonymous = true)

        assertNotNull(user)
        assertEquals(lastDrink, user.lastDrinkDate)
        assertEquals(1, user.totalDays)
        assertEquals(1, user.drinkDays)
    }

    @Test
    fun registeredUserUsesLongerThreshold() {
        val dates = mapOf("day" to DataDayData(date = daysAgo(800), drinkType = "wine"))

        val user = detector.detect("user@example.com", dates, isAnonymous = false)

        assertNotNull(user)
        assertEquals(daysAgo(800), user.lastDrinkDate)
    }

    @Test
    fun registeredUserWithinThresholdIsActive() {
        val dates = mapOf("day" to DataDayData(date = daysAgo(600), drinkType = "wine"))

        assertNull(detector.detect("user@example.com", dates, isAnonymous = false))
    }

    @Test
    fun latestDrinkDateAndOnlyDrinkDatesAreKept() {
        val latestDrink = daysAgo(800)
        val dates = mapOf(
            "old-drink" to DataDayData(date = daysAgo(900), drinkType = "beer"),
            "latest-drink" to DataDayData(date = latestDrink, drinkType = "wine"),
            "no-drink" to DataDayData(date = daysAgo(850), drinkType = null),
        )

        val user = detector.detect("user@example.com", dates, isAnonymous = false)

        assertNotNull(user)
        assertEquals(latestDrink, user.lastDrinkDate)
        assertEquals(3, user.totalDays)
        assertEquals(2, user.drinkDays)
        assertEquals(setOf("old-drink", "latest-drink"), user.allDates.keys)
    }

    private fun daysAgo(days: Long): Long =
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toEpochDays() - days
}
