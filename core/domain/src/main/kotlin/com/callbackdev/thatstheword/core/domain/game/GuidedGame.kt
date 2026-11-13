package com.callbackdev.thatstheword.core.domain.game

import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.Word

/**
 * The guided first game (VISION §12, PLANNING.md §11 Phase 7): a fixed easy word, the same for
 * every player of a language, and the rules said one at a time, each the first time it happens,
 * never as a page of instructions.
 *
 * Each word is a Base solution, five different letters among them three common vowels or more of
 * the language's common consonants, so a first guess almost always finds something to explain.
 * `GuidedWordsTest` in `:core:words` checks each against the real files.
 */
object GuidedGame {
    /** The key forms: *heart*, *cuore*, *amigo*. */
    private val SOLUTIONS = mapOf(
        Language.ENGLISH to "heart",
        Language.ITALIAN to "cuore",
        Language.SPANISH to "amigo",
    )

    fun solution(language: Language): Word =
        checkNotNull(Alphabet.of(language).parse(SOLUTIONS.getValue(language))) { "the guided word of $language" }
}

/**
 * What the guided game says, each once (PLANNING.md §11 Phase 7). The order of the entries is the
 * order a row's new lessons are said in.
 */
enum class Lesson {
    /** Before the first guess: what the game is, and what to type. */
    FIRST_GUESS,

    /** A guess sent with letters missing. */
    FIVE_LETTERS,

    /** A guess that is not a word of the list: it costs nothing. */
    ONLY_WORDS,

    /** The first letter found in its place: the square, in ink. */
    RIGHT_PLACE,

    /** The first letter found elsewhere: the round tile, in amber. */
    ELSEWHERE,

    /** The first letter not in the word: the sunk tile. */
    ABSENT,

    /** A row with nothing new to explain: the keyboard keeps the marks. */
    KEYBOARD,

    /** In a game whose language is not the interface's, the next quiet row: the hints. */
    HINTS,
}

/**
 * When each [Lesson] is due (PLANNING.md §11 Phase 7): a pure function of the game and what was
 * said already, so a page that comes back says nothing twice and a test can walk a whole game.
 */
object GuidedLessons {
    /** The marks' lessons, in the order a row says them. */
    private val MARKS = listOf(
        Mark.RIGHT_PLACE to Lesson.RIGHT_PLACE,
        Mark.ELSEWHERE to Lesson.ELSEWHERE,
        Mark.ABSENT to Lesson.ABSENT,
    )

    /**
     * What a game taken up at [game] has taught already: the start, once a guess was made, and the
     * mark of every letter its rows show. The keyboard's and the hints' lessons are counted from
     * the rows that would have said them.
     */
    fun taught(game: Game): Set<Lesson> {
        val said = LinkedHashSet<Lesson>()
        if (game.moves.isEmpty()) return said
        said += Lesson.FIRST_GUESS
        for (i in game.guesses.indices) {
            said += afterRow(game.guesses.take(i + 1).map { it.marks }, game.hintsOffered.isNotEmpty(), said)
        }
        return said
    }

    /** What the page says before the first guess, unless it was said. */
    fun atStart(game: Game, said: Set<Lesson>): List<Lesson> =
        if (game.guesses.isEmpty() && game.state == GameState.PLAYING && Lesson.FIRST_GUESS !in said) {
            listOf(Lesson.FIRST_GUESS)
        } else {
            emptyList()
        }

    /** What a refused guess teaches, the first time it is refused for that reason. */
    fun afterRefusal(refusal: Refusal, said: Set<Lesson>): List<Lesson> {
        val lesson = when (refusal) {
            Refusal.TooShort -> Lesson.FIVE_LETTERS
            Refusal.NotAWord -> Lesson.ONLY_WORDS
            else -> null
        }
        return if (lesson == null || lesson in said) emptyList() else listOf(lesson)
    }

    /**
     * What the latest row of [game] teaches: every mark it shows for the first time, in the order of
     * [Lesson]; on a row with nothing new, the keyboard, then (in a foreign game, while a hint can
     * be taken) the hints. A game over says nothing here: its end card speaks.
     */
    fun afterGuess(game: Game, said: Set<Lesson>): List<Lesson> {
        if (game.state.finished || game.guesses.isEmpty()) return emptyList()
        return afterRow(game.guesses.map { it.marks }, game.hintsOffered.isNotEmpty(), said)
    }

    private fun afterRow(rows: List<List<Mark>>, hintsOffered: Boolean, said: Set<Lesson>): List<Lesson> {
        val last = rows.last()
        val marks = MARKS.filter { (mark, lesson) -> mark in last && lesson !in said }.map { it.second }
        if (marks.isNotEmpty()) return marks
        // The keyboard's lesson needs a row behind it: on the first row the keys have just changed.
        if (rows.size >= 2 && Lesson.KEYBOARD !in said) return listOf(Lesson.KEYBOARD)
        if (rows.size >= 2 && hintsOffered && Lesson.HINTS !in said) return listOf(Lesson.HINTS)
        return emptyList()
    }
}
