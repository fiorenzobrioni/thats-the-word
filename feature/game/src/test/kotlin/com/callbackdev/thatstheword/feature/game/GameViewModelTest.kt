package com.callbackdev.thatstheword.feature.game

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.NotebookRepository
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.game.GuidedGame
import com.callbackdev.thatstheword.core.domain.game.Lesson
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.domain.share.ShareText
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.meaning
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.HintKind
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

    private fun viewModel(
        id: Long,
        speaks: Language = Language.ENGLISH,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = GameViewModel(
        gameId = id,
        savedState = savedState,
        games = games(),
        lexicons = lexicons,
        settings = settings,
        interfaceLanguage = { speaks },
        notebook = NotebookRepository(database) { clock },
    )

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
        assertThat(text).startsWith("That’s The Word · #20 · English · 2/6")
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

    /** The id the end card's next game opened, once it is. */
    private fun GameViewModel.awaitNext(): Long {
        var opened: Long? = null
        next { opened = it }
        return runBlocking {
            withTimeout(10_000) {
                while (opened == null) kotlinx.coroutines.delay(10)
                checkNotNull(opened)
            }
        }
    }

    @Test
    fun `a finished daily is played again as practice, not counted and not shared`() {
        val id = runBlocking { games().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false) }
        val solution = lexicons.of(Language.ENGLISH).daily.word(DailyCalendar(clock).number(today))
        val first = viewModel(id)
        assertThat(first.awaitState().next).isEqualTo(NextGame.PRACTICE)
        first.type(solution)
        first.submit()
        first.awaitState { it.finished }
        settle(id, 1)
        assertThat(first.awaitState().shareable).isTrue()

        val replay = first.awaitNext()
        assertThat(replay).isNotEqualTo(id)
        val practice = viewModel(replay)
        val state = practice.awaitState()
        assertThat(state.practice).isTrue()
        assertThat(state.shareable).isFalse()
        assertThat(state.finished).isFalse()
        practice.type(solution)
        practice.submit()
        practice.awaitState { it.finished }
        assertThat(
            practice.shareText(
                ShareText.Labels("That’s The Word", "English", "Free play", "Hard") {
                    "$it hints"
                },
            ),
        ).isNull()
    }

    @Test
    fun `a finished free game offers another word at the language's level`() {
        val id = runBlocking {
            games().freeGame(
                Language.SPANISH,
                com.callbackdev.thatstheword.core.model.Level.BASE,
                Language.ENGLISH,
                hard = false,
            )
        }
        val model = viewModel(id)
        val solution = runBlocking { games().load(id)!!.game.setup.solution }
        assertThat(model.awaitState().next).isEqualTo(NextGame.ANOTHER_WORD)
        // Nothing next while the game is played.
        var early: Long? = null
        model.next { early = it }
        assertThat(early).isNull()
        model.type(solution)
        model.submit()
        model.awaitState { it.finished }
        settle(id, 1)
        val another = model.awaitNext()
        assertThat(another).isNotEqualTo(id)
        assertThat(runBlocking { games().load(another)!!.game.setup.solution }).isNotEqualTo(solution)
    }

    @Test
    fun `a hint spends an attempt, is drawn in the last row and is kept with the game`() {
        // An Italian word for a player whose interface speaks English: the hints are there.
        val id = runBlocking { games().dailyGame(Language.ITALIAN, today, Language.ENGLISH, hard = false) }
        val model = viewModel(id)
        val start = model.awaitState()
        assertThat(start.hintsAvailable).isTrue()
        assertThat(start.hintChoices.first { it.kind == HintKind.LETTER }.status).isEqualTo(HintStatus.OFFERED)

        model.takeHint(HintKind.LETTER)
        val hinted = model.awaitState { it.takenHints.isNotEmpty() }
        val letter = hinted.takenHints.single()
        assertThat(letter.position).isEqualTo(0)
        assertThat(letter.letter).isEqualTo(hinted.solution.first())
        assertThat(hinted.spentOnHints).isEqualTo(1)
        assertThat(hinted.hintChoices.first { it.kind == HintKind.LETTER }.status).isEqualTo(HintStatus.TAKEN)
        // Taken twice: nothing more is spent.
        model.takeHint(HintKind.LETTER)
        assertThat(model.awaitState().spentOnHints).isEqualTo(1)
        settle(id, 1)
        assertThat(viewModel(id).awaitState().takenHints.single().letter).isEqualTo(letter.letter)
    }

    @Test
    fun `a game in the interface's own language has no hints`() {
        val id = runBlocking { games().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false) }
        val model = viewModel(id)
        assertThat(model.awaitState().hintsAvailable).isFalse()
        model.takeHint(HintKind.TRANSLATION)
        assertThat(model.awaitState().spentOnHints).isEqualTo(0)
    }

    @Test
    fun `the end explains the words, keeps the solution and adds a word tried by hand`() {
        val id = runBlocking { games().dailyGame(Language.SPANISH, today, Language.ENGLISH, hard = false) }
        val daily = lexicons.of(Language.SPANISH).daily
        val solution = daily.word(DailyCalendar(clock).number(today))
        val other = (1..daily.size).asSequence().map { daily.word(it) }.first { it != solution }
        val model = viewModel(id)
        model.awaitState()
        model.type(other)
        model.submit()
        model.awaitState { it.rows.size == 1 }
        model.type(solution)
        model.submit()
        val end = model.awaitState { it.finished && it.solutionKept }
        assertThat(end.solutionMeaning).isEqualTo(
            MeaningUi.of(lexicons.of(Language.SPANISH).meaning(solution, Language.ENGLISH)),
        )
        assertThat(end.sameLanguage).isFalse()
        assertThat(end.tried).hasSize(1)
        assertThat(end.tried.single().inNotebook).isFalse()
        // A first win in a foreign language, said once on the card.
        assertThat(model.awaitState { it.milestones.isNotEmpty() }.milestones)
            .containsExactly(com.callbackdev.thatstheword.core.model.Milestone.FIRST_FOREIGN_WIN)

        model.toggleNotebook(0)
        val added = model.awaitState { it.tried.single().inNotebook }.tried.single()
        assertThat(added.removable).isTrue()
        model.toggleNotebook(0)
        assertThat(model.awaitState { !it.tried.single().inNotebook }.tried.single().inNotebook).isFalse()
    }

    private fun word(language: Language, key: String) = Alphabet.of(language).parse(key)!!

    @Test
    fun `a free word left is lost, and its end card says the milestone it reached`() {
        val id = runBlocking {
            games().freeGame(
                Language.ENGLISH,
                com.callbackdev.thatstheword.core.model.Level.BASE,
                Language.ENGLISH,
                false,
            )
        }
        val solution = runBlocking { games().load(id)!!.game.setup.solution }
        val daily = lexicons.of(Language.ENGLISH).daily
        val others = (1..daily.size).asSequence().map { daily.word(it) }.filter { it != solution }.toList()
        // 99 words in the notebook: the word left is the hundredth.
        val notebook = NotebookRepository(database) { clock }
        runBlocking { others.drop(1).take(99).forEach { notebook.save(Language.ENGLISH, it) } }
        val model = viewModel(id)
        model.awaitState()
        // Nothing to leave before a word is tried.
        model.leave()
        assertThat(model.awaitState().finished).isFalse()
        model.type(others.first())
        model.submit()
        model.awaitState { it.rows.size == 1 }
        settle(id, 1)
        model.leave()
        val end = model.awaitState { it.finished && it.milestones.isNotEmpty() }
        assertThat(end.state).isEqualTo(GameState.LOST)
        assertThat(end.conceded).isTrue()
        assertThat(end.attemptsUsed).isEqualTo(1)
        assertThat(end.milestones).containsExactly(com.callbackdev.thatstheword.core.model.Milestone.HUNDRED_WORDS)
        assertThat(end.next).isEqualTo(NextGame.ANOTHER_WORD)
        // Saved left: taken up again, it is over.
        val again = viewModel(id).awaitState { it.finished }
        assertThat(again.conceded).isTrue()
    }

    @Test
    fun `a daily is never left`() {
        val id = runBlocking { games().dailyGame(Language.ENGLISH, today, Language.ENGLISH, hard = false) }
        val daily = lexicons.of(Language.ENGLISH).daily
        val solution = daily.word(DailyCalendar(clock).number(today))
        val model = viewModel(id)
        model.awaitState()
        model.type((1..daily.size).asSequence().map { daily.word(it) }.first { it != solution })
        model.submit()
        model.awaitState { it.rows.size == 1 }
        model.leave()
        assertThat(model.awaitState().finished).isFalse()
    }

    @Test
    fun `the guided game says each rule once, the first time it happens, and ends with the game`() {
        val id = runBlocking { games().guidedGame(Language.ENGLISH, Language.ENGLISH) }
        val handle = SavedStateHandle()
        val model = viewModel(id, savedState = handle)
        assertThat(model.awaitState { it.lessons.isNotEmpty() }.lessons).containsExactly(Lesson.FIRST_GUESS)

        // Too short, then not a word: each refusal teaches its rule once.
        model.type(word(Language.ENGLISH, "hea"))
        model.submit()
        assertThat(model.awaitState { Lesson.FIVE_LETTERS in it.lessons }.lessons).containsExactly(Lesson.FIVE_LETTERS)
        model.delete()
        model.delete()
        model.delete()
        model.type(word(Language.ENGLISH, "qqqqq"))
        model.submit()
        assertThat(model.awaitState { Lesson.ONLY_WORDS in it.lessons }.lessons).containsExactly(Lesson.ONLY_WORDS)
        repeat(5) { model.delete() }

        // EARTH against HEART: every letter elsewhere.
        model.type(word(Language.ENGLISH, "earth"))
        model.submit()
        assertThat(model.awaitState { it.rows.size == 1 }.lessons).containsExactly(Lesson.ELSEWHERE)
        settle(id, 1)

        // The page closed and opened again: what was said is not said again.
        val again = viewModel(id, savedState = SavedStateHandle())
        assertThat(again.awaitState().lessons).isEmpty()
        again.type(word(Language.ENGLISH, "board"))
        again.submit()
        assertThat(again.awaitState { it.rows.size == 2 }.lessons)
            .containsExactly(Lesson.RIGHT_PLACE, Lesson.ABSENT).inOrder()

        again.type(GuidedGame.solution(Language.ENGLISH))
        again.submit()
        val end = again.awaitState { it.finished }
        assertThat(end.lessons).isEmpty()
        assertThat(end.next).isEqualTo(NextGame.START_PLAYING)
        assertThat(end.shareable).isFalse()
        var done = false
        again.next(onGuidedDone = { done = true }) { error("the guided game opens no other game") }
        assertThat(done).isTrue()
    }
}
