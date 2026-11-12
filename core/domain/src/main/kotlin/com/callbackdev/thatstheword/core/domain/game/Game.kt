package com.callbackdev.thatstheword.core.domain.game

import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Guess
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.Word

/** The words a guess may be: a language's accepted set (PLANNING.md §6.2), by key form. */
fun interface WordList {
    operator fun contains(word: Word): Boolean
}

/**
 * What a player did, in order: a game is its setup and its moves, and everything else (the marks,
 * the attempts left, the state, the letter a hint revealed) follows from them. So a game is saved as
 * its moves (PLANNING.md §5) and taken up by replaying them.
 */
sealed interface Move {
    data class Guessed(val word: Word) : Move

    data class Hinted(val kind: HintKind) : Move
}

/** A hint taken; for [HintKind.LETTER], the [position] it revealed in its right place. */
data class TakenHint(val kind: HintKind, val position: Int? = null)

/** Why a guess was refused (PLANNING.md §4.3). A refused guess costs nothing. */
sealed interface Refusal {
    /** Fewer letters than the word has. */
    data object TooShort : Refusal

    /** Not in the language's accepted set. */
    data object NotAWord : Refusal

    /** Hard: the letter found at [position] (counted from 0) must stay there. */
    data class MustBeAt(val position: Int, val letter: Letter) : Refusal

    /** Hard: [letter] was found, and must be in the guess [times] times at least. */
    data class MustContain(val letter: Letter, val times: Int) : Refusal

    /** The game is over: it takes nothing more. */
    data object Finished : Refusal
}

/** The answer to a guess: the game after it, or why it was refused (and the game as it was). */
sealed interface Submission {
    data class Accepted(val game: Game, val guess: Guess) : Submission

    data class Refused(val reason: Refusal) : Submission
}

/**
 * A game (PLANNING.md §4.3 to §4.5), immutable: every move gives a new one.
 *
 * Attempts left are the attempts less the guesses and the hints, a hint costing one. Won when a
 * guess has every letter in its right place; lost when no attempt is left.
 */
class Game private constructor(val setup: GameSetup, val moves: List<Move>) {
    val guesses: List<Guess>
    val hints: List<TakenHint>
    val state: GameState

    init {
        val guesses = ArrayList<Guess>()
        val hints = ArrayList<TakenHint>()
        for (move in moves) {
            when (move) {
                is Move.Guessed -> guesses += Feedback.guess(setup.solution, move.word)

                is Move.Hinted -> hints += TakenHint(
                    move.kind,
                    if (move.kind == HintKind.LETTER) firstUnfound(setup.solution, guesses) else null,
                )
            }
        }
        this.guesses = guesses
        this.hints = hints
        state = when {
            guesses.lastOrNull()?.solved == true -> GameState.WON
            moves.size >= setup.attempts -> GameState.LOST
            else -> GameState.PLAYING
        }
    }

    val attemptsLeft: Int get() = setup.attempts - moves.size

    /** The attempts spent, guesses and hints: the score a result says ("4/6"). */
    val attemptsUsed: Int get() = moves.size

    /** Each letter's best mark so far, for the keyboard (a hint's letter is not a key's mark). */
    val keyboard: Map<Letter, Mark> get() = Feedback.keyboard(guesses)

    /**
     * The hints that can be taken now (PLANNING.md §4.5, the owner, PLANNING §15, Open 11): only in
     * a game whose language is not the interface's; each kind once, and only those this word can
     * give; none with one attempt left, which it would end; the letter only while a position is
     * still to be found.
     */
    val hintsOffered: Set<HintKind>
        get() {
            if (state.finished || attemptsLeft <= 1 || setup.interfaceLanguage == setup.language) {
                return emptySet()
            }
            val taken = hints.mapTo(HashSet()) { it.kind }
            return setup.hintKinds.filterTo(LinkedHashSet()) { kind ->
                kind !in taken && (kind != HintKind.LETTER || firstUnfound(setup.solution, guesses) != null)
            }
        }

    /**
     * A guess: refused when the game is over, the word too short, not in [words], or, with Hard,
     * when it leaves out a clue found; accepted otherwise, with its marks.
     */
    fun submit(word: Word, words: WordList): Submission {
        val refusal = when {
            state.finished -> Refusal.Finished

            word.length < setup.length -> Refusal.TooShort

            else -> {
                require(word.length == setup.length) { "a guess longer than the word" }
                when {
                    word !in words -> Refusal.NotAWord
                    setup.hard -> hardRefusal(word)
                    else -> null
                }
            }
        }
        if (refusal != null) return Submission.Refused(refusal)
        val next = Game(setup, moves + Move.Guessed(word))
        return Submission.Accepted(next, next.guesses.last())
    }

    /** Takes a hint offered (see [hintsOffered]): one attempt spent. */
    fun takeHint(kind: HintKind): Game {
        require(kind in hintsOffered) { "the hint $kind is not offered" }
        return Game(setup, moves + Move.Hinted(kind))
    }

    /** The position the letter hint revealed, if it was taken. */
    val revealed: Int? get() = hints.firstOrNull { it.kind == HintKind.LETTER }?.position

    /** Hard is chosen before the first guess, and holds for the game (PLANNING.md §4.4). */
    fun withHard(hard: Boolean): Game {
        check(guesses.isEmpty()) { "Hard is chosen before the first guess" }
        return Game(setup.copy(hard = hard), moves)
    }

    /**
     * Hard's rule (PLANNING.md §4.4): every letter found in its right place stays there (a hint's
     * letter counts as found there), and every letter found is in the guess as many times as one
     * guess marked it found. The first requirement missed is named: the positions left to right,
     * then the letters in the order they were found.
     */
    private fun hardRefusal(word: Word): Refusal? {
        val required = arrayOfNulls<Letter>(setup.length)
        val counts = LinkedHashMap<Letter, Int>()
        for (guess in guesses) {
            val found = HashMap<Letter, Int>()
            for (i in 0 until guess.word.length) {
                val mark = guess.marks[i]
                if (mark == Mark.RIGHT_PLACE) required[i] = guess.word[i]
                if (mark.found) found.merge(guess.word[i], 1, Int::plus)
            }
            for ((letter, n) in found) counts[letter] = maxOf(counts[letter] ?: 0, n)
        }
        revealed?.let { position ->
            val letter = setup.solution[position]
            required[position] = letter
            counts[letter] = maxOf(counts[letter] ?: 0, 1)
        }
        for (i in required.indices) {
            val letter = required[i] ?: continue
            if (word[i] != letter) return Refusal.MustBeAt(i, letter)
        }
        for ((letter, times) in counts) {
            if (word.letters.count { it == letter } < times) return Refusal.MustContain(letter, times)
        }
        return null
    }

    companion object {
        fun start(setup: GameSetup): Game = Game(setup, emptyList())

        /**
         * A game taken up from its saved moves (PLANNING.md §4.3). The moves are replayed, so the
         * marks and the hint's letter are computed again, never read back.
         */
        fun restore(setup: GameSetup, moves: List<Move>): Game {
            require(moves.size <= setup.attempts) { "more moves than attempts" }
            return Game(setup, moves)
        }

        /** The leftmost position no guess has found in its right place, or null. */
        private fun firstUnfound(solution: Word, guesses: List<Guess>): Int? =
            (0 until solution.length).firstOrNull { i -> guesses.none { it.marks[i] == Mark.RIGHT_PLACE } }
    }
}
