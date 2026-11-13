package com.callbackdev.thatstheword.core.data

import androidx.room.withTransaction
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.MODE_DAILY
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.MODE_FREE
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.MODE_GUIDED
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.STATUS_LOST
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.hintOf
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.languageOf
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.parseStored
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.statusOf
import com.callbackdev.thatstheword.core.data.NotebookRepository.Companion.entryOf
import com.callbackdev.thatstheword.core.data.NotebookRepository.Companion.milestoneOf
import com.callbackdev.thatstheword.core.data.NotebookRepository.Companion.rowOf
import com.callbackdev.thatstheword.core.data.db.GameEntity
import com.callbackdev.thatstheword.core.data.db.GuessEntity
import com.callbackdev.thatstheword.core.data.db.HintEntity
import com.callbackdev.thatstheword.core.data.db.MilestoneEntity
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.backup.Backup
import com.callbackdev.thatstheword.core.domain.backup.BackupGame
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.Move
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.GameMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider

/** What this phone holds, as Your data says it before a copy is made or brought back. */
data class DataContents(val games: Int, val words: Int, val milestones: Int) {
    val isEmpty: Boolean get() = games == 0 && words == 0 && milestones == 0
}

/**
 * Your data (PLANNING.md §5, §11 Phase 6): the whole of it to a backup and back. Files are the
 * caller's: the Storage Access Framework hands the page a stream, so the game needs no storage
 * permission and sends nothing; this class makes and takes their contents.
 *
 * A backup is read in one transaction, so it never holds half a move; a restore replaces the games,
 * the notebook and the milestones in one transaction, then the player's choices, only once the
 * file was read whole and checked ([BackupCodec][com.callbackdev.thatstheword.core.domain.backup.BackupCodec]
 * and [fits]) and the player said yes (the page's).
 */
class BackupRepository @Inject constructor(
    private val database: ThatsTheWordDatabase,
    private val settings: SettingsRepository,
    private val lexicons: Lexicons,
    private val clock: Provider<Clock>,
) {
    private val games = database.games()
    private val notebook = database.notebook()

    /** How many games that count, words and milestones there are, as they change. */
    val contents: Flow<DataContents> = combine(
        games.observeCountedCount(),
        notebook.observeSize(),
        notebook.observeMilestoneCount(),
        ::DataContents,
    ).distinctUntilChanged()

    /** Everything a new phone could not rebuild, as it stands now. */
    suspend fun backup(appVersion: String): Backup {
        val now = clock.get()
        val chosen = settings.current()
        return database.withTransaction {
            val guesses = games.allGuesses().groupBy { it.gameId }
            val hints = games.allHints().groupBy { it.gameId }
            Backup(
                exportedAt = Instant.now(now),
                zone = now.zone.id,
                appVersion = appVersion,
                settings = chosen,
                games = games.allGames().map { entity ->
                    val language = languageOf(entity.language)
                    val alphabet = Alphabet.of(language)
                    val moves = (
                        guesses[entity.id].orEmpty().map {
                            it.position to Move.Guessed(alphabet.parseStored(it.word))
                        } +
                            hints[entity.id].orEmpty().map { it.position to Move.Hinted(hintOf(it.kind)) }
                        ).sortedBy { it.first }.map { it.second }
                    BackupGame(
                        language = language,
                        interfaceLanguage = languageOf(entity.uiLanguage),
                        mode = modeOf(entity, now),
                        solution = alphabet.parseStored(entity.solution),
                        attempts = entity.attempts,
                        hard = entity.hard,
                        moves = moves,
                        startedAt = Instant.ofEpochMilli(entity.startedAt),
                        startedOn = LocalDate.ofEpochDay(entity.startedOn),
                        finishedAt = entity.finishedAt?.let(Instant::ofEpochMilli),
                        practice = entity.practice,
                        // Lost with attempts left: a free word left for another (PLANNING.md §4.7).
                        conceded = entity.status == STATUS_LOST && moves.size < entity.attempts,
                    )
                },
                notebook = notebook.allEntries().map(::entryOf),
                milestones = notebook.milestones().mapNotNull { row ->
                    milestoneOf(row.id)?.let { it to Instant.ofEpochMilli(row.unlockedAt) }
                }.toMap(),
            )
        }
    }

    /**
     * Whether [backup]'s games are this version's words (PLANNING.md §6.5): every daily is the
     * frozen schedule's word of its date, which is never changed once published, so a daily that is
     * not is a file changed by hand; free play's words are solutions of their language. A file that
     * fails this is damaged, and is not restored.
     */
    fun fits(backup: Backup): Boolean = backup.games.all { game ->
        val lexicon = lexicons.of(game.language)
        when (val mode = game.mode) {
            is GameMode.Daily ->
                mode.number in 1..lexicon.daily.size && lexicon.daily.word(mode.number) == game.solution

            is GameMode.Free -> lexicon.solutions.levelOf(game.solution) != null

            GameMode.Guided -> true
        }
    }

    /**
     * Replaces this phone's games, notebook and milestones with [backup]'s, in one transaction, then
     * its choices (PLANNING.md §5: a restore replaces, after the player's confirmation). Should the
     * transaction fail, nothing of it is written and the phone is as it was.
     */
    suspend fun restore(backup: Backup) {
        database.withTransaction {
            games.deleteAllGames()
            notebook.deleteAllEntries()
            notebook.deleteAllMilestones()
            backup.games.forEach { game ->
                val alphabet = Alphabet.of(game.language)
                val id = games.insertGame(game.toEntity())
                val moves = game.moves.withIndex()
                games.insertGuesses(
                    moves.mapNotNull { (position, move) ->
                        (move as? Move.Guessed)?.let { GuessEntity(id, position, alphabet.keyForm(it.word)) }
                    },
                )
                games.insertHints(
                    moves.mapNotNull { (position, move) ->
                        (move as? Move.Hinted)?.let { HintEntity(id, position, it.kind.name.lowercase(Locale.ROOT)) }
                    },
                )
            }
            notebook.insertEntries(backup.notebook.map(::rowOf))
            notebook.insertMilestones(
                backup.milestones.map { (milestone, at) ->
                    MilestoneEntity(milestone.name.lowercase(Locale.ROOT), at.toEpochMilli())
                },
            )
        }
        settings.restore(backup.settings)
    }

    private fun BackupGame.toEntity() = GameEntity(
        language = language.tag,
        mode = when (mode) {
            is GameMode.Daily -> MODE_DAILY
            is GameMode.Free -> MODE_FREE
            GameMode.Guided -> MODE_GUIDED
        },
        dailyDate = (mode as? GameMode.Daily)?.date?.toEpochDay(),
        level = (mode as? GameMode.Free)?.level?.code,
        solution = Alphabet.of(language).keyForm(solution),
        length = solution.length,
        attempts = attempts,
        hard = hard,
        uiLanguage = interfaceLanguage.tag,
        status = statusOf(state),
        startedAt = startedAt.toEpochMilli(),
        startedOn = startedOn.toEpochDay(),
        finishedAt = finishedAt?.toEpochMilli(),
        practice = practice,
    )

    /** The day a file's name is dated by: today, here. */
    fun today(): LocalDate = DailyCalendar(clock.get()).today()
}
