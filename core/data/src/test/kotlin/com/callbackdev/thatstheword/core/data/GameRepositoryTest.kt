package com.callbackdev.thatstheword.core.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.db.GameEntity
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.GuidedGame
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.Word
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/** The games kept (PLANNING.md §12): saved after every move, taken up where they were, one counted daily a day. */
@RunWith(AndroidJUnit4::class)
class GameRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val rome = ZoneId.of("Europe/Rome")
    private var clock: Clock = Clock.fixed(ZonedDateTime.of(2026, 11, 20, 21, 30, 0, 0, rome).toInstant(), rome)
    private val today = LocalDate.of(2026, 11, 20)
    private val lexicons = Lexicons(AssetWordFiles(context.assets))
    private lateinit var database: ThatsTheWordDatabase

    @Before
    fun open() {
        database =
            Room.inMemoryDatabaseBuilder(context, ThatsTheWordDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun close() = database.close()

    /** A fresh repository on the same database: the app closed and opened again. */
    private fun repository() = GameRepository(database, lexicons) { clock }

    /** A word that is surely accepted and is not today's: another day's daily. */
    private fun otherWord(language: Language): Word {
        val daily = lexicons.of(language).daily
        val number = DailyCalendar(clock).number(today)
        return (1..daily.size).asSequence().map { daily.word(it) }.first { it != daily.word(number) }
    }

    @Test
    fun `a daily is saved after every guess and taken up where it was left`() = runTest {
        val id = repository().dailyGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)
        val start = repository().load(id)!!
        assertThat(start.game.setup.mode).isEqualTo(GameMode.Daily(today, DailyCalendar(clock).number(today)))
        assertThat(
            start.game.setup.solution,
        ).isEqualTo(lexicons.of(Language.ITALIAN).daily.word(DailyCalendar(clock).number(today)))
        assertThat(start.startedOn).isEqualTo(today)

        val accepted = start.game.submit(
            otherWord(Language.ITALIAN),
            lexicons.of(Language.ITALIAN).accepted,
        ) as Submission.Accepted
        repository().save(id, accepted.game)

        // Closed in the middle, and opened again.
        val again = repository().load(id)!!
        assertThat(again.game.moves).isEqualTo(accepted.game.moves)
        assertThat(again.game.guesses.single().marks).isEqualTo(accepted.guess.marks)
        assertThat(again.game.state).isEqualTo(GameState.PLAYING)

        val won = again.game.submit(
            again.game.setup.solution,
            lexicons.of(Language.ITALIAN).accepted,
        ) as Submission.Accepted
        repository().save(id, won.game)
        val finished = repository().load(id)!!
        assertThat(finished.game.state).isEqualTo(GameState.WON)
        assertThat(finished.game.guesses.last().marks).containsExactlyElementsIn(List(5) { Mark.RIGHT_PLACE })
        assertThat(database.games().game(id)!!.finishedAt).isNotNull()
    }

    @Test
    fun `a day has one counted daily a language`() = runTest {
        val first = repository().dailyGame(Language.SPANISH, today, Language.ITALIAN, hard = false)
        assertThat(repository().dailyGame(Language.SPANISH, today, Language.ITALIAN, hard = true)).isEqualTo(first)
        assertThat(repository().dailyGame(Language.ENGLISH, today, Language.ITALIAN, hard = false)).isNotEqualTo(first)

        val counted = database.games().game(first)!!
        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { database.games().insertGame(counted.copy(id = 0)) }
        }
        // A replay of a finished day is practice, numbered, and does not collide.
        database.games().insertGame(counted.copy(id = 0, practice = 1))
        database.games().insertGame(counted.copy(id = 0, practice = 2))
    }

    @Test
    fun `a daily started before midnight stays the earlier date's`() = runTest {
        clock = Clock.fixed(ZonedDateTime.of(2026, 11, 20, 23, 58, 0, 0, rome).toInstant(), rome)
        val id = repository().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false)
        clock = Clock.fixed(ZonedDateTime.of(2026, 11, 21, 0, 3, 0, 0, rome).toInstant(), rome)
        val game = repository().load(id)!!
        val won = game.game.submit(
            game.game.setup.solution,
            lexicons.of(Language.ENGLISH).accepted,
        ) as Submission.Accepted
        repository().save(id, won.game)

        val record = repository().records().first().single()
        assertThat(record.daily?.date).isEqualTo(today)
        assertThat(record.startedOn).isEqualTo(today)
        assertThat(record.onItsDate).isTrue()
        assertThat(record.state).isEqualTo(GameState.WON)
        assertThat(record.attemptsUsed).isEqualTo(1)
    }

    @Test
    fun `a hint is saved among the moves and replayed`() = runTest {
        // Italian played by an English speaker: hints are offered.
        val id = repository().dailyGame(Language.ITALIAN, today, Language.ENGLISH, hard = false)
        val start = repository().load(id)!!.game
        assertThat(start.hintsOffered).contains(HintKind.LETTER)
        val guessed = (
            start.submit(
                otherWord(Language.ITALIAN),
                lexicons.of(Language.ITALIAN).accepted,
            ) as Submission.Accepted
            ).game
        val hinted = guessed.takeHint(HintKind.LETTER)
        repository().save(id, hinted)

        val again = repository().load(id)!!.game
        assertThat(again.moves).isEqualTo(hinted.moves)
        assertThat(again.revealed).isEqualTo(hinted.revealed)
        assertThat(again.attemptsLeft).isEqualTo(4)
        assertThat(repository().records().first().single().hints).isEqualTo(1)
    }

    @Test
    fun `a game's moves go with it`() = runTest {
        val id = repository().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false)
        val game = repository().load(id)!!.game
        repository().save(
            id,
            (
                game.submit(
                    otherWord(Language.ENGLISH),
                    lexicons.of(Language.ENGLISH).accepted,
                ) as Submission.Accepted
                ).game,
        )
        assertThat(database.games().guesses(id)).hasSize(1)

        database.games().deleteGame(id)
        assertThat(database.games().guesses(id)).isEmpty()
        assertThat(repository().load(id)).isNull()
    }

    @Test
    fun `the daily of a date after today is not started`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository().dailyGame(Language.ENGLISH, today.plusDays(1), Language.ENGLISH, hard = false)
            }
        }
        assertThat(repository().records().first()).isEmpty()
    }

    @Test
    fun `the stored forms are the keys' lower case`() = runTest {
        val id = repository().dailyGame(Language.SPANISH, today, Language.SPANISH, hard = false)
        val row: GameEntity = database.games().game(id)!!
        assertThat(row.solution).matches("[a-zñ]{5}")
        assertThat(row.language).isEqualTo("es")
        assertThat(row.status).isEqualTo(GameRepository.STATUS_PLAYING)
    }

    /** Plays [id] to its end: won with its solution, or lost with [other] six times. */
    private suspend fun finish(id: Long, won: Boolean) {
        var game = repository().load(id)!!.game
        val accepted = lexicons.of(game.setup.language).accepted
        val word = if (won) game.setup.solution else otherWord(game.setup.language)
        while (!game.state.finished) game = (game.submit(word, accepted) as Submission.Accepted).game
        repository().save(id, game)
    }

    @Test
    fun `a finished day is replayed as practice, and the first result stands`() = runTest {
        val counted = repository().dailyGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository().practiceGame(Language.ITALIAN, today, Language.ITALIAN, false)
            }
        }
        finish(counted, won = false)

        val first = repository().practiceGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)
        assertThat(first).isNotEqualTo(counted)
        assertThat(repository().load(first)!!.practice).isTrue()
        assertThat(
            repository().load(first)!!.game.setup.solution,
        ).isEqualTo(repository().load(counted)!!.game.setup.solution)
        // Left unfinished, it is taken up; finished, the next is a new one.
        assertThat(repository().practiceGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)).isEqualTo(first)
        finish(first, won = true)
        val second = repository().practiceGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)
        assertThat(second).isNotEqualTo(first)
        // The counted game is still the day's.
        assertThat(repository().dailyGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)).isEqualTo(counted)

        val records = repository().records().first()
        assertThat(records.count { it.practice }).isEqualTo(2)
        assertThat(
            com.callbackdev.thatstheword.core.domain.daily.Dailies.status(records, Language.ITALIAN, today),
        ).isEqualTo(com.callbackdev.thatstheword.core.domain.daily.DayStatus.MISSED)
    }

    @Test
    fun `an archive daily is the earlier date's, started today`() = runTest {
        val earlier = today.minusDays(3)
        val id = repository().dailyGame(Language.SPANISH, earlier, Language.ITALIAN, hard = false)
        val saved = repository().load(id)!!
        assertThat((saved.game.setup.mode as GameMode.Daily).date).isEqualTo(earlier)
        assertThat(saved.startedOn).isEqualTo(today)
        assertThat(
            saved.game.setup.solution,
        ).isEqualTo(lexicons.of(Language.SPANISH).daily.word(DailyCalendar(clock).number(earlier)))
    }

    @Test
    fun `free play takes up its unfinished game, then draws a new word at the level`() = runTest {
        val first = repository().freeGame(
            Language.ENGLISH,
            Level.BASE,
            Language.ITALIAN,
            hard = false,
            random = Random(1),
        )
        val game = repository().load(first)!!.game
        assertThat(game.setup.mode).isEqualTo(GameMode.Free(Level.BASE))
        assertThat(lexicons.of(Language.ENGLISH).solutions.levelOf(game.setup.solution)).isEqualTo(Level.BASE)
        // Under way: taken up, even when another level is asked for now.
        assertThat(
            repository().freeGame(Language.ENGLISH, Level.ADVANCED, Language.ITALIAN, false, Random(2)),
        ).isEqualTo(first)
        finish(first, won = true)
        val second = repository().freeGame(Language.ENGLISH, Level.ADVANCED, Language.ITALIAN, false, Random(1))
        assertThat(second).isNotEqualTo(first)
        val next = repository().load(second)!!.game
        assertThat(next.setup.mode).isEqualTo(GameMode.Free(Level.ADVANCED))
        // A word solved in free play is not drawn again while the level has others.
        assertThat(next.setup.solution).isNotEqualTo(game.setup.solution)
        // Each language has its own free game.
        assertThat(repository().freeGame(Language.ITALIAN, Level.BASE, Language.ITALIAN, false)).isNotEqualTo(second)
    }

    @Test
    fun `Hard chosen before the first move holds, and after it the game keeps its own`() = runTest {
        val id = repository().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false)
        repository().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = true)
        assertThat(repository().load(id)!!.game.setup.hard).isTrue()

        val accepted = repository().load(id)!!.game.submit(
            otherWord(Language.ENGLISH),
            lexicons.of(Language.ENGLISH).accepted,
        ) as Submission.Accepted
        repository().save(id, accepted.game)
        repository().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false)
        assertThat(repository().load(id)!!.game.setup.hard).isTrue()
    }

    @Test
    fun `the guided game is the language's fixed word, taken up, then started afresh, and counted nowhere`() = runTest {
        val id = repository().guidedGame(Language.SPANISH, Language.ITALIAN)
        val game = repository().load(id)!!.game
        assertThat(game.setup.mode).isEqualTo(GameMode.Guided)
        assertThat(game.setup.solution).isEqualTo(GuidedGame.solution(Language.SPANISH))
        assertThat(game.setup.hard).isFalse()
        // A foreign game has its hints, a lesson's too.
        assertThat(game.setup.hintKinds).contains(HintKind.LETTER)
        assertThat(repository().guidedGame(Language.SPANISH, Language.ITALIAN)).isEqualTo(id)
        finish(id, won = true)
        val again = repository().guidedGame(Language.SPANISH, Language.ITALIAN)
        assertThat(again).isNotEqualTo(id)
        // A lesson: no word kept, no milestone (a first win in another language would have been one).
        assertThat(NotebookRepository(database) { clock }.entries().first()).isEmpty()
        assertThat(NotebookRepository(database) { clock }.milestones().first()).isEmpty()
        assertThat(repository().records().first().none { it.counted }).isTrue()
    }

    @Test
    fun `today's word is finished once its counted game is won or lost`() = runTest {
        assertThat(repository().dailyFinished(Language.ITALIAN, today)).isFalse()
        val id = repository().dailyGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)
        assertThat(repository().dailyFinished(Language.ITALIAN, today)).isFalse()
        finish(id, won = false)
        assertThat(repository().dailyFinished(Language.ITALIAN, today)).isTrue()
        assertThat(repository().dailyFinished(Language.ENGLISH, today)).isFalse()
    }
}
