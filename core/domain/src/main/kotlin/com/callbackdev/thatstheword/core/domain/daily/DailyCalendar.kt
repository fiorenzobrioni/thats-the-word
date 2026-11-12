package com.callbackdev.thatstheword.core.domain.daily

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The daily word's dates and numbers (PLANNING.md §4.6), from the local date alone: the same word
 * for every player of a language on a date, with no server (VISION §8).
 *
 * Every date is a local date, read from an injected [clock] and its zone, so a test sets the hour,
 * the zone, the change of the clocks. The daily of a date `d` is number `d − epoch + 1`, and changes
 * at local midnight. A daily belongs to the date it was started on (the owner, PLANNING §15, Open 9):
 * the caller keeps the date of [today] at the start, and the game never moves.
 */
class DailyCalendar(private val clock: Clock, val epoch: LocalDate = EPOCH) {
    fun today(): LocalDate = LocalDate.now(clock)

    /** The local date of an [instant], in the clock's zone. */
    fun dateOf(instant: Instant): LocalDate = instant.atZone(clock.zone).toLocalDate()

    /** The daily's number of a [date], 1 on the epoch; 0 or less before it. */
    fun number(date: LocalDate): Int = (ChronoUnit.DAYS.between(epoch, date) + 1).toInt()

    fun dateOf(number: Int): LocalDate = epoch.plusDays(number - 1L)

    /**
     * Whether the daily of [date] can be played: from the epoch to today, within the schedule's
     * [scheduleLength]. A date ahead of today is not, by the phone's clock, which is not checked
     * against anything and needs not be: nobody is competing.
     */
    fun isPlayable(date: LocalDate, scheduleLength: Int): Boolean =
        !date.isBefore(epoch) && !date.isAfter(today()) && number(date) <= scheduleLength

    /**
     * How long until the next local date begins: the start page's ticker waits this long, while it
     * is on screen, to show the new day's word (PLANNING.md §9). A day of a change of the clocks is
     * 23 or 25 hours long, and a zone whose clocks change at midnight starts its day at 01:00: the
     * zone's own start of the next day, never `now + 24h`.
     */
    fun untilNextDay(): Duration {
        val now = Instant.now(clock)
        val next = today().plusDays(1).atStartOfDay(clock.zone).toInstant()
        return Duration.between(now, next)
    }

    /** Every playable date, from the epoch to today: the archive (PLANNING.md §4.6). */
    fun archive(scheduleLength: Int): List<LocalDate> {
        val last = minOf(today(), dateOf(scheduleLength))
        if (last.isBefore(epoch)) return emptyList()
        return (0..ChronoUnit.DAYS.between(epoch, last)).map { epoch.plusDays(it) }
    }

    companion object {
        /**
         * Daily number 1 (PLANNING.md §4.6), fixed by the owner on 12 Nov 2026 (PLANNING §15, Open
         * 10) and never moved again: before the release, so the archive already holds days to play
         * whenever 1.0 comes. `EPOCH` in `tools/words/`'s config is the same date; the schedules'
         * entries are by number, so the date chose no word.
         */
        val EPOCH: LocalDate = LocalDate.of(2026, 11, 1)
    }
}
