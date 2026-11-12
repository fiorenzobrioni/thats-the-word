package com.callbackdev.thatstheword.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The player's games, their moves, the notebook and the milestones (PLANNING.md §5), in
 * `thatstheword.db`. A contract: the schema is exported to `core/data/schemas/` and committed,
 * every migration has its test, and the database is never dropped to make a migration fit.
 */
@Database(
    entities = [
        GameEntity::class,
        GuessEntity::class,
        HintEntity::class,
        NotebookEntity::class,
        MilestoneEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ThatsTheWordDatabase : RoomDatabase() {
    abstract fun games(): GameDao

    companion object {
        const val NAME = "thatstheword.db"
    }
}
