package com.callbackdev.thatstheword.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** The daily reminder's next moment (PLANNING.md §8), across daylight saving and zones. */
class ReminderScheduleTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val lisbon = ZoneId.of("Europe/Lisbon")
    private val eight = ReminderSchedule.DEFAULT_TIME

    private fun at(local: String, zone: ZoneId = rome): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun `before the time, today's`() {
        assertThat(ReminderSchedule.next(at("2026-11-05T10:00"), rome, eight)).isEqualTo(at("2026-11-05T20:00"))
    }

    @Test
    fun `at the time or after it, tomorrow's`() {
        assertThat(ReminderSchedule.next(at("2026-11-05T20:00"), rome, eight)).isEqualTo(at("2026-11-06T20:00"))
        assertThat(ReminderSchedule.next(at("2026-11-05T23:59"), rome, eight)).isEqualTo(at("2026-11-06T20:00"))
    }

    @Test
    fun `just after midnight, the evening of the new day`() {
        assertThat(ReminderSchedule.next(at("2026-11-06T00:01"), rome, eight)).isEqualTo(at("2026-11-06T20:00"))
    }

    @Test
    fun `on the day the clocks go forward the evening is twenty-three hours on`() {
        // 29 Mar 2026: Rome skips 02:00 to 03:00.
        val next = ReminderSchedule.next(at("2026-03-28T20:00"), rome, eight)
        assertThat(next).isEqualTo(at("2026-03-29T20:00"))
        assertThat(Duration.between(at("2026-03-28T20:00"), next)).isEqualTo(Duration.ofHours(23))
    }

    @Test
    fun `on the day the clocks go back the evening is twenty-five hours on`() {
        // 25 Oct 2026: Rome repeats 02:00 to 03:00.
        val next = ReminderSchedule.next(at("2026-10-24T20:00"), rome, eight)
        assertThat(next).isEqualTo(at("2026-10-25T20:00"))
        assertThat(Duration.between(at("2026-10-24T20:00"), next)).isEqualTo(Duration.ofHours(25))
    }

    @Test
    fun `a time the clocks skip moves forward by the gap`() {
        val next = ReminderSchedule.next(at("2026-03-29T00:30"), rome, LocalTime.of(2, 30))
        assertThat(next).isEqualTo(at("2026-03-29T03:30"))
    }

    @Test
    fun `a time the clocks repeat is the first of the two`() {
        val next = ReminderSchedule.next(at("2026-10-25T00:30"), rome, LocalTime.of(2, 30))
        // The first 02:30 is still summer time, an hour before the second.
        assertThat(next).isEqualTo(Instant.parse("2026-10-25T00:30:00Z"))
    }

    @Test
    fun `a new zone puts the reminder at its own eight o'clock`() {
        val now = at("2026-11-05T10:00")
        val inLisbon = ReminderSchedule.next(now, lisbon, eight)
        assertThat(inLisbon).isEqualTo(at("2026-11-05T20:00", lisbon))
        assertThat(
            Duration.between(ReminderSchedule.next(now, rome, eight), inLisbon),
        ).isEqualTo(Duration.ofHours(1))
    }

    @Test
    fun `a flight west asks the same evening, in the new zone`() {
        // 20:30 in Rome is 19:30 in Lisbon: the evening there has not come yet.
        val now = at("2026-11-05T20:30")
        assertThat(ReminderSchedule.next(now, lisbon, eight)).isEqualTo(at("2026-11-05T20:00", lisbon))
    }

    @Test
    fun `a reminder is timely from its time to the end of its day`() {
        val due = at("2026-11-05T20:00")
        assertThat(ReminderSchedule.stillTimely(due, due, rome)).isTrue()
        assertThat(ReminderSchedule.stillTimely(due, at("2026-11-05T23:59"), rome)).isTrue()
    }

    @Test
    fun `a reminder held back to the next day stays quiet`() {
        val due = at("2026-11-05T20:00")
        assertThat(ReminderSchedule.stillTimely(due, at("2026-11-06T00:05"), rome)).isFalse()
        assertThat(ReminderSchedule.stillTimely(due, at("2026-11-06T20:00"), rome)).isFalse()
    }

    @Test
    fun `a reminder delivered before its time is not one`() {
        assertThat(ReminderSchedule.stillTimely(at("2026-11-05T20:00"), at("2026-11-05T19:59"), rome)).isFalse()
    }

    @Test
    fun `a reminder stays in the shade until its day ends, whatever the day's length`() {
        assertThat(ReminderSchedule.untilEndOfDay(at("2026-11-05T20:00"), rome)).isEqualTo(Duration.ofMinutes(240))
        // 25 Oct 2026 is 25 hours long in Rome; from 01:00 there are 24 left.
        assertThat(ReminderSchedule.untilEndOfDay(at("2026-10-25T01:00"), rome)).isEqualTo(Duration.ofHours(24))
        // 29 Mar 2026 is 23 hours long; from 01:00 there are 22 left.
        assertThat(ReminderSchedule.untilEndOfDay(at("2026-03-29T01:00"), rome)).isEqualTo(Duration.ofHours(22))
    }
}
