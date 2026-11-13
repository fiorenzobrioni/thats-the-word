package com.callbackdev.thatstheword.feature.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.GuidedGame
import com.callbackdev.thatstheword.core.domain.game.Lesson
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.domain.game.WordList
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.NotebookEntry
import com.callbackdev.thatstheword.core.model.Word
import com.callbackdev.thatstheword.core.testing.assertAccessible
import com.callbackdev.thatstheword.core.testing.walkPage
import com.callbackdev.thatstheword.core.testing.writeScreenshot
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * The game page drawn from its states (PLANNING.md §12): a game being played, a word refused, a
 * word found, a word not found; in the three languages, dark, on a small phone, at twice the text
 * size and on an open foldable; each checked for TalkBack's labels and a finger's targets.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class GameScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val files = AssetWordFiles(context.assets)
    private val anyWord = WordList { true }

    private fun game(
        language: Language,
        solution: String,
        vararg guesses: String,
        hard: Boolean = false,
        ui: Language = language,
    ): Game {
        val alphabet = Alphabet.of(language)
        var game = Game.start(
            GameSetup(
                language = language,
                mode = GameMode.Daily(LocalDate.of(2026, 11, 20), 9),
                solution = alphabet.parse(solution)!!,
                hard = hard,
                interfaceLanguage = ui,
                hintKinds = Lexicon(files, language).hintKinds(alphabet.parse(solution)!!, ui),
            ),
        )
        for (guess in guesses) game = (game.submit(alphabet.parse(guess)!!, anyWord) as Submission.Accepted).game
        return game
    }

    /** The guided first game in [language], played to [guesses], for a player speaking [ui]. */
    private fun guided(language: Language, vararg guesses: String, ui: Language = language): Game {
        val alphabet = Alphabet.of(language)
        val solution = GuidedGame.solution(language)
        var game = Game.start(
            GameSetup(
                language = language,
                mode = GameMode.Guided,
                solution = solution,
                interfaceLanguage = ui,
                hintKinds = Lexicon(files, language).hintKinds(solution, ui),
            ),
        )
        for (guess in guesses) game = (game.submit(alphabet.parse(guess)!!, anyWord) as Submission.Accepted).game
        return game
    }

    private fun state(
        game: Game,
        typed: String = "",
        refusal: Refusal? = null,
        letter: Char? = null,
        practice: Boolean = false,
        notebook: Map<Word, NotebookEntry> = emptyMap(),
        milestones: List<Milestone> = emptyList(),
        lessons: List<Lesson> = emptyList(),
    ): GameUiState {
        val alphabet = Alphabet.of(game.setup.language)
        return GameUiState.of(
            game = game,
            typed = typed.map { alphabet.letterOf(it)!! },
            refusal = refusal?.let { RefusalUi(it, letter, 1) },
            lexicon = Lexicon(files, game.setup.language),
            practice = practice,
            notebook = notebook,
            milestones = milestones,
            lessons = lessons,
        )
    }

    private val typedLetters = mutableListOf<Letter>()
    private var entered = 0

    private val hintsTaken = mutableListOf<HintKind>()
    private val toggled = mutableListOf<Int>()

    private fun draw(state: GameUiState, dark: Boolean = false, hintsOpen: Boolean = false) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                GameScreen(
                    state = state,
                    onBack = {},
                    onLetter = { typedLetters += it },
                    onDelete = {},
                    onEnter = { entered++ },
                    onShare = {},
                    onTakeHint = { hintsTaken += it },
                    onToggleNotebook = { toggled += it },
                    hintsOpenAtStart = hintsOpen,
                )
            }
        }
        compose.mainClock.advanceTimeBy(3_000)
    }

    @Test
    fun `a game being played reads row by row, and its keys type`() {
        draw(state(game(Language.ENGLISH, "train", "steam"), typed = "pl"))
        compose.onNodeWithContentDescription(
            "Row 1: S, absent; T, elsewhere; E, absent; A, elsewhere; M, absent",
        ).assertIsDisplayed()
        compose.onNodeWithContentDescription("Row 2 of 6, typing: P L").assertIsDisplayed()
        // The marks' key under the grid, while the game is played.
        compose.onNodeWithText("Right place").assertIsDisplayed()
        compose.onNodeWithText("Elsewhere").assertIsDisplayed()
        compose.onNodeWithText("Not in the word").assertIsDisplayed()
        compose.onNodeWithContentDescription("T, elsewhere").assertIsDisplayed()
        compose.onNodeWithContentDescription("S, absent").assertIsDisplayed()
        compose.onNodeWithContentDescription("Q").performClick()
        compose.onNodeWithContentDescription("Enter").performClick()
        assertThat(typedLetters.map { it.index }).containsExactly(16)
        assertThat(entered).isEqualTo(1)
        compose.assertAccessible()
        compose.writeScreenshot("game-playing")
    }

    @Test
    fun `a word refused is said above the keyboard`() {
        draw(state(game(Language.ENGLISH, "train", "steam"), typed = "qwxyz", refusal = Refusal.NotAWord))
        compose.onNodeWithText("Not in the list.").assertIsDisplayed()
        compose.writeScreenshot("game-refused")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `Hard's refusal names the position, in Italian`() {
        val game = game(Language.ITALIAN, "pasta", "salto", hard = true)
        draw(state(game, typed = "torta", refusal = Refusal.MustBeAt(1, Letter(0)), letter = 'A'))
        compose.onNodeWithText("La seconda lettera dev’essere A.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Italiano · #9 · difficile", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Al posto giusto").assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "Riga 1: S, altrove; A, al posto giusto; L, assente; T, al posto giusto; O, assente",
        )
            .assertIsDisplayed()
        compose.writeScreenshot("game-hard-it")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `a word found says its name and is written with its accents`() {
        draw(state(game(Language.ITALIAN, "citta", "torta", "citta")))
        compose.onNodeWithText("Ecco la parola.").assertExists()
        compose.onNodeWithText("città").assertExists()
        compose.onNodeWithText("Trovata in 2 tentativi su 6.").assertExists()
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-won-it")
    }

    @Test
    fun `a day replayed as practice says so, offers it again and shares nothing`() {
        draw(state(game(Language.ENGLISH, "train", "steam", "train"), practice = true))
        compose.onNodeWithContentDescription("English · #9 · practice", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Practice: the day keeps its first result.").assertExists()
        compose.onNodeWithText("Play it again, as practice").assertExists()
        compose.onNodeWithText("Share the result").assertDoesNotExist()
        compose.onNodeWithText("Right place").assertDoesNotExist()
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-practice")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `a free word found offers another, in Spanish`() {
        val alphabet = Alphabet.of(Language.SPANISH)
        var game = Game.start(
            GameSetup(
                language = Language.SPANISH,
                mode = GameMode.Free(com.callbackdev.thatstheword.core.model.Level.BASE),
                solution = alphabet.parse("arbol")!!,
                interfaceLanguage = Language.SPANISH,
            ),
        )
        game = (game.submit(alphabet.parse("arbol")!!, anyWord) as Submission.Accepted).game
        draw(state(game))
        compose.onNodeWithText("Otra palabra").assertExists()
        compose.onNodeWithText("Compartir el resultado").assertExists()
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-free-es")
    }

    @Test
    fun `a word not found shows it`() {
        draw(state(game(Language.ENGLISH, "train", "steam", "plant", "brain", "grain", "drain", "crane")))
        compose.onNodeWithText("Not this time.").assertExists()
        compose.onNodeWithText("All 6 attempts used.").assertExists()
        compose.onNodeWithText("train").assertExists()
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-lost")
    }

    @Test
    fun `a free word left for another shows it, and offers another`() {
        val alphabet = Alphabet.of(Language.ENGLISH)
        var game = Game.start(
            GameSetup(
                language = Language.ENGLISH,
                mode = GameMode.Free(com.callbackdev.thatstheword.core.model.Level.ADVANCED),
                solution = alphabet.parse("train")!!,
                interfaceLanguage = Language.ENGLISH,
            ),
        )
        game = (game.submit(alphabet.parse("steam")!!, anyWord) as Submission.Accepted).game
        game = (game.submit(alphabet.parse("plant")!!, anyWord) as Submission.Accepted).game
        draw(state(game.concede()))
        compose.onNodeWithText("Not this time.").assertExists()
        compose.onNodeWithText("Left after 2 attempts of 6.").assertExists()
        compose.onNodeWithText("Another word").assertExists()
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-left")
    }

    private fun freeUnderWay(language: Language, solution: String, vararg guesses: String): Game {
        val alphabet = Alphabet.of(language)
        return guesses.fold(
            Game.start(
                GameSetup(
                    language = language,
                    mode = GameMode.Free(com.callbackdev.thatstheword.core.model.Level.ADVANCED),
                    solution = alphabet.parse(solution)!!,
                    interfaceLanguage = language,
                ),
            ),
        ) { game, guess -> (game.submit(alphabet.parse(guess)!!, anyWord) as Submission.Accepted).game }
    }

    private fun drawAsking(game: Game, onLeave: () -> Unit, onKeep: () -> Unit) {
        compose.setContent {
            ThatsTheWordTheme {
                GameScreen(
                    state = state(game),
                    onBack = {},
                    onLetter = {},
                    onDelete = {},
                    onEnter = {},
                    onShare = {},
                    askToLeave = true,
                    onLeave = onLeave,
                    onKeep = onKeep,
                )
            }
        }
        compose.mainClock.advanceTimeBy(3_000)
    }

    @Test
    fun `a free word tried is asked about over its rows, and kept or left`() {
        var left = 0
        var kept = 0
        drawAsking(freeUnderWay(Language.ENGLISH, "train", "steam", "plant"), { left++ }, { kept++ })
        compose.onNodeWithTag(GameTags.LEAVE_DIALOG).assertIsDisplayed()
        compose.onNodeWithText("Leave this word?").assertIsDisplayed()
        compose.writeScreenshot("game-leave")
        compose.onNodeWithTag(GameTags.LEAVE_NO).performClick()
        assertThat(kept).isEqualTo(1)
        compose.onNodeWithTag(GameTags.LEAVE_YES).performClick()
        assertThat(left).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the question in Italian`() {
        drawAsking(freeUnderWay(Language.ITALIAN, "cuore", "carte", "porte"), {}, {})
        compose.onNodeWithText("Lasciare questa parola?").assertIsDisplayed()
        compose.writeScreenshot("game-leave-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `the question in Spanish`() {
        drawAsking(freeUnderWay(Language.SPANISH, "arbol", "nubes", "carta"), {}, {})
        compose.onNodeWithText("¿Dejar esta palabra?").assertIsDisplayed()
        compose.writeScreenshot("game-leave-es")
    }

    @Test
    fun `a word that ended meanwhile is not asked about`() {
        drawAsking(freeUnderWay(Language.ENGLISH, "train", "steam", "train"), {}, {})
        compose.onNodeWithTag(GameTags.LEAVE_DIALOG).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `the Spanish keyboard has its Ñ`() {
        draw(state(game(Language.SPANISH, "arbol", "nubes")))
        compose.onNodeWithContentDescription("Ñ").assertIsDisplayed()
        compose.onNodeWithContentDescription("Enviar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Fila 1: N, no está; U, no está; B, en su sitio; E, no está; S, no está")
            .assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("game-es")
    }

    @Test
    fun `dark`() {
        draw(state(game(Language.ENGLISH, "train", "steam", "brain"), typed = "t"), dark = true)
        compose.assertAccessible()
        compose.writeScreenshot("game-dark")
    }

    @Test
    fun `a word found while the page is open is played, then the card rises`() {
        val playing = game(Language.ENGLISH, "train", "steam")
        var state by mutableStateOf(state(playing, typed = "train"))
        compose.setContent {
            ThatsTheWordTheme {
                GameScreen(state = state, onBack = {}, onLetter = {}, onDelete = {}, onEnter = {}, onShare = {})
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithContentDescription("Enter").assertIsDisplayed()
        val won = (playing.submit(Alphabet.of(Language.ENGLISH).parse("train")!!, anyWord) as Submission.Accepted).game
        compose.mainClock.autoAdvance = false
        state = state(won)
        compose.mainClock.advanceTimeBy(300)
        compose.writeScreenshot("game-revealing")
        compose.mainClock.autoAdvance = true
        // The row's sentence for TalkBack, and the same sentence announced once the reveal ends.
        compose.onAllNodesWithContentDescription(
            "Row 2: T, right place; R, right place; A, right place; I, right place; N, right place",
        ).assertCountEquals(2)
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithTag(GameTags.END_CARD).assertExists()
        compose.onNodeWithContentDescription("Enter").assertDoesNotExist()
        compose.writeScreenshot("game-found-here")
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h640dp-xhdpi")
    fun `on a small phone`() {
        draw(state(game(Language.ENGLISH, "train", "steam", "brain"), typed = "gr"))
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-small")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw(state(game(Language.ENGLISH, "train", "steam", "brain"), typed = "gr", refusal = Refusal.TooShort))
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the end at twice the text size`() {
        draw(state(game(Language.ENGLISH, "train", "steam", "train")))
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-won-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw(state(game(Language.ENGLISH, "train", "steam", "brain"), typed = "gr"))
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-foldable")
    }

    /** An Italian word, played with the interface in English: the hints are there. */
    private fun foreign(vararg guesses: String) = game(Language.ITALIAN, "citta", *guesses, ui = Language.ENGLISH)

    @Test
    fun `the hints open in the keyboard's place, their cost said, and a touch takes one`() {
        draw(state(foreign("torta")))
        compose.onNodeWithContentDescription("Hints").assertIsDisplayed().performClick()
        compose.onNodeWithTag(GameTags.HINTS_PANEL).assertIsDisplayed()
        compose.onNodeWithText("Each costs an attempt: a row of the grid is spent.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Q").assertDoesNotExist()
        compose.assertAccessible()
        compose.writeScreenshot("game-hints")
        compose.onNodeWithTag(GameTags.hint(HintKind.LETTER)).performClick()
        assertThat(hintsTaken).containsExactly(HintKind.LETTER)
        // The keyboard is back, the hint taken.
        compose.onNodeWithContentDescription("Q").assertIsDisplayed()
    }

    @Test
    fun `a game in the interface's own language has no hints`() {
        draw(state(game(Language.ENGLISH, "train", "steam")))
        compose.onNodeWithContentDescription("Hints").assertDoesNotExist()
    }

    @Test
    fun `the hints taken are drawn in the rows they spent, the last first`() {
        val game = foreign("torta").takeHint(HintKind.TRANSLATION).takeHint(HintKind.LETTER)
        val state = state(game)
        assertThat(state.takenHints.map { it.kind }).containsExactly(HintKind.TRANSLATION, HintKind.LETTER).inOrder()
        draw(state)
        val translation = state.takenHints.first().text
        assertThat(translation).isNotEmpty()
        compose.onNodeWithContentDescription("Row 6, spent on a hint. Translation: $translation").assertIsDisplayed()
        compose.onNodeWithContentDescription("Row 5, spent on a hint: the first letter is C.").assertIsDisplayed()
        // The row to type says where the player stands, even with nothing typed yet.
        compose.onNodeWithContentDescription("Row 2 of 6: type a word").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("game-hints-taken")
    }

    @Test
    fun `with one attempt left the hints say why they cannot be taken`() {
        draw(state(foreign("torta", "pasta", "salto", "porta", "tetto")), hintsOpen = true)
        compose.onNodeWithTag(GameTags.hint(HintKind.TRANSLATION)).assertIsNotEnabled()
        compose.onAllNodesWithText("Not on the last attempt").assertCountEquals(3)
        compose.writeScreenshot("game-hints-last")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the hints at twice the text size, in Italian`() {
        draw(state(game(Language.SPANISH, "arbol", "nubes", ui = Language.ITALIAN)), hintsOpen = true)
        compose.onNodeWithText("Torna alla tastiera").assertExists()
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-hints-large-text-it")
    }

    @Test
    fun `the end explains the words, offers the notebook and says a milestone`() {
        val alphabet = Alphabet.of(Language.ITALIAN)
        val torta = alphabet.parse("torta")!!
        val done = state(
            foreign("torta", "pasta", "citta"),
            notebook = mapOf(
                alphabet.parse("citta")!! to entry(alphabet.parse("citta")!!, savedByHand = false),
                torta to entry(torta, savedByHand = true),
            ),
            milestones = listOf(Milestone.FIRST_FOREIGN_WIN),
        )
        draw(done)
        compose.onNodeWithText("città").assertExists()
        compose.onNodeWithTag(GameTags.MEANING).assertExists()
        compose.onNodeWithText("Kept in your notebook.").assertExists()
        compose.onNodeWithText("Your first word found in another language.").assertExists()
        compose.onNodeWithContentDescription("Take torta out of the notebook").assertExists()
        compose.onNodeWithContentDescription("Add pasta to the notebook").performScrollTo().performClick()
        assertThat(toggled).containsExactly(1)
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-won-meanings")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `the end in the game's own language gives the definitions, in Spanish`() {
        draw(state(game(Language.SPANISH, "arbol", "nubes", "arbol")))
        compose.onNodeWithText("árbol").assertExists()
        compose.onNodeWithContentDescription("Añade nubes al cuaderno").assertExists()
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-won-meanings-es")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `the meanings at twice the text size`() {
        draw(state(foreign("torta", "pasta", "citta"), milestones = listOf(Milestone.HUNDRED_WORDS)))
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-won-meanings-large-text")
    }

    private fun entry(word: Word, savedByHand: Boolean) = NotebookEntry(
        language = Language.ITALIAN,
        word = word,
        firstMet = LocalDate.of(2026, 11, 20),
        lastMet = LocalDate.of(2026, 11, 20),
        timesMet = 1,
        solved = if (savedByHand) 0 else 1,
        missed = 0,
        withHints = 0,
        savedByHand = savedByHand,
    )

    @Test
    fun `the guided game asks for its first word under the grid, in place of the key`() {
        draw(state(guided(Language.ENGLISH), lessons = listOf(Lesson.FIRST_GUESS)))
        compose.onNodeWithText("First game", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(GameTags.COACH).assertExists()
        compose.onNodeWithContentDescription(
            "Find the hidden word in 6 attempts. Start with any word of 5 letters.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Right place").assertDoesNotExist()
        compose.assertAccessible()
        compose.writeScreenshot("game-guided-start")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `a guided row says each new mark beside its tile, in Italian`() {
        // RUOTA against CUORE: U and O in their places, R elsewhere, T and A absent.
        val game = guided(Language.ITALIAN, "ruota")
        draw(state(game, lessons = listOf(Lesson.RIGHT_PLACE, Lesson.ELSEWHERE, Lesson.ABSENT)))
        compose.onNodeWithText("Quadrata, in inchiostro: la lettera è al suo posto.", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Tonda, in ambra: è nella parola, ma altrove.", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Affondata: la lettera non è nella parola.", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("game-guided-marks-it")
    }

    @Test
    fun `with nothing new to say, the guided game shows the key of the marks`() {
        draw(state(guided(Language.ENGLISH, "pious", "lymph")))
        compose.onNodeWithTag(GameTags.COACH).assertDoesNotExist()
        compose.onNodeWithText("Right place").assertIsDisplayed()
    }

    @Test
    fun `a foreign guided game points at the hints`() {
        draw(state(guided(Language.SPANISH, "playa", "mundo", ui = Language.ENGLISH), lessons = listOf(Lesson.HINTS)))
        compose.onNodeWithContentDescription("Stuck? The bulb above gives a hint, for one attempt.").assertIsDisplayed()
        compose.onNodeWithTag(GameTags.HINTS).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("game-guided-hints")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `the guided game ends with the game itself, nothing to share, in Spanish`() {
        draw(state(guided(Language.SPANISH, "mundo", "amigo")))
        compose.onNodeWithText("Esa es la palabra.").assertExists()
        compose.onNodeWithTag(GameTags.LESSON_DONE).assertExists()
        compose.onNodeWithText("Eso es todo. Una palabra nueva cada día, en cada idioma.").assertExists()
        compose.onNodeWithText("Empezar a jugar").assertExists()
        compose.onNodeWithText("Compartir el resultado").assertDoesNotExist()
        compose.walkPage(hasTestTag(GameTags.PAGE), "game-guided-end-es")
    }
}
