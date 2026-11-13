package com.callbackdev.thatstheword.feature.game

import androidx.compose.runtime.Immutable
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.Lesson
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.domain.game.TakenHint
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.domain.words.WordMeaning
import com.callbackdev.thatstheword.core.domain.words.meaning
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.NotebookEntry
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

/**
 * Where a hint stands (PLANNING.md §4.5): to take, taken, or why it cannot be: the word has none
 * in the interface language, one attempt is left (it would end the game), or every place of the
 * word is found already (the letter).
 */
enum class HintStatus {
    OFFERED,
    TAKEN,
    NOT_FOR_THIS_WORD,
    LAST_ATTEMPT,
    ALL_FOUND,
}

/** A hint in the panel that offers them. */
@Immutable
data class HintChoiceUi(val kind: HintKind, val status: HintStatus)

/**
 * A hint taken, as the grid draws it in the row it spent: the translation's or the definition's
 * [text] in the interface language; the letter's capital and its [position].
 */
@Immutable
data class HintUi(val kind: HintKind, val text: String? = null, val letter: Char? = null, val position: Int? = null)

/**
 * A word as the end card explains it (VISION §9): its written forms with their accents, the lemma
 * it is a form of, its translations and its meaning in the interface language.
 */
@Immutable
data class MeaningUi(val written: String, val lemma: String?, val translations: String?, val meaning: String?) {
    val known: Boolean get() = translations != null || meaning != null

    companion object {
        fun of(meaning: WordMeaning): MeaningUi = MeaningUi(
            written = meaning.written.joinToString(", "),
            lemma = meaning.lemma,
            translations = meaning.translations.takeIf { it.isNotEmpty() }?.joinToString(", "),
            meaning = meaning.meaning,
        )
    }
}

/**
 * A word tried, on the end card: what it means, and whether it is in the notebook. [removable] a
 * word added by hand may be taken back; one a game hid stays (the owner, PLANNING §15, Open 8).
 */
@Immutable
data class TriedWordUi(val index: Int, val meaning: MeaningUi, val inNotebook: Boolean, val removable: Boolean)

/**
 * What the end card offers next (PLANNING.md §4.6, §4.7): the day again as practice, another free
 * word, or, after the guided first game, the game itself (PLANNING.md §11 Phase 7).
 */
enum class NextGame {
    PRACTICE,
    ANOTHER_WORD,
    START_PLAYING,
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
    /** Whether this game has hints at all: its language is not the interface's (VISION §5). */
    val hintsAvailable: Boolean = false,
    val hintChoices: List<HintChoiceUi> = emptyList(),
    /** The hints taken, in order: the first spent the grid's last row, the next the row above. */
    val takenHints: List<HintUi> = emptyList(),
    /** The solution explained, for the end card. */
    val solutionMeaning: MeaningUi? = null,
    /** The words tried that are not the solution, each once, for the end card. */
    val tried: List<TriedWordUi> = emptyList(),
    /** Whether the solution is in the notebook: written down when a counted game ends. */
    val solutionKept: Boolean = false,
    /** The milestones this game reached, said once on its end card (PLANNING.md §7.4). */
    val milestones: List<Milestone> = emptyList(),
    /** Whether the meanings are the word's own language's definitions: no translation to give. */
    val sameLanguage: Boolean = true,
    /** What the guided first game says now, each lesson once (PLANNING.md §11 Phase 7); empty otherwise. */
    val lessons: List<Lesson> = emptyList(),
) {
    val guided: Boolean get() = mode == GameMode.Guided

    val next: NextGame?
        get() = when (mode) {
            is GameMode.Daily -> NextGame.PRACTICE
            is GameMode.Free -> NextGame.ANOTHER_WORD
            GameMode.Guided -> NextGame.START_PLAYING
        }

    /** A day's result is shared, a replay's is not: it is no day's result; nor the guided game's, a lesson. */
    val shareable: Boolean get() = !practice && !guided
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

        /**
         * The page of [game]. [lexicon] gives the written forms and the meanings; the meanings are
         * read in [interfaceLanguage], the language the interface speaks now (the hints' in the
         * one the game started in, which decided them). [notebook] is the language's words in the
         * notebook, [milestones] those the game reached while the page was open.
         */
        fun of(
            game: Game,
            typed: List<Letter>,
            refusal: RefusalUi?,
            lexicon: Lexicon,
            practice: Boolean = false,
            interfaceLanguage: Language = game.setup.interfaceLanguage,
            notebook: Map<Word, NotebookEntry> = emptyMap(),
            milestones: List<Milestone> = emptyList(),
            lessons: List<Lesson> = emptyList(),
        ): GameUiState {
            val language = game.setup.language
            val alphabet = Alphabet.of(language)
            val locale = Locale.forLanguageTag(language.tag)
            val forms = lexicon.forms
            fun capital(letter: Letter) = alphabet.keyChar(letter).toString().uppercase(locale).single()
            val finished = game.state.finished
            val solution = game.setup.solution
            val tried = game.guesses.map { it.word }.distinct().filter { it != solution }
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
                hintsAvailable = game.setup.language != game.setup.interfaceLanguage,
                hintChoices = hintChoicesOf(game),
                takenHints = game.hints.map { hint -> hintOf(hint, game, lexicon, ::capital) },
                // The meanings are read once the game is over: the end card is their only page.
                solutionMeaning = if (finished) MeaningUi.of(lexicon.meaning(solution, interfaceLanguage)) else null,
                tried = if (finished) {
                    tried.mapIndexed { i, word ->
                        val entry = notebook[word]
                        TriedWordUi(
                            index = i,
                            meaning = MeaningUi.of(lexicon.meaning(word, interfaceLanguage)),
                            inNotebook = entry != null,
                            removable = entry?.savedByHand == true && entry.solved + entry.missed == 0,
                        )
                    }
                } else {
                    emptyList()
                },
                solutionKept = notebook.containsKey(solution),
                milestones = milestones,
                sameLanguage = language == interfaceLanguage,
                lessons = if (finished) emptyList() else lessons,
            )
        }

        /** The three hints in their order, each with where it stands (PLANNING.md §4.5). */
        private fun hintChoicesOf(game: Game): List<HintChoiceUi> {
            val offered = game.hintsOffered
            val taken = game.hints.mapTo(HashSet()) { it.kind }
            return HintKind.entries.map { kind ->
                val status = when {
                    kind in taken -> HintStatus.TAKEN
                    kind in offered -> HintStatus.OFFERED
                    kind !in game.setup.hintKinds -> HintStatus.NOT_FOR_THIS_WORD
                    game.attemptsLeft <= 1 -> HintStatus.LAST_ATTEMPT
                    else -> HintStatus.ALL_FOUND
                }
                HintChoiceUi(kind, status)
            }
        }

        /**
         * A hint taken, with what it shows: in the language the game started in, the one that
         * decided which hints it has. The definition is the meaning alone, never the translation
         * it was read through, which would give away the translation's hint.
         */
        private fun hintOf(hint: TakenHint, game: Game, lexicon: Lexicon, capital: (Letter) -> Char): HintUi {
            val gloss by lazy { lexicon.gloss(game.setup.solution, game.setup.interfaceLanguage) }
            return when (hint.kind) {
                HintKind.TRANSLATION -> HintUi(hint.kind, text = gloss?.translations?.joinToString(", "))

                HintKind.DEFINITION -> HintUi(hint.kind, text = gloss?.meaning)

                HintKind.LETTER -> HintUi(
                    hint.kind,
                    letter = hint.position?.let { capital(game.setup.solution[it]) },
                    position = hint.position,
                )
            }
        }
    }
}
