package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import java.time.LocalDate

/**
 * A language's streak (PLANNING.md §4.8): the run of consecutive dates whose daily was won on its own
 * date, hints or not. A daily won from the archive counts in the statistics and the calendar, never
 * in the streak (VISION §11). Computed, never stored.
 */
object Streaks {
    /** The dates whose daily was won on its own date, in [language]. */
    private fun wonDates(records: List<GameRecord>, language: Language): Set<LocalDate> = records
        .filter { it.language == language && it.counted && it.onItsDate && it.state == GameState.WON }
        .mapNotNullTo(HashSet()) { it.daily?.date }

    /**
     * The streak running [today]: it ends today or yesterday, so a streak whose today is not yet
     * played, or still under way, still runs; a day missed or not played before that ends it, and
     * so does today's word missed: the run cannot go on past it.
     */
    fun current(records: List<GameRecord>, language: Language, today: LocalDate): Int {
        val missedToday = records.any {
            it.language == language && it.counted && it.daily?.date == today && it.state == GameState.LOST
        }
        if (missedToday) return 0
        val won = wonDates(records, language)
        var day = if (today in won) today else today.minusDays(1)
        var run = 0
        while (day in won) {
            run++
            day = day.minusDays(1)
        }
        return run
    }

    /** The longest streak ever, the statistics' answer. */
    fun longest(records: List<GameRecord>, language: Language): Int {
        val won = wonDates(records, language).sorted()
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in won) {
            run = if (previous != null && previous.plusDays(1) == date) run + 1 else 1
            best = maxOf(best, run)
            previous = date
        }
        return best
    }
}
