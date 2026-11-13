package com.callbackdev.thatstheword.core.domain.reminder

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * When the daily reminder comes (PLANNING.md §8): always at the player's local time, worked out
 * again after every clock or zone change, since an alarm is an instant and 20:00 in Rome is not
 * 20:00 in Lisbon.
 *
 * A time that does not exist on a day (the hour the clocks skip in spring) moves forward by the
 * length of the gap, as `java.time` resolves it; a time that happens twice (the hour repeated in
 * autumn) is the first of the two.
 */
object ReminderSchedule {
    /** Eight in the evening: after most days' work, with the evening still ahead for a word. */
    val DEFAULT_TIME: LocalTime = LocalTime.of(20, 0)

    /** The next reminder: [time] on the first day whose [time] is still after [now], in [zone]. */
    fun next(now: Instant, zone: ZoneId, time: LocalTime): Instant {
        var day = LocalDate.ofInstant(now, zone)
        while (true) {
            val at = ZonedDateTime.of(day, time, zone).toInstant()
            if (at.isAfter(now)) return at
            day = day.plusDays(1)
        }
    }

    /**
     * Whether a reminder due at [dueAt] still has something to say at [now]: not early, and still
     * the day it was for. Doze, a phone switched off or a flight can hold an alarm back; "today's
     * word is waiting" read the next morning would be about a word that is no longer today's.
     */
    fun stillTimely(dueAt: Instant, now: Instant, zone: ZoneId): Boolean =
        !now.isBefore(dueAt) && LocalDate.ofInstant(dueAt, zone) == LocalDate.ofInstant(now, zone)

    /**
     * How long until the local day of [now] ends: how long the reminder stays in the shade, since
     * at midnight another word is today's. The day may be 23 or 25 hours long.
     */
    fun untilEndOfDay(now: Instant, zone: ZoneId): Duration =
        Duration.between(now, LocalDate.ofInstant(now, zone).plusDays(1).atStartOfDay(zone).toInstant())
}
