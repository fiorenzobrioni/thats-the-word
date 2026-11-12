package com.callbackdev.thatstheword.feature.game

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.domain.share.ShareText
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Word
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Phase 3's acceptance, in the JVM (PLANNING.md §11): a daily played from start to end, closed in
 * the middle and taken up again, through the page's view model and the real words.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GameViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val rome = ZoneId.of("Europe/Rome")
    private val clock = Clock.fixed(ZonedDateTime.of(2026, 11, 20, 9, 0, 0, 0, rome).toInstant(), rome)
    private val today = LocalDate.of(2026, 11, 20)
    private val lexicons = Lexicons(AssetWordFiles(context.assets))
    private lateinit var database: ThatsTheWordDatabase
    private lateinit var settings: SettingsRepository

    @Before
    fun open() {
        // The model's flows run where they are fed, so each call's state is there when it returns.
        Dispatchers.setMain(Dispatchers.Unconfined)
        database =
            Room.inMemoryDatabaseBuilder(context, ThatsTheWordDatabase::class.java).allowMainThreadQueries().build()
        settings =
            SettingsRepository(
                PreferenceDataStoreFactory.create {
                    folder.root.resolve("settings.preferences_pb")
                },
            ) { Language.ENGLISH }
    }

    @After
    fun close() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun games() = GameRepository(database, lexicons) { clock }

    private fun viewModel(id: Long) = GameViewModel(id, SavedStateHandle(), games(), lexicons, settings)

    /** The model's state once it is loaded and [until] holds: its flows are fed from Room's threads too. */
    private fun GameViewModel.awaitState(until: (GameUiState) -> Boolean = { true }) =
        runBlocking { withTimeout(10_000) { state.filterNotNull().first(until) } }

    private fun GameViewModel.type(word: Word) = word.letters.forEach(::type)

    /** Waits for the saves the view model launched: the next model reads what they wrote. */
    private fun settle(id: Long, moves: Int) = runBlocking {
        repeat(1_000) {
            if ((games().load(id)?.game?.moves?.size ?: 0) >= moves) return@runBlocking
            Thread.sleep(10)
        }
        error("the game's moves were not saved")
    }

    @Test
    fun `a daily played from start to end, closed in the middle and taken up again`() {
        val id = runBlocking { games().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false) }
        val daily = lexicons.of(Language.ENGLISH).daily
        val solution = daily.word(DailyCalendar(clock).number(today))
        val other = (1..daily.size).asSequence().map { daily.word(it) }.first { it != solution }

        val first = viewModel(id)
        first.awaitState()
        first.type(other)
        first.delete()
        first.submit()
        assertThat(first.awaitState { it.refusal != null }.refusal?.reason).isEqualTo(Refusal.TooShort)
        first.type(other.letters.last())
        first.submit()
        assertThat(first.awaitState { it.rows.isNotEmpty() }.rows).hasSize(1)
        assertThat(first.awaitState().refusal).isNull()
        settle(id, 1)

        // Closed in the middle: a new page, a new model, the same game.
        val second = viewModel(id)
        val taken = second.awaitState()
        assertThat(taken.rows).hasSize(1)
        assertThat(
            taken.rows.single().letters.joinToString(""),
        ).isEqualTo(Alphabet.of(Language.ENGLISH).keyForm(other).uppercase())
        assertThat(taken.state).isEqualTo(GameState.PLAYING)

        second.type(solution)
        second.submit()
        assertThat(second.awaitState { it.finished }.state).isEqualTo(GameState.WON)
        settle(id, 2)
        assertThat(viewModel(id).awaitState().state).isEqualTo(GameState.WON)

        val text = second.shareText(ShareText.Labels("That’s The Word", "English", "Free play", "Hard") { "$it hints" })
        assertThat(text).startsWith("That’s The Word · #9 · English · 2/6")
        assertThat(text).doesNotContain(Alphabet.of(Language.ENGLISH).keyForm(solution).uppercase())
    }

    @Test
    fun `a hardware keyboard types the game language's letters only`() {
        val id = runBlocking { games().dailyGame(Language.SPANISH, today, Language.ENGLISH, hard = false) }
        val model = viewModel(id)
        model.awaitState()
        assertThat(model.typeChar('ñ')).isTrue()
        assertThat(model.typeChar('Ñ')).isTrue()
        assertThat(model.typeChar('1')).isFalse()
        assertThat(model.awaitState { it.typed.size == 2 }.typed).containsExactly('Ñ', 'Ñ').inOrder()
    }
}
