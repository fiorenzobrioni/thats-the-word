package com.callbackdev.thatstheword.core.data

import androidx.room.withTransaction
import com.callbackdev.thatstheword.core.data.db.MilestoneEntity
import com.callbackdev.thatstheword.core.data.db.NotebookEntity
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.notebook.Notebook
import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.progress.Milestones
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.NotebookEntry
import com.callbackdev.thatstheword.core.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider

/**
 * The notebook (VISION §9, PLANNING.md §5) and the milestones (PLANNING.md §4.8). A game's
 * solution is written down when the game finishes, in the game's own transaction
 * ([GameRepository.save]); a word tried, when the player adds it from the end card. The milestones
 * are asked after each of these, unlocked once with their date, and never taken back.
 */
class NotebookRepository @Inject constructor(
    private val database: ThatsTheWordDatabase,
    private val clock: Provider<Clock>,
) {
    private val dao = database.notebook()

    /** Every word met, the latest first. */
    fun entries(): Flow<List<NotebookEntry>> = dao.observeEntries().map { rows -> rows.map(::entryOf) }

    /** How many words the notebook holds, in every language. */
    fun size(): Flow<Int> = dao.observeSize().distinctUntilChanged()

    /** The milestones unlocked, each with the instant it was. */
    fun milestones(): Flow<Map<Milestone, Instant>> = dao.observeMilestones().map { rows ->
        rows.mapNotNull { row -> milestoneOf(row.id)?.let { it to Instant.ofEpochMilli(row.unlockedAt) } }.toMap()
    }

    /** A word tried, added by hand (the owner, PLANNING §15, Open 8); the milestones it reached. */
    suspend fun save(language: Language, word: Word): Set<Milestone> = database.withTransaction {
        val key = Alphabet.of(language).keyForm(word)
        val entry = dao.entry(language.tag, key)?.let(::entryOf)
        dao.upsert(rowOf(Notebook.savedByHand(entry, language, word, today())))
        checkMilestones()
    }

    /** A word added by hand taken back: gone if no game hid it, else only its mark. */
    suspend fun unsave(language: Language, word: Word) {
        database.withTransaction {
            val key = Alphabet.of(language).keyForm(word)
            val entry = dao.entry(language.tag, key)?.let(::entryOf) ?: return@withTransaction
            if (!entry.savedByHand) return@withTransaction
            Notebook.unsaved(entry)?.let { dao.upsert(rowOf(it)) } ?: dao.delete(language.tag, key)
        }
    }

    /**
     * After a game finished, inside its transaction: its solution written down (not for practice),
     * then the milestones asked. Returns those it reached.
     */
    internal suspend fun afterGame(record: GameRecord): Set<Milestone> {
        val key = Alphabet.of(record.language).keyForm(record.solution)
        val entry = dao.entry(record.language.tag, key)?.let(::entryOf)
        Notebook.afterGame(entry, record, today())?.let { dao.upsert(rowOf(it)) }
        return checkMilestones()
    }

    /** The milestones the facts reach now and were not unlocked, unlocked now (inside a transaction). */
    private suspend fun checkMilestones(): Set<Milestone> {
        val unlocked = dao.milestones().mapNotNullTo(HashSet()) { milestoneOf(it.id) }
        val games = database.games().summaries().map { recordOf(it, clock.get()) }
        val reached = Milestones.newlyReached(Milestones.Facts(games, dao.size()), unlocked)
        val now = Instant.now(clock.get()).toEpochMilli()
        reached.forEach { dao.unlock(MilestoneEntity(it.name.lowercase(), now)) }
        return reached
    }

    private fun today(): LocalDate = LocalDate.now(clock.get())

    internal companion object {
        fun milestoneOf(id: String): Milestone? = Milestone.entries.firstOrNull { it.name.lowercase() == id }

        fun entryOf(row: NotebookEntity): NotebookEntry {
            val language = Language.ofTag(row.language) ?: error("no language is ${row.language}")
            return NotebookEntry(
                language = language,
                word = Alphabet.of(language).parse(row.word) ?: error("${row.word} is not a word of $language"),
                firstMet = LocalDate.ofEpochDay(row.firstMet),
                lastMet = LocalDate.ofEpochDay(row.lastMet),
                timesMet = row.timesMet,
                solved = row.solved,
                missed = row.missed,
                withHints = row.withHints,
                savedByHand = row.savedByHand,
            )
        }

        fun rowOf(entry: NotebookEntry) = NotebookEntity(
            language = entry.language.tag,
            word = Alphabet.of(entry.language).keyForm(entry.word),
            firstMet = entry.firstMet.toEpochDay(),
            lastMet = entry.lastMet.toEpochDay(),
            timesMet = entry.timesMet,
            solved = entry.solved,
            missed = entry.missed,
            withHints = entry.withHints,
            savedByHand = entry.savedByHand,
        )
    }
}
