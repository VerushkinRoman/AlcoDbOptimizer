package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import kotlin.test.Test
import kotlin.test.assertEquals

class FailedUsersCsvTest {
    @Test
    fun csvContainsHeaderAndOneUserPerLine() {
        val csv = buildFailedUsersCsv(
            listOf(
                user("first@example.com"),
                user("anonymous-id"),
            ),
        )

        assertEquals(
            """
            user_id
            first@example.com
            anonymous-id

            """.trimIndent(),
            csv,
        )
    }

    @Test
    fun csvEscapesCommasQuotesAndLineBreaks() {
        val csv = buildFailedUsersCsv(
            listOf(
                user("with,comma"),
                user("with\"quote"),
                user("with\nline"),
            ),
        )

        assertEquals(
            """
            user_id
            "with,comma"
            "with""quote"
            "with
            line"

            """.trimIndent(),
            csv,
        )
    }

    @Test
    fun emptyListStillProducesHeader() {
        assertEquals("user_id\n", buildFailedUsersCsv(emptyList()))
    }

    private fun user(id: String) = InactiveUser(
        id = id,
        isAnonymous = true,
        lastDrinkDate = 0L,
        allDates = emptyMap(),
        totalDays = 0,
        drinkDays = 0,
    )
}
