package com.callbackdev.thatstheword.feature.game.share

import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Mark
import java.time.LocalDate

/**
 * A finished game as a shared result tells it (PLANNING.md §7.6): its language, mode and date,
 * each guess's marks, the hints and Hard, the score. Never the word, never a letter.
 *
 * @property date a daily's own date; a free game's, the date it was started on.
 */
data class ResultFacts(
    val language: Language,
    val mode: GameMode,
    val date: LocalDate,
    val rows: List<List<Mark>>,
    val hints: Int,
    val hard: Boolean,
    val length: Int,
    val attempts: Int,
    val attemptsUsed: Int,
    val won: Boolean,
) {
    companion object {
        fun of(game: Game, startedOn: LocalDate): ResultFacts {
            require(game.state.finished) { "a game is shared once it is over" }
            return ResultFacts(
                language = game.setup.language,
                mode = game.setup.mode,
                date = (game.setup.mode as? GameMode.Daily)?.date ?: startedOn,
                rows = game.guesses.map { it.marks },
                hints = game.hints.size,
                hard = game.setup.hard,
                length = game.setup.length,
                attempts = game.setup.attempts,
                attemptsUsed = game.attemptsUsed,
                won = game.state == GameState.WON,
            )
        }
    }
}
