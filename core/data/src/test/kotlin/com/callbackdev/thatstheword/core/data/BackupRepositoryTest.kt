package com.callbackdev.thatstheword.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.backup.BackupCodec
import com.callbackdev.thatstheword.core.domain.backup.BackupRead
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.Word
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * Your data (PLANNING.md §5, §11 Phase 6): a phone's games, notebook, milestones and choices to a
 * file and back onto another phone, replacing what that one held; a file whose daily is not the
 * schedule's word does not fit.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val rome = ZoneId.of("Europe/Rome")
    private val clock: Clock = Clock.fixed(ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, rome).toInstant(), rome)
    private val today = LocalDate.of(2026, 11, 20)
    private val lexicons = Lexicons(AssetWordFiles(context.assets))
    private val databases = mutableListOf<ThatsTheWordDatabase>()

    /** A phone: its database, its settings, and the repositories over them. */
    private inner class Phone(scope: TestScope, name: String) {
        val database = Room.inMemoryDatabaseBuilder(context, ThatsTheWordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { databases += it }
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
                folder.root.resolve("$name.preferences_pb")
            },
        ) { Language.ENGLISH }
        val games = GameRepository(database, lexicons) { clock }
        val notebook = NotebookRepository(database) { clock }
        val backups = BackupRepository(database, settings, lexicons) { clock }
    }

    @After
    fun close() = databases.forEach { it.close() }

    private fun solutionOf(language: Language, date: LocalDate = today): Word =
        lexicons.of(language).daily.word(DailyCalendar(clock).number(date))

    private fun otherWord(language: Language): Word {
        val daily = lexicons.of(language).daily
        return (1..daily.size).asSequence().map { daily.word(it) }.first { it != solutionOf(language) }
    }

    private fun Game.guess(word: Word, language: Language): Game =
        (submit(word, lexicons.of(language).accepted) as Submission.Accepted).game

    /** A phone with a little of everything: a foreign daily won with a hint, one under way, a free word, a word kept. */
    private suspend fun Phone.play() {
        settings.setGameLanguage(Language.SPANISH)
        settings.setLevel(Language.SPANISH, Level.ADVANCED)
        settings.setFont(AppFont.INTER)
        settings.setTipsSeen()

        val spanish = games.dailyGame(Language.SPANISH, today, Language.ENGLISH, hard = false)
        val tried = games.load(spanish)!!.game.guess(otherWord(Language.SPANISH), Language.SPANISH)
        games.save(spanish, tried)
        games.save(spanish, tried.takeHint(HintKind.LETTER).guess(solutionOf(Language.SPANISH), Language.SPANISH))
        notebook.save(Language.SPANISH, otherWord(Language.SPANISH))

        // Yesterday's, from the archive, and today's English one, under way.
        val archive = games.dailyGame(Language.ITALIAN, today.minusDays(1), Language.ENGLISH, hard = true)
        games.save(
            archive,
            games.load(archive)!!.game.guess(solutionOf(Language.ITALIAN, today.minusDays(1)), Language.ITALIAN),
        )
        val replay = games.practiceGame(Language.ITALIAN, today.minusDays(1), Language.ENGLISH, hard = false)
        games.save(replay, games.load(replay)!!.game.guess(otherWord(Language.ITALIAN), Language.ITALIAN))
        val english = games.dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false)
        games.save(english, games.load(english)!!.game.guess(otherWord(Language.ENGLISH), Language.ENGLISH))
        games.freeGame(Language.ENGLISH, Level.BASE, Language.ENGLISH, hard = false, random = Random(7))
    }

    @Test
    fun `a phone's data goes to a file and comes back on another`() = runTest {
        val old = Phone(this, "old").apply { play() }
        val backup = old.backups.backup("1.0.0")
        assertThat(backup.games).hasSize(5)
        assertThat(backup.countedGames).isEqualTo(2)
        assertThat(backup.notebook).hasSize(3)
        assertThat(backup.milestones.keys).containsExactly(Milestone.FIRST_FOREIGN_WIN)
        assertThat(backup.zone).isEqualTo("Europe/Rome")

        val read = BackupCodec.decode(BackupCodec.encode(backup))
        val file = (read as BackupRead.Ok).backup
        val new = Phone(this, "new")
        assertThat(new.backups.fits(file)).isTrue()
        new.backups.restore(file)

        val back = new.backups.backup("1.0.0")
        assertThat(back.games).isEqualTo(backup.games)
        assertThat(back.notebook).isEqualTo(backup.notebook)
        assertThat(back.milestones).isEqualTo(backup.milestones)
        assertThat(new.backups.contents.first()).isEqualTo(DataContents(games = 2, words = 3, milestones = 1))

        // The games are taken up where they were: the English daily under way, its guess kept.
        val english = new.games.dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false)
        val game = new.games.load(english)!!.game
        assertThat(game.state).isEqualTo(GameState.PLAYING)
        assertThat(game.guesses).hasSize(1)
        // The day already has its result: another game of it is practice, numbered after the one restored.
        val italian = new.games.practiceGame(Language.ITALIAN, today.minusDays(1), Language.ENGLISH, hard = false)
        assertThat(new.games.load(italian)!!.practice).isTrue()
    }

    @Test
    fun `the player's choices come back, the phone's own stay`() = runTest {
        val old = Phone(this, "old").apply { play() }
        val backup = old.backups.backup("1.0.0")
        val new = Phone(this, "new")
        new.backups.restore(backup)
        val settings = new.settings.current()
        assertThat(settings.gameLanguage).isEqualTo(Language.SPANISH)
        assertThat(settings.levelOf(Language.SPANISH)).isEqualTo(Level.ADVANCED)
        assertThat(settings.font).isEqualTo(AppFont.INTER)
        assertThat(settings.firstRunDone).isTrue()
        // The tips were the old phone's: this one has not shown them.
        assertThat(settings.tipsSeen).isFalse()
    }

    @Test
    fun `a restore replaces what the phone held`() = runTest {
        val old = Phone(this, "old")
        old.settings.setLevel(Language.ITALIAN, Level.COMPLETE)
        val empty = old.backups.backup("1.0.0")
        assertThat(empty.games).isEmpty()

        val phone = Phone(this, "phone").apply { play() }
        phone.settings.setLevel(Language.ITALIAN, Level.COMPLETE)
        phone.backups.restore(empty.copy(settings = empty.settings.copy(levels = emptyMap())))
        assertThat(phone.backups.contents.first().isEmpty).isTrue()
        assertThat(phone.notebook.entries().first()).isEmpty()
        assertThat(phone.notebook.milestones().first()).isEmpty()
        // A level the file has none for goes back to Base.
        assertThat(phone.settings.current().levelOf(Language.ITALIAN)).isEqualTo(Level.BASE)
    }

    @Test
    fun `a daily that is not the schedule's word does not fit`() = runTest {
        val old = Phone(this, "old").apply { play() }
        val backup = old.backups.backup("1.0.0")
        val wrong = backup.games.map { game ->
            if (game.mode is GameMode.Daily && game.language == Language.ENGLISH) {
                game.copy(solution = otherWord(Language.ENGLISH))
            } else {
                game
            }
        }
        assertThat(old.backups.fits(backup.copy(games = wrong))).isFalse()
        // A free word that is no solution of its language does not fit either.
        val free = backup.games.map { game ->
            if (game.mode is GameMode.Free) game.copy(solution = Word.ofIndices(25, 25, 25, 25, 25)) else game
        }
        assertThat(old.backups.fits(backup.copy(games = free))).isFalse()
    }
}
