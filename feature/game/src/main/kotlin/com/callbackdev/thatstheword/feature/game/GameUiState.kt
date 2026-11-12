package com.callbackdev.thatstheword.feature.game

import androidx.compose.runtime.Immutable
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Forms
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.Word
import java.util.Locale

/** A guess as the grid draws it: its capitals and their marks. */
@Immutable
data class RowUi(val letters: List<Char>, val marks: List<Mark>)

/** A key of the game's keyboard: its capital, and the letter it types. */
@Immutable
data class KeyUi(val char: Char, val letter: Letter)

/** Why a guess was refused, with the capital it names, for the line above the keyboard. */
@Immutable
data class RefusalUi(val reason: Refusal, val letter: Char?, val id: Int)

/** What the end card offers next (PLANNING.md §4.6, §4.7): the day again as practice, or another free word. */
enum class NextGame {
    PRACTICE,
    ANOTHER_WORD,
}

/**
 * The game page, all of it, from one game (PLANNING.md §7.3): drawn from this alone, so a test
 * draws any moment of a game without a database.
 *
 * @property practice a replay of a day already finished: not counted, its result not shared
 *   (PLANNING.md §4.6).
 * @property next what the end card offers once the game is over.
 * @property finishedHere whether the game ended while the page was open, so its end is played
 *   (the reveal, the wave, the line, the card rising); a game taken up already finished is drawn
 *   at rest.
 */
@Immutable
data class GameUiState(
    val language: Language,
    val mode: GameMode,
    val length: Int,
    val attempts: Int,
    val rows: List<RowUi>,
    val spentOnHints: Int,
    val typed: List<Char>,
    val keyboard: List<List<KeyUi>>,
    val keyMarks: Map<Char, Mark>,
    val state: GameState,
    val refusal: RefusalUi?,
    val hard: Boolean,
    val hints: Int,
    val attemptsUsed: Int,
    /** The solution's capitals, and its written form (*città*), shown once the game is over. */
    val solution: String,
    val solutionWritten: String,
    /** Each guess's written forms ("pero, però"), for the end card. */
    val guessesWritten: List<String>,
    val practice: Boolean = false,
) {
    val next: NextGame?
        get() = when (mode) {
            is GameMode.Daily -> NextGame.PRACTICE
            is GameMode.Free -> NextGame.ANOTHER_WORD
            GameMode.Guided -> null
        }

    /** A day's result is shared, a replay's is not: it is no day's result. */
    val shareable: Boolean get() = !practice
    val finished: Boolean get() = state.finished

    companion object {
        /** The keyboard's rows (PLANNING.md §7.3): QWERTY in every language, Spanish's Ñ beside L. */
        private val ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

        fun keyboardOf(language: Language): List<List<KeyUi>> {
            val alphabet = Alphabet.of(language)
            val locale = Locale.forLanguageTag(language.tag)
            return ROWS.mapIndexed { i, row ->
                val chars = if (i == 1 && language == Language.SPANISH) row + "ñ" else row
                chars.map { c -> KeyUi(c.toString().uppercase(locale).single(), checkNotNull(alphabet.letterOf(c))) }
            }
        }

        fun capitals(word: Word, language: Language): List<Char> {
            val alphabet = Alphabet.of(language)
            val locale = Locale.forLanguageTag(language.tag)
            return word.letters.map { alphabet.keyChar(it).toString().uppercase(locale).single() }
        }

        fun of(
            game: Game,
            typed: List<Letter>,
            refusal: RefusalUi?,
            forms: Forms,
            practice: Boolean = false,
        ): GameUiState {
            val language = game.setup.language
            val alphabet = Alphabet.of(language)
            val locale = Locale.forLanguageTag(language.tag)
            fun capital(letter: Letter) = alphabet.keyChar(letter).toString().uppercase(locale).single()
            return GameUiState(
                language = language,
                mode = game.setup.mode,
                length = game.setup.length,
                attempts = game.setup.attempts,
                rows = game.guesses.map { RowUi(capitals(it.word, language), it.marks) },
                spentOnHints = game.hints.size,
                typed = typed.map(::capital),
                keyboard = keyboardOf(language),
                keyMarks = game.keyboard.mapKeys { capital(it.key) },
                state = game.state,
                refusal = refusal,
                hard = game.setup.hard,
                hints = game.hints.size,
                attemptsUsed = game.attemptsUsed,
                solution = capitals(game.setup.solution, language).joinToString(""),
                solutionWritten = forms.solutionDisplay(game.setup.solution),
                guessesWritten = game.guesses.map { guess -> forms.of(guess.word).joinToString(", ") { it.display } },
                practice = practice,
            )
        }
    }
}
