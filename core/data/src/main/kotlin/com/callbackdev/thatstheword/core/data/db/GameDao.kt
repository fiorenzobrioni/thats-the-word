package com.callbackdev.thatstheword.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Insert
    suspend fun insertGuess(guess: GuessEntity)

    @Insert
    suspend fun insertHint(hint: HintEntity)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun game(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeGame(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM guesses WHERE game_id = :gameId ORDER BY position")
    suspend fun guesses(gameId: Long): List<GuessEntity>

    @Query("SELECT * FROM hints WHERE game_id = :gameId ORDER BY position")
    suspend fun hints(gameId: Long): List<HintEntity>

    /** A day's counted game in a language, if it was started. */
    @Query("SELECT * FROM games WHERE language = :language AND daily_date = :date AND practice = 0")
    suspend fun countedDaily(language: String, date: Long): GameEntity?

    /** A day's replays as practice, in the order they were started (PLANNING.md §4.6). */
    @Query("SELECT * FROM games WHERE language = :language AND daily_date = :date AND practice > 0 ORDER BY practice")
    suspend fun practiceDailies(language: String, date: Long): List<GameEntity>

    /** A language's guided first games, oldest first (PLANNING.md §11 Phase 7). */
    @Query("SELECT * FROM games WHERE language = :language AND mode = 'guided' ORDER BY id")
    suspend fun guidedGames(language: String): List<GameEntity>

    /** A language's free games, oldest first: free play's history (PLANNING.md §4.7). */
    @Query("SELECT * FROM games WHERE language = :language AND mode = 'free' ORDER BY id")
    suspend fun freeGames(language: String): List<GameEntity>

    @Query(
        "SELECT (SELECT COUNT(*) FROM guesses WHERE game_id = :id) + (SELECT COUNT(*) FROM hints WHERE game_id = :id)",
    )
    suspend fun moveCount(id: Long): Int

    @Query("UPDATE games SET hard = :hard WHERE id = :id")
    suspend fun updateHard(id: Long, hard: Boolean)

    @Query("UPDATE games SET status = :status, finished_at = :finishedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, finishedAt: Long?)

    @Query(
        """
        SELECT g.*,
            (SELECT COUNT(*) FROM guesses WHERE game_id = g.id) AS guess_count,
            (SELECT COUNT(*) FROM hints WHERE game_id = g.id) AS hint_count
        FROM games g ORDER BY g.id
        """,
    )
    fun observeSummaries(): Flow<List<GameSummary>>

    @Query(
        """
        SELECT g.*,
            (SELECT COUNT(*) FROM guesses WHERE game_id = g.id) AS guess_count,
            (SELECT COUNT(*) FROM hints WHERE game_id = g.id) AS hint_count
        FROM games g ORDER BY g.id
        """,
    )
    suspend fun summaries(): List<GameSummary>

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    // The backup file (PLANNING.md §5): every game with its moves, out and in, in one transaction.

    @Query("SELECT * FROM games ORDER BY id")
    suspend fun allGames(): List<GameEntity>

    @Query("SELECT * FROM guesses ORDER BY game_id, position")
    suspend fun allGuesses(): List<GuessEntity>

    @Query("SELECT * FROM hints ORDER BY game_id, position")
    suspend fun allHints(): List<HintEntity>

    @Insert
    suspend fun insertGuesses(guesses: List<GuessEntity>)

    @Insert
    suspend fun insertHints(hints: List<HintEntity>)

    /** Every game; their guesses and hints go with them (the foreign keys' cascade). */
    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    /** The games that count, finished, not practice, not the guided lesson: what Your data says the phone holds. */
    @Query("SELECT COUNT(*) FROM games WHERE practice = 0 AND status != 'playing' AND mode != 'guided'")
    fun observeCountedCount(): Flow<Int>
}
