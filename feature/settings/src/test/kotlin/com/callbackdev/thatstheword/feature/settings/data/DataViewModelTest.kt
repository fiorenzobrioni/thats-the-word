package com.callbackdev.thatstheword.feature.settings.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.BackupRepository
import com.callbackdev.thatstheword.core.data.DataContents
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.backup.BackupCodec
import com.callbackdev.thatstheword.core.domain.backup.BackupRead
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Your data on real files (PLANNING.md §5, §11 Phase 6): a copy saved, read back on another phone,
 * shown before it is written, restored on the yes; a file that is no copy, or one changed by hand,
 * refused with nothing changed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DataViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val rome = ZoneId.of("Europe/Rome")
    private val clock: Clock = Clock.fixed(ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, rome).toInstant(), rome)
    private val today = LocalDate.of(2026, 11, 20)
    private val lexicons = Lexicons(AssetWordFiles(context.assets))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val databases = mutableListOf<ThatsTheWordDatabase>()

    private inner class Phone(name: String) {
        val database = Room.inMemoryDatabaseBuilder(context, ThatsTheWordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { databases += it }
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("$name.preferences_pb") },
        ) { Language.ENGLISH }
        val games = GameRepository(database, lexicons) { clock }
        val backups = BackupRepository(database, settings, lexicons) { clock }
        val viewModel = DataViewModel(context, backups)

        init {
            // The page collects its state; so does the test.
            scope.launch(Dispatchers.Main) { viewModel.state.collect {} }
        }
    }

    @Before
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun close() {
        scope.cancel()
        databases.forEach { it.close() }
        Dispatchers.resetMain()
    }

    /** Today's Italian daily, found at the first guess. */
    private suspend fun Phone.win() {
        val id = games.dailyGame(Language.ITALIAN, today, Language.ENGLISH, hard = false)
        val solution = lexicons.of(Language.ITALIAN).daily.word(DailyCalendar(clock).number(today))
        val game = games.load(id)!!.game.submit(solution, lexicons.of(Language.ITALIAN).accepted) as Submission.Accepted
        games.save(id, game.game)
    }

    /** Waits for the page's task to be done, and returns what it came to. */
    private suspend fun Phone.settled(): DataUiState = withTimeout(10_000) {
        delay(50)
        while (viewModel.state.value.task != null) delay(20)
        viewModel.state.value
    }

    @Test
    fun `a copy saved, read on another phone, shown, then brought back on the yes`() = runBlocking {
        val old = Phone("old").apply { win() }
        val file = folder.newFile("copy.json")
        old.viewModel.save(Uri.fromFile(file))
        val saved = old.settled().outcome as DataOutcome.Saved
        // An Italian word found with the interface in English: a first win in another language.
        assertThat(saved.contents).isEqualTo(DataContents(games = 1, words = 1, milestones = 1))
        assertThat(BackupCodec.decode(file.readText())).isInstanceOf(BackupRead.Ok::class.java)

        val new = Phone("new")
        new.viewModel.read(Uri.fromFile(file))
        val preview = checkNotNull(new.settled().preview)
        assertThat(preview.contents).isEqualTo(saved.contents)
        assertThat(preview.savedOn).isEqualTo(today)
        // Nothing is written before the yes.
        assertThat(new.backups.contents.first().isEmpty).isTrue()

        new.viewModel.confirmRestore()
        assertThat(new.settled().outcome).isEqualTo(DataOutcome.Restored(saved.contents))
        assertThat(new.backups.contents.first()).isEqualTo(saved.contents)
    }

    @Test
    fun `a copy put away is not brought back`() = runBlocking {
        val old = Phone("old").apply { win() }
        val file = folder.newFile("copy.json")
        old.viewModel.save(Uri.fromFile(file))
        old.settled()
        val new = Phone("new")
        new.viewModel.read(Uri.fromFile(file))
        new.settled()
        new.viewModel.cancelRestore()
        assertThat(new.viewModel.state.value.preview).isNull()
        assertThat(new.backups.contents.first().isEmpty).isTrue()
    }

    @Test
    fun `a file that is no copy is refused`() = runBlocking {
        val phone = Phone("phone")
        val file = folder.newFile("notes.txt").apply { writeText("Buy bread.") }
        phone.viewModel.read(Uri.fromFile(file))
        assertThat(phone.settled().outcome).isEqualTo(DataOutcome.Failed(DataFailure.NOT_A_BACKUP))
    }

    @Test
    fun `a copy whose daily is not the day's word is damaged, and nothing changes`() = runBlocking {
        val old = Phone("old").apply { win() }
        val file = folder.newFile("copy.json")
        old.viewModel.save(Uri.fromFile(file))
        old.settled()
        val solution = Regex("\"solution\": \"(\\w+)\"").find(file.readText())!!.groupValues[1]
        // Another word that keeps the copy whole: the same letters turned round.
        file.writeText(
            file.readText().replace("\"solution\": \"$solution\"", "\"solution\": \"${solution.reversed()}\""),
        )
        val new = Phone("new")
        new.viewModel.read(Uri.fromFile(file))
        val outcome = new.settled().outcome
        // Reversed, the guess no longer finds it: the game replays into another result, or its day's word is not it.
        assertThat(outcome).isEqualTo(DataOutcome.Failed(DataFailure.DAMAGED))
        assertThat(new.backups.contents.first().isEmpty).isTrue()
    }

    @Test
    fun `a file that cannot be opened says so`() = runBlocking {
        val phone = Phone("phone")
        phone.viewModel.read(Uri.fromFile(File(folder.root, "gone.json")))
        assertThat(phone.settled().outcome).isEqualTo(DataOutcome.Failed(DataFailure.READ))
    }
}
