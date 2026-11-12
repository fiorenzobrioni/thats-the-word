package com.callbackdev.thatstheword.core.domain.daily

import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.Language
import java.time.LocalDate
import java.time.YearMonth

/**
 * A date of the calendar of the dailies (PLANNING.md §4.6, VISION §8): how its daily stands in a
 * language, and whether it can be played.
 *
 * @property fromArchive the day's counted game was started on a later date: caught up from the
 *   archive. It counts in the calendar and the statistics, never in the streak (VISION §11).
 * @property playable from the epoch to today, within the schedule: a date ahead of today, or
 *   before daily number 1, has no word to play.
 * @property future a date ahead of today: its word is not yet.
 */
data class ArchiveDay(
    val date: LocalDate,
    val status: DayStatus,
    val fromArchive: Boolean,
    val playable: Boolean,
    val today: Boolean,
    val future: Boolean,
)

/**
 * One month of the calendar of the dailies in a language: every date of it, with the counts its
 * sentence says ("12 found, 2 missed"), and whether a month before or after it has dailies.
 * The weeks' layout (which weekday a row starts on) is the locale's, so it is the page's.
 */
data class ArchiveMonth(
    val month: YearMonth,
    val days: List<ArchiveDay>,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
) {
    val found: Int get() = days.count { it.status == DayStatus.SOLVED }
    val missed: Int get() = days.count { it.status == DayStatus.MISSED }

    /** The days whose result was played after their date. */
    val caughtUp: Int get() = days.count {
        it.fromArchive &&
            (it.status == DayStatus.SOLVED || it.status == DayStatus.MISSED)
    }

    companion object {
        /**
         * The calendar's months: from the epoch's to today's, or to the last the schedule reaches;
         * [month] clamped into them, so a month asked for before a change of day (or of the
         * clock) still shows a month that has dailies.
         */
        fun of(
            month: YearMonth,
            records: List<GameRecord>,
            language: Language,
            calendar: DailyCalendar,
            scheduleLength: Int,
        ): ArchiveMonth {
            val today = calendar.today()
            val first = YearMonth.from(calendar.epoch)
            val lastDate = minOf(today, calendar.dateOf(scheduleLength))
            val last = maxOf(first, YearMonth.from(lastDate))
            val shown = month.coerceIn(first, last)
            val dailies = records
                .filter { it.language == language && !it.practice }
                .mapNotNull { record -> (record.mode as? GameMode.Daily)?.let { it.date to record } }
                .filter { (date, _) -> YearMonth.from(date) == shown }
                .groupBy({ it.first }, { it.second })
            val days = (1..shown.lengthOfMonth()).map { day ->
                val date = shown.atDay(day)
                val games = dailies[date].orEmpty()
                // A day's counted game is unique (the database's index): its first, finished or not.
                val counted = games.firstOrNull()
                ArchiveDay(
                    date = date,
                    status = Dailies.status(games, language, date),
                    fromArchive = counted != null && counted.startedOn != date,
                    playable = calendar.isPlayable(date, scheduleLength),
                    today = date == today,
                    future = date.isAfter(today),
                )
            }
            return ArchiveMonth(shown, days, hasPrevious = shown > first, hasNext = shown < last)
        }
    }
}
