package com.callbackdev.thatstheword.core.model

import java.time.LocalDate

/**
 * A word met, in the notebook (VISION §9, PLANNING.md §5). The notebook gathers the solutions by
 * themselves, and a guessed word only when the player adds it from the end card (the owner,
 * PLANNING §15, Open 8): [savedByHand]. Its meanings are read from the words' files, never copied.
 */
data class NotebookEntry(
    val language: Language,
    val word: Word,
    val firstMet: LocalDate,
    val lastMet: LocalDate,
    val timesMet: Int,
    val solved: Int,
    val missed: Int,
    val withHints: Int,
    val savedByHand: Boolean,
)

/**
 * The milestones' first list (VISION §11, PLANNING.md §4.8): recognition, never a key. Each is
 * unlocked once, with its date, and never taken back; its sentence is the interface's.
 */
enum class Milestone {
    /** Seven dailies of one language won in a row. */
    SEVEN_DAILIES_IN_A_ROW,

    /** A first game won in a language that is not the interface's. */
    FIRST_FOREIGN_WIN,

    /** A hundred words in the notebook. */
    HUNDRED_WORDS,

    /** One date's daily won in all three languages. */
    ONE_DAY_THREE_LANGUAGES,
}
