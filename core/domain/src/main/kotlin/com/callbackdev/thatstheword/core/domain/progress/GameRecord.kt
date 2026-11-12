package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Word
import java.time.LocalDate

/**
 * A game as the progress sees it (PLANNING.md §4.8): one row of the games table, whatever stores
 * it. Statistics, streaks and milestones are computed from these, never kept as counters.
 *
 * [startedOn] is the local date it was started on: a daily started on its own date is the day's,
 * one started later is from the archive. [practice] is a replay of a day already finished (the
 * owner, PLANNING §15, Open 9): not counted, not in the calendar.
 */
data class GameRecord(
    val language: Language,
    val interfaceLanguage: Language,
    val mode: GameMode,
    val solution: Word,
    val state: GameState,
    val attemptsUsed: Int,
    val attempts: Int,
    val hints: Int,
    val hard: Boolean,
    val startedOn: LocalDate,
    val practice: Boolean = false,
) {
    val daily: GameMode.Daily? get() = mode as? GameMode.Daily

    /** A daily played on its own date, not from the archive: what a streak is made of. */
    val onItsDate: Boolean get() = daily?.date == startedOn

    val counted: Boolean get() = !practice && state.finished

    companion object {
        fun of(game: Game, startedOn: LocalDate, practice: Boolean = false): GameRecord = GameRecord(
            language = game.setup.language,
            interfaceLanguage = game.setup.interfaceLanguage,
            mode = game.setup.mode,
            solution = game.setup.solution,
            state = game.state,
            attemptsUsed = game.attemptsUsed,
            attempts = game.setup.attempts,
            hints = game.hints.size,
            hard = game.setup.hard,
            startedOn = startedOn,
            practice = practice,
        )
    }
}
