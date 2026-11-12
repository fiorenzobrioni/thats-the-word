package com.callbackdev.thatstheword.core.domain.daily

import com.callbackdev.thatstheword.core.domain.daily
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/** The calendar of the dailies (PLANNING.md §4.6, §11 Phase 4): one month, read right. */
class ArchiveMonthTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val epoch = LocalDate.of(2026, 11, 12)
    private val today = LocalDate.of(2026, 12, 20)
    private val calendar = DailyCalendar(
        Clock.fixed(ZonedDateTime.of(2026, 12, 20, 9, 0, 0, 0, rome).toInstant(), rome),
        epoch,
    )
    private val december = YearMonth.of(2026, 12)
    private val it = Language.ITALIAN

    private fun month(
        records: List<com.callbackdev.thatstheword.core.domain.progress.GameRecord>,
        month: YearMonth = december,
    ) = ArchiveMonth.of(month, records, it, calendar, scheduleLength = 3_654)

    private fun ArchiveMonth.day(n: Int) = days[n - 1]

    @Test
    fun `a month with solved, missed, unplayed and archive days`() {
        val records = listOf(
            daily(LocalDate.of(2026, 12, 1), GameState.WON),
            daily(LocalDate.of(2026, 12, 2), GameState.LOST),
            // Caught up from the archive: played on the 10th for the 3rd.
            daily(LocalDate.of(2026, 12, 3), GameState.WON, startedOn = LocalDate.of(2026, 12, 10)),
            daily(LocalDate.of(2026, 12, 4), GameState.LOST, startedOn = LocalDate.of(2026, 12, 10)),
            daily(LocalDate.of(2026, 12, 5), GameState.PLAYING),
            daily(today, GameState.WON),
        )
        val shown = month(records)
        assertThat(shown.days).hasSize(31)
        assertThat(shown.day(1).status).isEqualTo(DayStatus.SOLVED)
        assertThat(shown.day(1).fromArchive).isFalse()
        assertThat(shown.day(2).status).isEqualTo(DayStatus.MISSED)
        assertThat(shown.day(3).status).isEqualTo(DayStatus.SOLVED)
        assertThat(shown.day(3).fromArchive).isTrue()
        assertThat(shown.day(4).status).isEqualTo(DayStatus.MISSED)
        assertThat(shown.day(4).fromArchive).isTrue()
        assertThat(shown.day(5).status).isEqualTo(DayStatus.IN_PROGRESS)
        assertThat(shown.day(6).status).isEqualTo(DayStatus.NOT_PLAYED)
        assertThat(shown.day(6).playable).isTrue()
        assertThat(shown.day(20).today).isTrue()
        assertThat(shown.day(20).status).isEqualTo(DayStatus.SOLVED)
        assertThat(shown.found).isEqualTo(3)
        assertThat(shown.missed).isEqualTo(2)
        assertThat(shown.caughtUp).isEqualTo(2)
    }

    @Test
    fun `the days after today are not playable, nor those before the epoch`() {
        assertThat(month(emptyList()).day(21).playable).isFalse()
        assertThat(month(emptyList()).day(21).future).isTrue()
        assertThat(month(emptyList()).day(20).future).isFalse()
        assertThat(month(emptyList()).day(20).playable).isTrue()
        val november = month(emptyList(), YearMonth.of(2026, 11))
        assertThat(november.day(11).playable).isFalse()
        assertThat(november.day(11).future).isFalse()
        assertThat(november.day(12).playable).isTrue()
    }

    @Test
    fun `a replay as practice leaves the day as its first result made it`() {
        val records = listOf(
            daily(LocalDate.of(2026, 12, 1), GameState.LOST),
            daily(LocalDate.of(2026, 12, 1), GameState.WON, practice = true),
        )
        assertThat(month(records).day(1).status).isEqualTo(DayStatus.MISSED)
        assertThat(month(records).found).isEqualTo(0)
    }

    @Test
    fun `each language has its own calendar`() {
        val records = listOf(daily(LocalDate.of(2026, 12, 1), GameState.WON, language = Language.SPANISH))
        assertThat(month(records).day(1).status).isEqualTo(DayStatus.NOT_PLAYED)
    }

    @Test
    fun `the months run from the epoch's to today's, and a month outside them is brought back`() {
        assertThat(month(emptyList()).hasPrevious).isTrue()
        assertThat(month(emptyList()).hasNext).isFalse()
        val november = month(emptyList(), YearMonth.of(2026, 11))
        assertThat(november.hasPrevious).isFalse()
        assertThat(november.hasNext).isTrue()
        assertThat(month(emptyList(), YearMonth.of(2027, 3)).month).isEqualTo(december)
        assertThat(month(emptyList(), YearMonth.of(2025, 1)).month).isEqualTo(YearMonth.of(2026, 11))
    }

    @Test
    fun `before the epoch's month there is only the epoch's`() {
        val early = DailyCalendar(
            Clock.fixed(ZonedDateTime.of(2026, 10, 1, 9, 0, 0, 0, rome).toInstant(), rome),
            epoch,
        )
        val shown = ArchiveMonth.of(YearMonth.of(2026, 10), emptyList(), it, early, 3_654)
        assertThat(shown.month).isEqualTo(YearMonth.of(2026, 11))
        assertThat(shown.days.none { it.playable }).isTrue()
        assertThat(shown.hasNext).isFalse()
    }
}
