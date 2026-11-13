package com.callbackdev.thatstheword.core.data

import androidx.room.withTransaction
import com.callbackdev.thatstheword.core.data.db.GameEntity
import com.callbackdev.thatstheword.core.data.db.GuessEntity
import com.callbackdev.thatstheword.core.data.db.HintEntity
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.FreePlay
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.GuidedGame
import com.callbackdev.thatstheword.core.domain.game.Move
import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider
import kotlin.random.Random

/** A game as it is kept: its row's id, the game replayed from its moves, and the date it was started on. */
data class SavedGame(val id: Long, val game: Game, val startedOn: LocalDate, val practice: Boolean)

/**
 * The games (PLANNING.md §4.3, §5): a game is saved after every move, guess or hint, and taken up
 * where it was left, on any page, after any restart, by replaying its moves. The marks are never
 * stored.
 *
 * The clock is read afresh at every call ([clock]), so a phone carried across a time zone, or a
 * night that passes with the app open, gives the date it is now.
 */
class GameRepository @Inject constructor(
    private val database: ThatsTheWordDatabase,
    private val lexicons: Lexicons,
    private val clock: Provider<Clock>,
) {
    private val dao = database.games()
    private val notebook = NotebookRepository(database, clock)

    private fun calendar() = DailyCalendar(clock.get())

    /**
     * The id of [language]'s counted daily of [date], started now if it was not (the owner,
     * PLANNING §15, Open 9: a daily belongs to the date it was started on, and a day has one
     * counted game). [hard] and [interfaceLanguage] are read at the start and hold for the game;
     * a game taken up before its first move takes the Hard chosen since (PLANNING.md §4.4).
     */
    suspend fun dailyGame(language: Language, date: LocalDate, interfaceLanguage: Language, hard: Boolean): Long =
        database.withTransaction {
            dao.countedDaily(language.tag, date.toEpochDay())?.let { takeUp(it, hard) }
                ?: insertDaily(language, date, interfaceLanguage, hard, practice = 0)
        }

    /**
     * The id of a replay of [language]'s daily of [date] as practice (PLANNING.md §4.6): not
     * counted, not in the calendar. A replay left unfinished is taken up; otherwise a new one is
     * started, numbered after the day's others. Only a day already finished is replayed.
     */
    suspend fun practiceGame(language: Language, date: LocalDate, interfaceLanguage: Language, hard: Boolean): Long =
        database.withTransaction {
            val counted = dao.countedDaily(language.tag, date.toEpochDay())
            require(counted != null && counted.status != STATUS_PLAYING) {
                "the daily of $date has no result to replay"
            }
            val replays = dao.practiceDailies(language.tag, date.toEpochDay())
            replays.lastOrNull { it.status == STATUS_PLAYING }?.let { takeUp(it, hard) }
                ?: insertDaily(
                    language,
                    date,
                    interfaceLanguage,
                    hard,
                    practice =
                    (replays.maxOfOrNull { it.practice } ?: 0) + 1,
                )
        }

    /**
     * The id of a free game in [language] (PLANNING.md §4.7): the one left unfinished, taken up
     * wherever it was, whatever its level; otherwise a new word drawn at [level], never one of the
     * language's last 200 free games nor one solved in free play, until the level runs out. One
     * free game a language is under way at a time, so a word is never left behind unseen.
     */
    suspend fun freeGame(
        language: Language,
        level: Level,
        interfaceLanguage: Language,
        hard: Boolean,
        random: Random = Random.Default,
    ): Long = database.withTransaction {
        val history = dao.freeGames(language.tag)
        history.lastOrNull { it.status == STATUS_PLAYING }?.let { return@withTransaction takeUp(it, hard) }
        val alphabet = Alphabet.of(language)
        val played = history.map { FreePlay.Played(alphabet.parseStored(it.solution), it.status == STATUS_WON) }
        val solution = FreePlay.draw(lexicons.of(language).solutions.at(level), played, random)
        val now = clock.get()
        dao.insertGame(
            GameEntity(
                language = language.tag,
                mode = MODE_FREE,
                dailyDate = null,
                level = level.code,
                solution = alphabet.keyForm(solution),
                length = solution.length,
                attempts = GameSetup.ATTEMPTS,
                hard = hard,
                uiLanguage = interfaceLanguage.tag,
                status = STATUS_PLAYING,
                startedAt = Instant.now(now).toEpochMilli(),
                startedOn = LocalDate.now(now).toEpochDay(),
                finishedAt = null,
                practice = 0,
            ),
        )
    }

    /**
     * Leaves [language]'s free word under way for a new one (PLANNING.md §4.7; the owner, §15,
     * 13 Nov 2026): a word tried is lost, over at once, its solution in the notebook as any word
     * missed, and its id returned, so its end card shows what it was; a word never tried is no game,
     * and goes without a trace (null). Null too when no free word is under way.
     */
    suspend fun leaveFree(language: Language): Long? = database.withTransaction {
        val entity = dao.freeGames(language.tag).lastOrNull { it.status == STATUS_PLAYING }
            ?: return@withTransaction null
        if (dao.moveCount(entity.id) == 0) {
            dao.deleteGame(entity.id)
            return@withTransaction null
        }
        save(entity.id, load(entity).game.concede())
        entity.id
    }

    /**
     * The id of the guided first game in [language] (PLANNING.md §11 Phase 7): the one under way
     * taken up, else a new one on the language's fixed word. Never Hard: it teaches the rules, not
     * the harder one. Started again from Settings ("How to play") as often as the player likes; a
     * lesson, so it is counted nowhere.
     */
    suspend fun guidedGame(language: Language, interfaceLanguage: Language): Long = database.withTransaction {
        dao.guidedGames(language.tag).lastOrNull { it.status == STATUS_PLAYING }?.let { return@withTransaction it.id }
        val solution = GuidedGame.solution(language)
        val now = clock.get()
        dao.insertGame(
            GameEntity(
                language = language.tag,
                mode = MODE_GUIDED,
                dailyDate = null,
                level = null,
                solution = Alphabet.of(language).keyForm(solution),
                length = solution.length,
                attempts = GameSetup.ATTEMPTS,
                hard = false,
                uiLanguage = interfaceLanguage.tag,
                status = STATUS_PLAYING,
                startedAt = Instant.now(now).toEpochMilli(),
                startedOn = LocalDate.now(now).toEpochDay(),
                finishedAt = null,
                practice = 0,
            ),
        )
    }

    /**
     * Whether [language]'s word of [date] is finished, its counted game won or lost: the daily
     * reminder stays quiet then (PLANNING.md §8).
     */
    suspend fun dailyFinished(language: Language, date: LocalDate): Boolean =
        dao.countedDaily(language.tag, date.toEpochDay())?.let { it.status != STATUS_PLAYING } ?: false

    /** A game taken up: before its first move it takes the Hard chosen now; after, it keeps its own. */
    private suspend fun takeUp(game: GameEntity, hard: Boolean): Long {
        if (game.status == STATUS_PLAYING && game.hard != hard && dao.moveCount(game.id) == 0) {
            dao.updateHard(game.id, hard)
        }
        return game.id
    }

    private suspend fun insertDaily(
        language: Language,
        date: LocalDate,
        interfaceLanguage: Language,
        hard: Boolean,
        practice: Int,
    ): Long {
        val calendar = calendar()
        val lexicon = lexicons.of(language)
        val number = calendar.number(date)
        require(calendar.isPlayable(date, lexicon.daily.size)) { "the daily of $date is not playable" }
        val solution = lexicon.daily.word(number)
        val now = clock.get()
        return dao.insertGame(
            GameEntity(
                language = language.tag,
                mode = MODE_DAILY,
                dailyDate = date.toEpochDay(),
                level = null,
                solution = Alphabet.of(language).keyForm(solution),
                length = solution.length,
                attempts = GameSetup.ATTEMPTS,
                hard = hard,
                uiLanguage = interfaceLanguage.tag,
                status = STATUS_PLAYING,
                startedAt = Instant.now(now).toEpochMilli(),
                startedOn = LocalDate.now(now).toEpochDay(),
                finishedAt = null,
                practice = practice,
            ),
        )
    }

    /** How many dailies [language]'s schedule holds: the archive's last date (PLANNING.md §4.6). */
    fun scheduleLength(language: Language): Int = lexicons.of(language).daily.size

    /** A game, replayed from its moves; null once it is gone. */
    fun game(id: Long): Flow<SavedGame?> = dao.observeGame(id)
        .distinctUntilChanged()
        .map { entity -> entity?.let { load(it) } }

    suspend fun load(id: Long): SavedGame? = dao.game(id)?.let { load(it) }

    private suspend fun load(entity: GameEntity): SavedGame {
        val language = languageOf(entity.language)
        val alphabet = Alphabet.of(language)
        val moves = (
            dao.guesses(entity.id).map { it.position to Move.Guessed(alphabet.parseStored(it.word)) } +
                dao.hints(entity.id).map { it.position to Move.Hinted(hintOf(it.kind)) }
            ).sortedBy { it.first }.map { it.second }
        return SavedGame(
            id = entity.id,
            // Lost with attempts left: a free word left for another (PLANNING.md §4.7).
            game = Game.restore(setupOf(entity), moves, conceded = entity.status == STATUS_LOST),
            startedOn = LocalDate.ofEpochDay(entity.startedOn),
            practice = entity.practice > 0,
        )
    }

    /**
     * Saves [game]'s moves that are not saved yet, and where it stands, in one transaction: a
     * crash between two moves loses nothing that was played. The move that ends the game writes
     * its solution in the notebook (not a replay's: PLANNING.md §4.6) and asks the milestones, in
     * the same transaction; the milestones it reached are returned, for the end card to say once.
     */
    suspend fun save(id: Long, game: Game): Set<Milestone> {
        val alphabet = Alphabet.of(game.setup.language)
        return database.withTransaction {
            val entity = checkNotNull(dao.game(id)) { "no game $id to save" }
            val saved = dao.guesses(id).size + dao.hints(id).size
            for (position in saved until game.moves.size) {
                when (val move = game.moves[position]) {
                    is Move.Guessed -> dao.insertGuess(GuessEntity(id, position, alphabet.keyForm(move.word)))
                    is Move.Hinted -> dao.insertHint(HintEntity(id, position, move.kind.name.lowercase()))
                }
            }
            val finishedAt = if (game.state.finished) Instant.now(clock.get()).toEpochMilli() else null
            dao.updateStatus(id, statusOf(game.state), finishedAt)
            if (entity.status == STATUS_PLAYING && game.state.finished) {
                val record = GameRecord.of(game, LocalDate.ofEpochDay(entity.startedOn), entity.practice > 0)
                notebook.afterGame(record)
            } else {
                emptySet()
            }
        }
    }

    /** Every game as the progress reads it (PLANNING.md §4.8), in the order they were started. */
    fun records(): Flow<List<GameRecord>> = dao.observeSummaries().map { rows ->
        rows.map { recordOf(it, clock.get()) }
    }

    private fun setupOf(entity: GameEntity): GameSetup {
        val language = languageOf(entity.language)
        val interfaceLanguage = languageOf(entity.uiLanguage)
        val solution = Alphabet.of(language).parseStored(entity.solution)
        return GameSetup(
            language = language,
            mode = modeOf(entity, clock.get()),
            solution = solution,
            attempts = entity.attempts,
            hard = entity.hard,
            interfaceLanguage = interfaceLanguage,
            hintKinds = lexicons.of(language).hintKinds(solution, interfaceLanguage),
        )
    }

    companion object {
        const val MODE_DAILY = "daily"
        const val MODE_FREE = "free"
        const val MODE_GUIDED = "guided"
        const val STATUS_PLAYING = "playing"
        const val STATUS_WON = "won"
        const val STATUS_LOST = "lost"

        internal fun languageOf(tag: String): Language = Language.ofTag(tag) ?: error("no language is $tag")

        internal fun hintOf(kind: String): HintKind = HintKind.valueOf(kind.uppercase())

        internal fun statusOf(state: GameState): String = when (state) {
            GameState.PLAYING -> STATUS_PLAYING
            GameState.WON -> STATUS_WON
            GameState.LOST -> STATUS_LOST
        }

        internal fun stateOf(status: String): GameState = when (status) {
            STATUS_PLAYING -> GameState.PLAYING
            STATUS_WON -> GameState.WON
            STATUS_LOST -> GameState.LOST
            else -> error("no state is $status")
        }

        internal fun Alphabet.parseStored(keyForm: String): Word =
            parse(keyForm) ?: error("$keyForm is not a word of $language")
    }
}
