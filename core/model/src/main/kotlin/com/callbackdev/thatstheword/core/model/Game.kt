package com.callbackdev.thatstheword.core.model

import java.time.LocalDate

/** What kind of game it is (PLANNING.md §4.3); 1.1 and 1.2 add theirs. */
sealed interface GameMode {
    /**
     * The daily of a local [date], its [number] counted from the epoch (PLANNING.md §4.6). A daily
     * belongs to the date it was started on (the owner, PLANNING §15, Open 9).
     */
    data class Daily(val date: LocalDate, val number: Int) : GameMode

    /** Free play, at a [level] of the game language (PLANNING.md §4.7). */
    data class Free(val level: Level) : GameMode

    /** The guided first game, a fixed easy word (VISION §12). */
    data object Guided : GameMode
}

/** The three hints, each once a game, each at the cost of one attempt (PLANNING.md §4.5). */
enum class HintKind {
    /** The solution's translation in the interface language. */
    TRANSLATION,

    /** A definition in the interface language. */
    DEFINITION,

    /** The leftmost position not yet found, revealed in its right place. */
    LETTER,
}

/**
 * Everything a game is decided by before its first guess (PLANNING.md §4.3).
 *
 * [hintKinds] are the hints this solution can give: a word with no translation or no definition
 * in the interface language offers no such hint. A game in the interface's own language offers none
 * (VISION §5).
 */
data class GameSetup(
    val language: Language,
    val mode: GameMode,
    val solution: Word,
    val attempts: Int = ATTEMPTS,
    val hard: Boolean = false,
    val interfaceLanguage: Language,
    val hintKinds: Set<HintKind> = emptySet(),
) {
    val length: Int get() = solution.length

    init {
        require(attempts >= 1) { "a game needs an attempt" }
    }

    companion object {
        /** 1.0's game: five letters, six attempts (VISION §8). */
        const val LENGTH = 5
        const val ATTEMPTS = 6
    }
}

/** Where a game stands (PLANNING.md §4.3). A finished game takes nothing more. */
enum class GameState {
    PLAYING,
    WON,
    LOST,
    ;

    val finished: Boolean get() = this != PLAYING
}
