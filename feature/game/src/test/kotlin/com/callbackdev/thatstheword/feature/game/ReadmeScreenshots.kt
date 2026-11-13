package com.callbackdev.thatstheword.feature.game

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.GuidedGame
import com.callbackdev.thatstheword.core.domain.game.Lesson
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.domain.game.WordList
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.NotebookEntry
import com.callbackdev.thatstheword.core.model.Word
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * The README's pictures of the game (docs/screenshots), drawn by the game's own page from sample
 * games on Friday 13 November 2026, daily number 13, in English. Run with
 * `./gradlew :feature:game:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("thatstheword.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val files = AssetWordFiles(context.assets)
    private val anyWord = WordList { true }
    private val today = LocalDate.of(2026, 11, 13)

    private fun game(
        language: Language,
        solution: String,
        vararg guesses: String,
        ui: Language = Language.ENGLISH,
        mode: GameMode = GameMode.Daily(today, 13),
    ): Game {
        val alphabet = Alphabet.of(language)
        val word = alphabet.parse(solution)!!
        var game = Game.start(
            GameSetup(
                language = language,
                mode = mode,
                solution = word,
                interfaceLanguage = ui,
                hintKinds = Lexicon(files, language).hintKinds(word, ui),
            ),
        )
        for (guess in guesses) game = (game.submit(alphabet.parse(guess)!!, anyWord) as Submission.Accepted).game
        return game
    }

    private fun state(
        game: Game,
        typed: String = "",
        notebook: Map<Word, NotebookEntry> = emptyMap(),
        milestones: List<Milestone> = emptyList(),
        lessons: List<Lesson> = emptyList(),
    ): GameUiState {
        val alphabet = Alphabet.of(game.setup.language)
        return GameUiState.of(
            game = game,
            typed = typed.map { alphabet.letterOf(it)!! },
            refusal = null,
            lexicon = Lexicon(files, game.setup.language),
            practice = false,
            notebook = notebook,
            milestones = milestones,
            lessons = lessons,
        )
    }

    /** Today's English word, two rows in, the third being typed: the keys keep what was found. */
    @Test
    fun game() {
        show(state(game(Language.ENGLISH, "brave", "great", "trace"), typed = "bra"))
        save("game")
    }

    /**
     * Today's Italian word found from an English phone: the line the name stands for, the score,
     * every word explained in English, the solution kept in the notebook, a milestone said.
     */
    @Test
    fun wordFound() {
        val alphabet = Alphabet.of(Language.ITALIAN)
        val citta = alphabet.parse("citta")!!
        val done = state(
            game(Language.ITALIAN, "citta", "torta", "pasta", "citta"),
            notebook = mapOf(citta to entry(citta)),
            milestones = listOf(Milestone.FIRST_FOREIGN_WIN),
        )
        show(done)
        save("word-found")
    }

    /** A Spanish word from an English phone, in the dark: one hint taken, the others offered. */
    @Test
    fun hints() {
        val game = game(Language.SPANISH, "arbol", "nubes", "carta").takeHint(HintKind.TRANSLATION)
        show(state(game), dark = true, hintsOpen = true)
        save("hints")
    }

    /** The guided first game: the first row's three marks, each said beside its tile. */
    @Test
    fun guided() {
        val solution = GuidedGame.solution(Language.ENGLISH)
        var game = Game.start(
            GameSetup(
                language = Language.ENGLISH,
                mode = GameMode.Guided,
                solution = solution,
                interfaceLanguage = Language.ENGLISH,
                hintKinds = emptySet(),
            ),
        )
        game = (game.submit(Alphabet.of(Language.ENGLISH).parse("react")!!, anyWord) as Submission.Accepted).game
        show(state(game, lessons = listOf(Lesson.RIGHT_PLACE, Lesson.ELSEWHERE, Lesson.ABSENT)))
        save("guided")
    }

    private fun entry(word: Word) = NotebookEntry(
        language = Language.ITALIAN,
        word = word,
        firstMet = today,
        lastMet = today,
        timesMet = 1,
        solved = 1,
        missed = 0,
        withHints = 0,
        savedByHand = false,
    )

    private fun show(state: GameUiState, dark: Boolean = false, hintsOpen: Boolean = false) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                GameScreen(
                    state = state,
                    onBack = {},
                    onLetter = {},
                    onDelete = {},
                    onEnter = {},
                    onShare = {},
                    hintsOpenAtStart = hintsOpen,
                )
            }
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
