package com.callbackdev.thatstheword.core.domain.backup

import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.Move
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.NotebookEntry
import com.callbackdev.thatstheword.core.model.UserSettings
import com.callbackdev.thatstheword.core.model.Word
import java.time.Instant
import java.time.LocalDate

/**
 * A game as a backup keeps it (PLANNING.md §5): its setup, its moves in the order they were made,
 * and when it was started and finished. The marks are not kept, as in the database: they follow
 * from the solution and the moves.
 *
 * @property practice 0 for a game that counts, 1, 2… for a day's replays as practice (PLANNING.md §4.6).
 */
data class BackupGame(
    val language: Language,
    val interfaceLanguage: Language,
    val mode: GameMode,
    val solution: Word,
    val attempts: Int,
    val hard: Boolean,
    val moves: List<Move>,
    val startedAt: Instant,
    val startedOn: LocalDate,
    val finishedAt: Instant?,
    val practice: Int,
) {
    /** Where the game stands, by replaying its moves: never read from the file as it says it. */
    val state: GameState get() = replay(moves).state

    val counted: Boolean get() = practice == 0 && state.finished && mode != GameMode.Guided

    internal fun replay(moves: List<Move>): Game = Game.restore(
        GameSetup(
            language = language,
            mode = mode,
            solution = solution,
            attempts = attempts,
            hard = hard,
            interfaceLanguage = interfaceLanguage,
        ),
        moves,
    )
}

/**
 * Everything a new phone could not rebuild (PLANNING.md §5, VISION §11): the games with their
 * moves, the notebook, the milestones with the instant each was unlocked, and the player's
 * choices. The words are not in it: they ship with the app.
 *
 * [settings]' first run reads as done, and its tips as not seen: those are the phone's, not the
 * player's, and a restore keeps the phone's own.
 */
data class Backup(
    val exportedAt: Instant,
    val zone: String,
    val appVersion: String,
    val settings: UserSettings,
    val games: List<BackupGame>,
    val notebook: List<NotebookEntry>,
    val milestones: Map<Milestone, Instant>,
) {
    /** The games that count, finished and not practice: what a restore's question says it brings. */
    val countedGames: Int get() = games.count { it.counted }
}
