package com.callbackdev.thatstheword.shell

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.testing.assertAccessible
import com.callbackdev.thatstheword.core.testing.walkPage
import com.callbackdev.thatstheword.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** The start page (PLANNING.md §11 Phases 0 and 3), in the three languages, both themes and both dresses. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class StartPageTest {
    @get:Rule val compose = createComposeRule()

    private var today: TodayUi? = TodayUi(Language.ITALIAN, LocalDate.of(2026, 11, 20), 9, DayStatus.NOT_PLAYED)
    private var played = 0

    private fun draw(dark: Boolean = false, palette: AppPalette = AppPalette.VIVID) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark, palette = palette) {
                Surface { StartPage(today = today, onPlay = { played++ }) }
            }
        }
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
    fun `offers today's daily in the game language`() {
        draw()
        compose.onNodeWithText("Play today’s word").performClick()
        compose.onNodeWithText("Italian · daily #9").assertIsDisplayed()
        assertThat(played).isEqualTo(1)
    }

    @Test
    fun `takes up a daily in progress, and shows one finished`() {
        today = today?.copy(status = DayStatus.IN_PROGRESS)
        draw()
        compose.onNodeWithText("Take up today’s word").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `a finished daily, in Spanish`() {
        today = today?.copy(status = DayStatus.SOLVED, language = Language.ENGLISH)
        draw()
        compose.onNodeWithText("Mira la palabra de hoy").assertIsDisplayed()
        compose.onNodeWithText("Inglés · diaria n.º 9").assertIsDisplayed()
        compose.writeScreenshot("start-es-solved")
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
