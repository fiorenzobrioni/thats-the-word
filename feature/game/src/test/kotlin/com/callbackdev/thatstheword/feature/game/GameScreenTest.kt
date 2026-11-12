package com.callbackdev.thatstheword.feature.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.domain.game.WordList
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Letter
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
    ): GameUiState {
        val alphabet = Alphabet.of(game.setup.language)
        return GameUiState.of(
            game,
            typed.map { alphabet.letterOf(it)!! },
            refusal?.let { RefusalUi(it, letter, 1) },
            Lexicon(files, game.setup.language).forms,
            practice,
        )
    }

    private val typedLetters = mutableListOf<Letter>()
    private var entered = 0

    private fun draw(state: GameUiState, dark: Boolean = false) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                GameScreen(
                    state = state,
                    onBack = {},
                    onLetter = { typedLetters += it },
                    onDelete = {},
                    onEnter = { entered++ },
                    onShare = {},
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
        compose.onNodeWithContentDescription("Row 2, typing: P L").assertIsDisplayed()
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
}
