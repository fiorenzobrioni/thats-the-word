package com.callbackdev.thatstheword.core.domain.daily

import java.time.Clock
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
class DailyCalendar(private val clock: Clock, val epoch: LocalDate = PROVISIONAL_EPOCH) {
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

    /** Every playable date, from the epoch to today: the archive (PLANNING.md §4.6). */
    fun archive(scheduleLength: Int): List<LocalDate> {
        val last = minOf(today(), dateOf(scheduleLength))
        if (last.isBefore(epoch)) return emptyList()
        return (0..ChronoUnit.DAYS.between(epoch, last)).map { epoch.plusDays(it) }
    }

    companion object {
        /**
         * Daily number 1, provisional until the 1.0 release fixes it (PLANNING §15, Open 10): the
         * date `tools/words/` wrote the schedules from (`PROVISIONAL_EPOCH` in its config).
         */
        val PROVISIONAL_EPOCH: LocalDate = LocalDate.of(2026, 11, 12)
    }
}
