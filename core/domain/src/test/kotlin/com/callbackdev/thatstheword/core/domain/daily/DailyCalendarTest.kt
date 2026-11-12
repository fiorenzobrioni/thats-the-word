package com.callbackdev.thatstheword.core.domain.daily

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyCalendarTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val london = ZoneId.of("Europe/London")
    private val epoch = LocalDate.of(2026, 11, 12)

    private fun calendarAt(time: ZonedDateTime) = DailyCalendar(Clock.fixed(time.toInstant(), time.zone), epoch)

    private fun at(text: String, zone: ZoneId = rome): ZonedDateTime = LocalDateTime.parse(text).atZone(zone)

    @Test
    fun `the epoch is daily number 1, and each date after it one more`() {
        val calendar = calendarAt(at("2026-11-12T09:00"))
        assertThat(calendar.number(epoch)).isEqualTo(1)
        assertThat(calendar.number(LocalDate.of(2026, 11, 13))).isEqualTo(2)
        assertThat(calendar.number(LocalDate.of(2027, 11, 12))).isEqualTo(366)
        assertThat(calendar.dateOf(366)).isEqualTo(LocalDate.of(2027, 11, 12))
    }

    @Test
    fun `the epoch is the owner's, the first of November 2026, as the words' script has it`() {
        assertThat(DailyCalendar.EPOCH).isEqualTo(LocalDate.of(2026, 11, 1))
    }

    @Test
    fun `a daily started 23-58 and finished 00-03 is the earlier date's`() {
        // The date is read once, at the start; the clock at the finish names the next day, and the
        // game keeps the date it was started on (the owner, PLANNING §15, Open 9).
        val start = calendarAt(at("2026-12-10T23:58"))
        val finish = calendarAt(at("2026-12-11T00:03"))
        val started = start.today()
        assertThat(started).isEqualTo(LocalDate.of(2026, 12, 10))
        assertThat(finish.today()).isEqualTo(LocalDate.of(2026, 12, 11))
        assertThat(start.number(started)).isEqualTo(29)
    }

    @Test
    fun `a daily played at 23-59 in Rome and at 00-01 after flying to London - each local date its own word`() {
        val inRome = calendarAt(at("2026-12-10T23:59", rome))
        // A day and a flight later, in London, an hour behind Rome.
        val inLondon = calendarAt(at("2026-12-12T00:01", london))
        assertThat(inRome.number(inRome.today())).isEqualTo(29)
        assertThat(inLondon.number(inLondon.today())).isEqualTo(31)
        // The same instant reads as a date in the clock's zone: 23:30 in London is the next day in Rome.
        val instant = at("2026-12-10T23:30", london).toInstant()
        assertThat(inLondon.dateOf(instant)).isEqualTo(LocalDate.of(2026, 12, 10))
        assertThat(inRome.dateOf(instant)).isEqualTo(LocalDate.of(2026, 12, 11))
    }

    @Test
    fun `the daily of a date before the epoch, or after today, is not playable`() {
        val calendar = calendarAt(at("2026-12-10T12:00"))
        assertThat(calendar.isPlayable(epoch.minusDays(1), 3654)).isFalse()
        assertThat(calendar.isPlayable(LocalDate.of(2026, 12, 11), 3654)).isFalse()
        assertThat(calendar.isPlayable(epoch, 3654)).isTrue()
        assertThat(calendar.isPlayable(LocalDate.of(2026, 12, 10), 3654)).isTrue()
    }

    @Test
    fun `a date beyond the schedule is not playable`() {
        val calendar = calendarAt(at("2026-12-10T12:00"))
        assertThat(calendar.isPlayable(LocalDate.of(2026, 12, 10), 28)).isFalse()
    }

    @Test
    fun `daylight saving's change at night - one date, one word`() {
        // Europe/Rome moves from 02:00 to 03:00 on 28 March 2027, and back from 03:00 to 02:00 on
        // 31 October 2027, when 02:30 happens twice.
        val before = calendarAt(at("2027-03-28T01:59"))
        val after = calendarAt(at("2027-03-28T03:01"))
        assertThat(before.today()).isEqualTo(after.today())
        assertThat(before.number(before.today())).isEqualTo(after.number(after.today()))

        val first = at("2027-10-31T02:30").withEarlierOffsetAtOverlap()
        val second = at("2027-10-31T02:30").withLaterOffsetAtOverlap()
        assertThat(first.toInstant()).isNotEqualTo(second.toInstant())
        assertThat(calendarAt(first).today()).isEqualTo(calendarAt(second).today())
        // And the day before the change and the day after are one number apart, not two or none.
        val late = calendarAt(at("2027-10-30T23:59"))
        val early = calendarAt(at("2027-11-01T00:01"))
        assertThat(early.number(early.today()) - late.number(late.today())).isEqualTo(2)
    }

    @Test
    fun `the archive is every date from the epoch to today`() {
        val calendar = calendarAt(at("2026-11-14T08:00"))
        assertThat(calendar.archive(3654))
            .containsExactly(epoch, LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14)).inOrder()
        assertThat(calendarAt(at("2026-11-11T08:00")).archive(3654)).isEmpty()
    }

    @Test
    fun `the next day begins at the zone's own midnight`() {
        assertThat(calendarAt(at("2026-12-10T23:58")).untilNextDay()).isEqualTo(Duration.ofMinutes(2))
        assertThat(calendarAt(at("2026-12-10T00:00")).untilNextDay()).isEqualTo(Duration.ofHours(24))
        // The night the clocks go forward: 22:00 to midnight is still two hours, the day itself 23.
        assertThat(calendarAt(at("2027-03-27T22:00")).untilNextDay()).isEqualTo(Duration.ofHours(2))
        assertThat(calendarAt(at("2027-03-28T00:00")).untilNextDay()).isEqualTo(Duration.ofHours(23))
        assertThat(calendarAt(at("2027-10-31T00:00")).untilNextDay()).isEqualTo(Duration.ofHours(25))
        // Santiago's clocks go from 24:00 to 01:00: its day begins at one, an hour after 23:00.
        val santiago = ZoneId.of("America/Santiago")
        assertThat(calendarAt(at("2026-09-05T23:00", santiago)).untilNextDay()).isEqualTo(Duration.ofHours(1))
    }
}
