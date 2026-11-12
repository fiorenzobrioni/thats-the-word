package com.callbackdev.thatstheword.core.domain.share

import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Mark

/**
 * A result as text for any messaging app (PLANNING.md §7.6, VISION §11), without the word: a line
 * that says the game ("That's The Word · #123 · Italian · 4/6"), a line for Hard and the hints when
 * there are any, then a row of symbols a guess, which keep the marks' shapes (square for the right
 * place, round for elsewhere), never the genre's coloured squares. A hint's row is said, not drawn.
 *
 * The words are the interface's, from its resources: the domain only puts them in order.
 */
object ShareText {
    /** The interface's words for a result. */
    data class Labels(
        val appName: String,
        val languageName: String,
        /** Free play's or the guided game's name, where a daily has its number. */
        val modeName: String,
        val hard: String,
        /** "1 hint", "2 hints", in the interface's plural. */
        val hints: (Int) -> String,
    )

    const val SEPARATOR = " · "
    const val RIGHT_PLACE = "🟪" // 🟪, a square: here
    const val ELSEWHERE = "🟠" // 🟠, round: somewhere in the word
    const val ABSENT = "▫️" // ▫️

    /** "X" for a game lost, as every result in the genre says it. */
    const val LOST = "X"

    fun of(game: Game, labels: Labels): String {
        require(game.state.finished) { "a game is shared once it is over" }
        val mode = when (val m = game.setup.mode) {
            is GameMode.Daily -> "#${m.number}"
            else -> labels.modeName
        }
        val score = if (game.state == GameState.WON) game.attemptsUsed.toString() else LOST
        val title = listOf(labels.appName, mode, labels.languageName, "$score/${game.setup.attempts}")
        val extras = buildList {
            if (game.setup.hard) add(labels.hard)
            if (game.hints.isNotEmpty()) add(labels.hints(game.hints.size))
        }
        val rows = game.guesses.map { guess -> guess.marks.joinToString("") { symbol(it) } }
        return buildList {
            add(title.joinToString(SEPARATOR))
            if (extras.isNotEmpty()) add(extras.joinToString(SEPARATOR))
            add("")
            addAll(rows)
        }.joinToString("\n")
    }

    fun symbol(mark: Mark): String = when (mark) {
        Mark.RIGHT_PLACE -> RIGHT_PLACE
        Mark.ELSEWHERE -> ELSEWHERE
        Mark.ABSENT -> ABSENT
    }
}
