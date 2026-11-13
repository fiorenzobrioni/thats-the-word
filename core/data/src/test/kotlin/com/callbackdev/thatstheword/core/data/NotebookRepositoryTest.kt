package com.callbackdev.thatstheword.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.Word
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The notebook and the milestones kept (PLANNING.md §11 Phase 5): a solution written down when its
 * game ends, a word tried only by hand, a replay leaving both alone, the milestones unlocked once.
 */
@RunWith(AndroidJUnit4::class)
class NotebookRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val rome = ZoneId.of("Europe/Rome")
    private val clock: Clock = Clock.fixed(ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, rome).toInstant(), rome)
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

    private fun games() = GameRepository(database, lexicons) { clock }

    private fun notebook() = NotebookRepository(database) { clock }

    private fun solutionOf(language: Language): Word =
        lexicons.of(language).daily.word(DailyCalendar(clock).number(today))

    private fun otherWord(language: Language): Word {
        val daily = lexicons.of(language).daily
        return (1..daily.size).asSequence().map { daily.word(it) }.first { it != solutionOf(language) }
    }

    private fun Game.guess(word: Word, language: Language): Game =
        (submit(word, lexicons.of(language).accepted) as Submission.Accepted).game

    @Test
    fun `a solution is written down when its game ends, not before`() = runTest {
        val id = games().dailyGame(Language.SPANISH, today, Language.ENGLISH, hard = false)
        val start = games().load(id)!!.game
        val tried = start.guess(otherWord(Language.SPANISH), Language.SPANISH)
        assertThat(games().save(id, tried)).isEmpty()
        assertThat(notebook().entries().first()).isEmpty()

        val hinted = tried.takeHint(HintKind.LETTER)
        val won = hinted.guess(solutionOf(Language.SPANISH), Language.SPANISH)
        // A first win in a foreign language: the end card says it once.
        assertThat(games().save(id, won)).containsExactly(Milestone.FIRST_FOREIGN_WIN)
        val entry = notebook().entries().first().single()
        assertThat(entry.word).isEqualTo(solutionOf(Language.SPANISH))
        assertThat(entry.language).isEqualTo(Language.SPANISH)
        assertThat(entry.firstMet).isEqualTo(today)
        assertThat(entry.solved).isEqualTo(1)
        assertThat(entry.withHints).isEqualTo(1)
        assertThat(entry.savedByHand).isFalse()
        assertThat(notebook().milestones().first().keys).containsExactly(Milestone.FIRST_FOREIGN_WIN)

        // Saved again, the game finished already: nothing written twice, nothing unlocked twice.
        assertThat(games().save(id, won)).isEmpty()
        assertThat(notebook().entries().first().single().timesMet).isEqualTo(1)
    }

    @Test
    fun `a replay as practice leaves the notebook as it was`() = runTest {
        val id = games().dailyGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)
        games().save(id, games().load(id)!!.game.guess(solutionOf(Language.ITALIAN), Language.ITALIAN))
        val replay = games().practiceGame(Language.ITALIAN, today, Language.ITALIAN, hard = false)
        games().save(replay, games().load(replay)!!.game.guess(solutionOf(Language.ITALIAN), Language.ITALIAN))
        assertThat(notebook().entries().first().single().timesMet).isEqualTo(1)
    }

    @Test
    fun `a word tried is added by hand, and taken back`() = runTest {
        val word = otherWord(Language.ENGLISH)
        notebook().save(Language.ENGLISH, word)
        val added = notebook().entries().first().single()
        assertThat(added.word).isEqualTo(word)
        assertThat(added.savedByHand).isTrue()
        assertThat(notebook().size().first()).isEqualTo(1)
        notebook().unsave(Language.ENGLISH, word)
        assertThat(notebook().entries().first()).isEmpty()
    }

    @Test
    fun `a solution taken back by hand keeps its game`() = runTest {
        val id = games().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false)
        games().save(id, games().load(id)!!.game.guess(solutionOf(Language.ENGLISH), Language.ENGLISH))
        notebook().save(Language.ENGLISH, solutionOf(Language.ENGLISH))
        assertThat(notebook().entries().first().single().savedByHand).isTrue()
        notebook().unsave(Language.ENGLISH, solutionOf(Language.ENGLISH))
        val entry = notebook().entries().first().single()
        assertThat(entry.savedByHand).isFalse()
        assertThat(entry.solved).isEqualTo(1)
    }
}
