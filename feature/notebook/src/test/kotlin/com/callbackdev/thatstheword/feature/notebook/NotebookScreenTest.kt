package com.callbackdev.thatstheword.feature.notebook

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.NotebookEntry
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
 * The notebook drawn from its states (PLANNING.md §12), its words read from the real words' files:
 * the words met with their meanings, the search, the languages, a word opened, the empty notebook;
 * in the three languages, dark, at twice the text size and on an open foldable.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class NotebookScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val files = AssetWordFiles(context.assets)
    private val day = LocalDate.of(2026, 11, 20)

    private fun entry(
        language: Language,
        key: String,
        daysAgo: Long,
        solved: Int = 1,
        missed: Int = 0,
        hints: Int = 0,
        byHand: Boolean = false,
    ) = NotebookEntry(
        language = language,
        word = Alphabet.of(language).parse(key)!!,
        firstMet = day.minusDays(daysAgo + 10),
        lastMet = day.minusDays(daysAgo),
        timesMet = maxOf(1, solved + missed),
        solved = solved,
        missed = missed,
        withHints = hints,
        savedByHand = byHand,
    )

    private val entries = listOf(
        entry(Language.ITALIAN, "citta", 0, hints = 1),
        entry(Language.SPANISH, "arbol", 1, solved = 2, missed = 1),
        entry(Language.ITALIAN, "torta", 2, solved = 0, byHand = true),
        entry(Language.ENGLISH, "apple", 3, solved = 0, missed = 1),
        entry(Language.SPANISH, "nubes", 4, solved = 0, byHand = true),
    )

    private fun words(speaks: Language) = entries.map { NotebookWordUi.of(it, Lexicon(files, it.language), speaks) }

    private var query: String? = null
    private var language: Language? = Language.ENGLISH

    private fun draw(state: NotebookUiState, dark: Boolean = false) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                NotebookScreen(state = state, onBack = {}, onQuery = { query = it }, onLanguage = { language = it })
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun `the words met, each with what it means, the latest first`() {
        draw(NotebookUiState.of(words(Language.ENGLISH), "", null))
        compose.onNodeWithContentDescription("5 words", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("città").assertIsDisplayed()
        compose.onNodeWithText("city, town").assertIsDisplayed()
        compose.onNodeWithText("Found with hints · 20 November").assertIsDisplayed()
        compose.onNodeWithText("árbol").assertIsDisplayed()
        compose.onNodeWithText("Found, sometimes missed · 19 November").assertIsDisplayed()
        compose.onNodeWithText("Kept from a game · 18 November").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("notebook")
    }

    @Test
    fun `a word opened says the rest`() {
        draw(NotebookUiState.of(words(Language.ENGLISH), "", null))
        compose.onNodeWithTag(NotebookTags.word("es:arbol")).performClick()
        compose.onNodeWithText("Met 3 times since 9 November.").assertIsDisplayed()
        compose.writeScreenshot("notebook-opened")
    }

    @Test
    fun `the search forgives the accents, and the languages filter`() {
        val all = words(Language.ENGLISH)
        val found = NotebookUiState.of(all, "citta", null)
        assertThat(found.words.map { it.written }).containsExactly("città")
        // A translation is searched too.
        assertThat(NotebookUiState.of(all, "tree", null).words.map { it.written }).containsExactly("árbol")
        val spanish = NotebookUiState.of(all, "", Language.SPANISH)
        assertThat(spanish.words.map { it.language }.toSet()).containsExactly(Language.SPANISH)
        assertThat(spanish.languages).containsExactly(Language.ITALIAN, Language.ENGLISH, Language.SPANISH).inOrder()

        draw(found)
        compose.onNodeWithTag(NotebookTags.SEARCH).performTextInput("x")
        assertThat(query).contains("x")
        compose.onNodeWithContentDescription("Clear the search").performClick()
        assertThat(query).isEmpty()
        compose.onNodeWithText("All").performClick()
        assertThat(language).isNull()
        compose.writeScreenshot("notebook-search")
    }

    @Test
    fun `a search that finds nothing says so`() {
        draw(NotebookUiState.of(words(Language.ENGLISH), "zzz", null))
        compose.onNodeWithText("No word matches.").assertIsDisplayed()
    }

    @Test
    fun `an empty notebook says what will gather there`() {
        draw(NotebookUiState.of(emptyList(), "", null))
        compose.onNodeWithText("No word yet. The words of your games gather here.").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("notebook-empty")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw(NotebookUiState.of(words(Language.ITALIAN), "", null))
        compose.onNodeWithText("Trovata con aiuti · 20 novembre").assertIsDisplayed()
        compose.walkPage(hasTestTag(NotebookTags.LIST), "notebook-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `in Spanish`() {
        draw(NotebookUiState.of(words(Language.SPANISH), "", null))
        compose.walkPage(hasTestTag(NotebookTags.LIST), "notebook-es")
    }

    @Test
    fun `dark`() {
        draw(NotebookUiState.of(words(Language.ENGLISH), "", null), dark = true)
        compose.assertAccessible()
        compose.writeScreenshot("notebook-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw(NotebookUiState.of(words(Language.ENGLISH), "", null))
        compose.walkPage(hasTestTag(NotebookTags.LIST), "notebook-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw(NotebookUiState.of(words(Language.ENGLISH), "", null))
        compose.walkPage(hasTestTag(NotebookTags.LIST), "notebook-foldable")
    }
}
