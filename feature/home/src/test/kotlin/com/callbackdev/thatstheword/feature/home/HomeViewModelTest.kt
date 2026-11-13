package com.callbackdev.thatstheword.feature.home

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.DayTicker
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.NotebookRepository
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/** The start page's view model on Room, the settings and the real words (PLANNING.md §11 Phase 4). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val rome = ZoneId.of("Europe/Rome")
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 12, 3, 9, 0, 0, 0, rome).toInstant(), rome)
    private val today = LocalDate.of(2026, 12, 3)
    private val lexicons = Lexicons(AssetWordFiles(context.assets))
    private lateinit var database: ThatsTheWordDatabase
    private lateinit var settings: SettingsRepository

    @Before
    fun open() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        database =
            Room.inMemoryDatabaseBuilder(context, ThatsTheWordDatabase::class.java).allowMainThreadQueries().build()
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create { folder.root.resolve("settings.preferences_pb") },
        ) { Language.ITALIAN }
    }

    @After
    fun close() {
        database.close()
        Dispatchers.resetMain()
    }

    private val games get() = GameRepository(database, lexicons) { clock }

    private fun viewModel() = HomeViewModel(
        settings,
        games,
        { Language.ITALIAN },
        { clock },
        SavedStateHandle(),
        DayTicker { clock },
        NotebookRepository(database) { clock },
    )

    /** The state once [until] holds, collected as the page would. */
    private fun HomeViewModel.await(until: (HomeUiState) -> Boolean = { true }): HomeUiState = runBlocking {
        withTimeout(10_000) { state.filterNotNull().first(until) }
    }

    private fun HomeViewModel.opened(open: HomeViewModel.((Long) -> Unit) -> Unit): Long = runBlocking {
        var id: Long? = null
        open { id = it }
        withTimeout(10_000) {
            while (id == null) delay(10)
            checkNotNull(id)
        }
    }

    @Test
    fun `today's word, a day from the archive and free play open their games`() {
        val model = viewModel()
        val keep = kotlinx.coroutines.GlobalScope.launch(Dispatchers.Unconfined) { model.state.collect {} }
        try {
            val first = model.await()
            assertThat(first.gameLanguage).isEqualTo(Language.ITALIAN)
            assertThat(first.number).isEqualTo(33)
            assertThat(first.gameLanguageToday.status).isEqualTo(DayStatus.NOT_PLAYED)

            val todayId = model.opened { openDaily(today, it) }
            assertThat(
                model.await {
                    it.gameLanguageToday.status == DayStatus.IN_PROGRESS
                }.gameLanguageToday.attemptsUsed,
            )
                .isEqualTo(0)

            val earlier = LocalDate.of(2026, 11, 20)
            val archiveId = model.opened { openDaily(earlier, it) }
            assertThat(archiveId).isNotEqualTo(todayId)
            val archived = runBlocking { games.load(archiveId)!! }
            assertThat((archived.game.setup.mode as GameMode.Daily).date).isEqualTo(earlier)
            assertThat(archived.startedOn).isEqualTo(today)

            model.setLevel(Level.ADVANCED)
            assertThat(model.await { it.level == Level.ADVANCED }.level).isEqualTo(Level.ADVANCED)
            val freeId = model.opened { openFree(it) }
            assertThat(runBlocking { games.load(freeId)!!.game.setup.mode }).isEqualTo(GameMode.Free(Level.ADVANCED))
            assertThat(model.await { it.freeUnderWay != null }.freeUnderWay)
                .isEqualTo(FreeUnderWay(Level.ADVANCED, attemptsUsed = 0, attempts = 6, hard = false))
        } finally {
            keep.cancel()
        }
    }

    @Test
    fun `the game language, Hard and the calendar's month are the player's`() {
        val model = viewModel()
        val keep = kotlinx.coroutines.GlobalScope.launch(Dispatchers.Unconfined) { model.state.collect {} }
        try {
            model.await()
            model.selectLanguage(Language.SPANISH)
            assertThat(
                model.await {
                    it.gameLanguage == Language.SPANISH
                }.month.month,
            ).isEqualTo(YearMonth.of(2026, 12))
            model.setHard(true)
            assertThat(model.await { it.hard }.hard).isTrue()
            model.showMonth(YearMonth.of(2026, 11))
            val november = model.await { it.month.month == YearMonth.of(2026, 11) }
            assertThat(november.month.hasNext).isTrue()
            assertThat(november.month.hasPrevious).isFalse()
            // Hard holds for the next game.
            val id = model.opened { openDaily(today, it) }
            assertThat(runBlocking { games.load(id)!!.game.setup.hard }).isTrue()
            assertThat(runBlocking { games.load(id)!!.game.setup.language }).isEqualTo(Language.SPANISH)
        } finally {
            keep.cancel()
        }
    }

    @Test
    fun `a new free word at another level - an untried word gives way, a tried one is left and shown`() {
        val model = viewModel()
        val keep = kotlinx.coroutines.GlobalScope.launch(Dispatchers.Unconfined) { model.state.collect {} }
        try {
            model.await()
            val untried = model.opened { openFree(it) }
            model.setLevel(Level.ADVANCED)
            model.await { it.level == Level.ADVANCED }
            // Never tried: the new word comes at once, at the level chosen.
            val fresh = model.opened { newFree(it) }
            assertThat(fresh).isNotEqualTo(untried)
            assertThat(runBlocking { games.load(untried) }).isNull()
            assertThat(runBlocking { games.load(fresh)!!.game.setup.mode }).isEqualTo(GameMode.Free(Level.ADVANCED))

            // Tried: left, lost, and its end card is what opens.
            val game = runBlocking { games.load(fresh)!!.game }
            val daily = lexicons.of(Language.ITALIAN).daily
            val guess = (1..daily.size).asSequence().map { daily.word(it) }.first { it != game.setup.solution }
            val accepted = game.submit(guess, lexicons.of(Language.ITALIAN).accepted) as Submission.Accepted
            runBlocking { games.save(fresh, accepted.game) }
            assertThat(model.await { it.freeUnderWay?.attemptsUsed == 1 }.freeUnderWay?.level).isEqualTo(Level.ADVANCED)
            val shown = model.opened { newFree(it) }
            assertThat(shown).isEqualTo(fresh)
            assertThat(runBlocking { games.load(shown)!!.game.state }).isEqualTo(GameState.LOST)
            assertThat(model.await { it.freeUnderWay == null }.freeUnderWay).isNull()
        } finally {
            keep.cancel()
        }
    }

    @Test
    fun `Hard says which of the page's words it holds for`() {
        val model = viewModel()
        val keep = kotlinx.coroutines.GlobalScope.launch(Dispatchers.Unconfined) { model.state.collect {} }
        try {
            assertThat(model.await().hardStanding).isEqualTo(HardStanding(today = false, free = false))
            // Today's word opened, not tried: the switch still decides it.
            val id = model.opened { openDaily(today, it) }
            model.setHard(true)
            assertThat(model.await { it.hard }.hardStanding).isEqualTo(HardStanding(today = true, free = true))
            // Taken up in Hard and tried, then the switch turned off: today's word keeps Hard, and says so.
            assertThat(model.opened { openDaily(today, it) }).isEqualTo(id)
            val game = runBlocking { games.load(id)!!.game }
            assertThat(game.setup.hard).isTrue()
            val daily = lexicons.of(Language.ITALIAN).daily
            val guess = (1..daily.size).asSequence().map { daily.word(it) }.first { it != game.setup.solution }
            val accepted = game.submit(guess, lexicons.of(Language.ITALIAN).accepted) as Submission.Accepted
            runBlocking { games.save(id, accepted.game) }
            model.setHard(false)
            assertThat(model.await { !it.hard && it.hardStanding.todayKept }.hardStanding)
                .isEqualTo(HardStanding(today = true, free = false, todayKept = true))
        } finally {
            keep.cancel()
        }
    }
}
