package com.callbackdev.thatstheword.core.domain.daily

import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import java.time.LocalDate

/** How a date's daily stands in a language, for the calendar (PLANNING.md §4.6). */
enum class DayStatus {
    NOT_PLAYED,
    IN_PROGRESS,
    SOLVED,
    MISSED,
}

/**
 * A day's result is its first finished game (the owner, PLANNING §15, Open 9): a day already
 * finished can be played again as practice, which is not counted, not in the calendar, not in the
 * notebook's marks.
 */
object Dailies {
    /** The games of one language's daily of [date], in the order they were started. */
    private fun of(records: List<GameRecord>, language: Language, date: LocalDate): List<GameRecord> =
        records.filter { it.language == language && (it.mode as? GameMode.Daily)?.date == date }

    /** The game that is the day's result: the first finished one that is not practice. */
    fun result(records: List<GameRecord>, language: Language, date: LocalDate): GameRecord? =
        of(records, language, date).firstOrNull { it.counted }

    fun status(records: List<GameRecord>, language: Language, date: LocalDate): DayStatus {
        val games = of(records, language, date)
        val result = games.firstOrNull { it.counted }
        return when {
            result?.state == GameState.WON -> DayStatus.SOLVED
            result?.state == GameState.LOST -> DayStatus.MISSED
            games.any { !it.practice && it.state == GameState.PLAYING } -> DayStatus.IN_PROGRESS
            else -> DayStatus.NOT_PLAYED
        }
    }

    /** Whether a new game of that daily would be practice: the day already has its result. */
    fun nextIsPractice(records: List<GameRecord>, language: Language, date: LocalDate): Boolean =
        result(records, language, date) != null
}
