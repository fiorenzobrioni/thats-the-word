package com.callbackdev.thatstheword.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A game (PLANNING.md §5). The marks are never stored: they follow from the solution and the
 * moves. [status] is kept beside the moves, in the same transaction, so a table's query can count
 * the games won without replaying them.
 *
 * [practice] is 0 for a daily's counted game and 1, 2… for the replays of a day already finished
 * (the owner, PLANNING §15, Open 9), so the unique index on `(language, daily_date, practice)` is a
 * day's one counted game. Free play and the guided game have no `daily_date`, and SQLite's unique
 * index lets NULLs repeat. [startedOn] is the local date the game was started on, kept as it was
 * read: an instant read again in another time zone would give another date.
 */
@Entity(
    tableName = "games",
    indices = [Index(value = ["language", "daily_date", "practice"], unique = true)],
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The language's tag: `it`, `en`, `es`. */
    val language: String,
    /** `daily`, `free` or `guided`. */
    val mode: String,
    /** A daily's date, as an epoch day. */
    @ColumnInfo(name = "daily_date") val dailyDate: Long?,
    /** Free play's level, its code. */
    val level: Int?,
    /** The key form, in the keys' lower case (`citta`). */
    val solution: String,
    val length: Int,
    val attempts: Int,
    val hard: Boolean,
    @ColumnInfo(name = "ui_language") val uiLanguage: String,
    /** `playing`, `won`, `lost`. */
    val status: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "started_on") val startedOn: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    val practice: Int,
)

/** A guess of a game, at its [position] among the game's moves (guesses and hints share one sequence). */
@Entity(
    tableName = "guesses",
    primaryKeys = ["game_id", "position"],
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["game_id"], onDelete = ForeignKey.CASCADE)],
)
data class GuessEntity(
    @ColumnInfo(name = "game_id") val gameId: Long,
    val position: Int,
    /** The key form, in the keys' lower case. */
    val word: String,
)

/** A hint taken, at its [position] among the game's moves; the letter it revealed follows from the guesses before it. */
@Entity(
    tableName = "hints",
    primaryKeys = ["game_id", "position"],
    foreignKeys = [ForeignKey(GameEntity::class, ["id"], ["game_id"], onDelete = ForeignKey.CASCADE)],
)
data class HintEntity(
    @ColumnInfo(name = "game_id") val gameId: Long,
    val position: Int,
    /** `translation`, `definition`, `letter`. */
    val kind: String,
)

/**
 * A word met, one row a word and language (PLANNING.md §5): the solutions by themselves, a guessed
 * word when the player adds it (the owner, PLANNING §15, Open 8). Its meanings are read from the
 * words' files, never copied. Written from Phase 5; in the first schema so that no migration is
 * needed for it.
 */
@Entity(tableName = "notebook", primaryKeys = ["language", "word"])
data class NotebookEntity(
    val language: String,
    val word: String,
    @ColumnInfo(name = "first_met") val firstMet: Long,
    @ColumnInfo(name = "last_met") val lastMet: Long,
    @ColumnInfo(name = "times_met") val timesMet: Int,
    val solved: Int,
    val missed: Int,
    @ColumnInfo(name = "with_hints") val withHints: Int,
    @ColumnInfo(name = "saved_by_hand") val savedByHand: Boolean,
)

/** A milestone unlocked, once, with its date (PLANNING.md §4.8). Written from Phase 5. */
@Entity(tableName = "milestones")
data class MilestoneEntity(@PrimaryKey val id: String, @ColumnInfo(name = "unlocked_at") val unlockedAt: Long)

/** A game with the number of its moves, for the progress: what a row of the table is worth without its moves. */
data class GameSummary(
    @Embedded val game: GameEntity,
    @ColumnInfo(name = "guess_count") val guessCount: Int,
    @ColumnInfo(name = "hint_count") val hintCount: Int,
)
