package com.callbackdev.thatstheword.core.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** The change of day seen live (PLANNING.md §11 Phase 4): one wait until midnight, the new date then. */
@OptIn(ExperimentalCoroutinesApi::class)
class DayTickerTest {
    private val rome = ZoneId.of("Europe/Rome")

    /** A clock that runs with the test's virtual time, from [start]. */
    private fun TestScope.clockFrom(start: ZonedDateTime): Clock = object : Clock() {
        override fun getZone(): ZoneId = start.zone

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = start.toInstant().plusMillis(currentTime)
    }

    @Test
    fun `today, then the next day at midnight, and nothing between`() = runTest {
        val clock = clockFrom(ZonedDateTime.of(2026, 11, 20, 23, 59, 0, 0, rome))
        val dates = DayTicker { clock }.dates.take(3).toList()
        assertThat(dates).containsExactly(
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 11, 21),
            LocalDate.of(2026, 11, 22),
        ).inOrder()
        // Two midnights waited for, a minute and a day: no wake between them.
        assertThat(currentTime).isLessThan((60_000L + 86_400_000L) + 2_000L)
        assertThat(currentTime).isAtLeast(60_000L + 86_400_000L)
    }
}
