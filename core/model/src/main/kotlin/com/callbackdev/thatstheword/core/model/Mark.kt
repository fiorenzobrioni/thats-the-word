package com.callbackdev.thatstheword.core.model

/**
 * A letter's verdict (PLANNING.md §4.2), always drawn by colour and shape together (VISION §6).
 * Computed from the solution and the guesses, never stored.
 *
 * In order of strength, as the keyboard keeps a letter's best: [RIGHT_PLACE] over [ELSEWHERE] over
 * [ABSENT].
 */
enum class Mark {
    ABSENT,
    ELSEWHERE,
    RIGHT_PLACE,
    ;

    /** Whether the letter is in the solution at all. */
    val found: Boolean get() = this != ABSENT
}

/** A word tried, with its marks. */
data class Guess(val word: Word, val marks: List<Mark>) {
    init {
        require(marks.size == word.length) { "a mark a letter" }
    }

    val solved: Boolean get() = marks.all { it == Mark.RIGHT_PLACE }
}
