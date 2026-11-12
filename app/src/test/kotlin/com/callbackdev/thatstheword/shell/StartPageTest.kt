package com.callbackdev.thatstheword.shell

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.testing.assertAccessible
import com.callbackdev.thatstheword.core.testing.walkPage
import com.callbackdev.thatstheword.core.testing.writeScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The start page (PLANNING.md §11 Phase 0), in the three languages, both themes and both dresses. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class StartPageTest {
    @get:Rule val compose = createComposeRule()

    private fun draw(dark: Boolean = false, palette: AppPalette = AppPalette.VIVID) {
        compose.setContent { ThatsTheWordTheme(darkTheme = dark, palette = palette) { ThatsTheWordRoot() } }
        // Past the tiles' wave: the test clock runs every spring to its end.
        compose.mainClock.advanceTimeBy(2_000)
    }

    @Test
    fun `says the name, composes a word and greets it`() {
        draw()
        compose.onNodeWithContentDescription("That’s The Word").assertIsDisplayed()
        compose.onNodeWithText("That’s the word.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Table").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("start")
    }

    @Test
    fun `draws the capitals and keeps the words for TalkBack`() {
        draw()
        compose.onAllNodesWithText("THAT’S THE WORD").assertCountEquals(0)
        compose.onNodeWithContentDescription("One word a day. Three languages.").assertIsDisplayed()
    }

    @Test
    fun `dark`() {
        draw(dark = true)
        compose.onNodeWithText("That’s the word.").assertIsDisplayed()
        compose.writeScreenshot("start-dark")
    }

    @Test
    fun `in the paper dress`() {
        draw(palette = AppPalette.PAPER)
        compose.writeScreenshot("start-paper")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw()
        compose.onNodeWithText("Ecco la parola.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sedia").assertIsDisplayed()
        compose.onNodeWithContentDescription("That’s The Word").assertIsDisplayed()
        compose.writeScreenshot("start-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `in Spanish`() {
        draw()
        compose.onNodeWithText("Esa es la palabra.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Una palabra al día. Tres idiomas.").assertIsDisplayed()
        compose.writeScreenshot("start-es")
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h640dp-xhdpi")
    fun `on a small phone`() {
        draw()
        compose.walkPage(hasTestTag(ShellTags.PAGE), "start-small")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw()
        compose.walkPage(hasTestTag(ShellTags.PAGE), "start-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw()
        compose.walkPage(hasTestTag(ShellTags.PAGE), "start-foldable")
    }
}
