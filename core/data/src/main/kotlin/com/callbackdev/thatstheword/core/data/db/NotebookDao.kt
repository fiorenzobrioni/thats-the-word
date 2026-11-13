package com.callbackdev.thatstheword.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The notebook's rows and the milestones unlocked (PLANNING.md §5), written from Phase 5. */
@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebook WHERE language = :language AND word = :word")
    suspend fun entry(language: String, word: String): NotebookEntity?

    @Upsert
    suspend fun upsert(entry: NotebookEntity)

    @Query("DELETE FROM notebook WHERE language = :language AND word = :word")
    suspend fun delete(language: String, word: String)

    /** The words met, the latest first. */
    @Query("SELECT * FROM notebook ORDER BY last_met DESC, first_met DESC, word")
    fun observeEntries(): Flow<List<NotebookEntity>>

    @Query("SELECT COUNT(*) FROM notebook")
    suspend fun size(): Int

    @Query("SELECT COUNT(*) FROM notebook")
    fun observeSize(): Flow<Int>

    @Query("SELECT * FROM milestones ORDER BY unlocked_at")
    suspend fun milestones(): List<MilestoneEntity>

    @Query("SELECT * FROM milestones ORDER BY unlocked_at")
    fun observeMilestones(): Flow<List<MilestoneEntity>>

    @Upsert
    suspend fun unlock(milestone: MilestoneEntity)

    // The backup file (PLANNING.md §5).

    @Query("SELECT * FROM notebook ORDER BY language, word")
    suspend fun allEntries(): List<NotebookEntity>

    @Insert
    suspend fun insertEntries(entries: List<NotebookEntity>)

    @Insert
    suspend fun insertMilestones(milestones: List<MilestoneEntity>)

    @Query("DELETE FROM notebook")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM milestones")
    suspend fun deleteAllMilestones()

    @Query("SELECT COUNT(*) FROM milestones")
    fun observeMilestoneCount(): Flow<Int>
}
