package com.callbackdev.thatstheword.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.callbackdev.thatstheword.core.model.UserSettings
import com.callbackdev.thatstheword.core.reminder.ReminderBlock
import com.callbackdev.thatstheword.core.testing.assertAccessible
import com.callbackdev.thatstheword.core.testing.walkPage
import com.callbackdev.thatstheword.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime

/**
 * Settings drawn from its states (PLANNING.md §12): how to play first, every choice with its line,
 * the reminder on, and blocked; in the three languages, dark, at twice the text size and on an open
 * foldable; the licences' page.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val settings = UserSettings(
        gameLanguage = Language.ITALIAN,
        levels = mapOf(Language.ITALIAN to Level.INTERMEDIATE),
        hard = false,
        sound = true,
        haptics = true,
        reducedMotion = null,
        themeMode = ThemeMode.SYSTEM,
        palette = AppPalette.VIVID,
        font = AppFont.GOOGLE_SANS,
        wallpaperColors = false,
        firstRunDone = true,
        tipsSeen = true,
        reminder = false,
        reminderTime = LocalTime.of(20, 0),
        askedNotifications = false,
    )

    private val chosen = mutableListOf<Any?>()
    private var guided = 0

    private fun draw(state: SettingsUiState, dark: Boolean = false, block: ReminderBlock = ReminderBlock.NONE) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                SettingsScreen(
                    state = state,
                    onBack = {},
                    reminderBlock = block,
                    version = "0.7.0",
                    actions = SettingsActions(
                        gameLanguage = { chosen += it },
                        level = { chosen += it },
                        hard = { chosen += "hard $it" },
                        reminder = { chosen += "reminder $it" },
                        motion = { chosen += "motion $it" },
                        palette = { chosen += it },
                        font = { chosen += it },
                        playGuided = { guided++ },
                    ),
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun `how to play comes first, with its sample row and the first game again`() {
        draw(SettingsUiState(settings, Language.ENGLISH))
        compose.onNodeWithContentDescription("How to play").assertExists()
        compose.onNodeWithContentDescription(
            "An example row: P, right place; L, not in the word; A, elsewhere; N, not in the word; T, right place",
        ).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.PLAY_GUIDED).performScrollTo().performClick()
        assertThat(guided).isEqualTo(1)
        compose.assertAccessible()
        compose.writeScreenshot("settings-top")
    }

    @Test
    fun `every choice is said with its line, and a touch chooses`() {
        draw(SettingsUiState(settings, Language.ENGLISH))
        compose.onNodeWithTag(SettingsTags.gameLanguage(Language.ITALIAN)).assertIsSelected()
        compose.onNodeWithText("In Italian. Each language keeps its own; today’s word is the same for everyone.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.level(Level.INTERMEDIATE)).assertIsSelected()
        compose.onNodeWithTag(SettingsTags.gameLanguage(Language.SPANISH)).performClick()
        compose.onNodeWithTag(SettingsTags.level(Level.ADVANCED)).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.HARD).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.motion(true)).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.dress(AppPalette.PAPER)).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTags.font(AppFont.INTER)).performScrollTo().performClick()
        assertThat(chosen).containsExactly(
            Language.SPANISH,
            Level.ADVANCED,
            "hard true",
            "motion true",
            AppPalette.PAPER,
            AppFont.INTER,
        ).inOrder()
        compose.onNodeWithTag(SettingsTags.SOUND).performScrollTo().assertIsOn()
        compose.onNodeWithText("That’s The Word sends nothing").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Version 0.7.0").performScrollTo().assertIsDisplayed()
        compose.walkPage(hasTestTag(SettingsTags.PAGE), "settings")
    }

    @Test
    fun `the reminder on says its hour, and says when it cannot come`() {
        draw(
            SettingsUiState(settings.copy(reminder = true, reminderTime = LocalTime.of(19, 30)), Language.ENGLISH),
            block = ReminderBlock.APP,
        )
        compose.onNodeWithTag(SettingsTags.REMINDER).performScrollTo().assertIsOn()
        compose.onNodeWithText("Every day at 19:30").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Notifications are off for That’s The Word, so the reminder cannot come.")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Open notification settings").performScrollTo().assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-reminder-blocked")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian, dark`() {
        draw(
            SettingsUiState(settings.copy(themeMode = ThemeMode.DARK, palette = AppPalette.PAPER), Language.ITALIAN),
            dark = true,
        )
        compose.onNodeWithText("Rigioca la prima partita").assertExists()
        compose.onNodeWithText(
            "In italiano. Ogni lingua ha il suo; la parola di oggi è uguale per tutti.",
        ).assertExists()
        compose.walkPage(hasTestTag(SettingsTags.PAGE), "settings-it-dark")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `in Spanish, with the wallpaper's colours`() {
        draw(SettingsUiState(settings.copy(wallpaperColors = true, reminder = true), Language.SPANISH))
        compose.onNodeWithContentDescription("Cómo se juega").assertExists()
        compose.onNodeWithText(
            "Ahora se usan los colores del fondo. El vestido vuelve cuando los apagas.",
        ).assertExists()
        compose.walkPage(hasTestTag(SettingsTags.PAGE), "settings-es")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw(SettingsUiState(settings.copy(reminder = true), Language.ENGLISH))
        compose.walkPage(hasTestTag(SettingsTags.PAGE), "settings-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw(SettingsUiState(settings, Language.ENGLISH))
        compose.walkPage(hasTestTag(SettingsTags.PAGE), "settings-foldable")
    }

    @Test
    fun `the licences credit every source`() {
        compose.setContent { ThatsTheWordTheme { LicencesScreen(onBack = {}) } }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("GNU General Public License v3.0 or later").assertExists()
        compose.onNodeWithText("Wiktionary, through wiktextract and kaikki.org").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("SCOWL, by Kevin Atkinson (2020.12.07)").performScrollTo().assertIsDisplayed()
        compose.walkPage(hasTestTag(SettingsTags.LICENCES_PAGE), "licences")
    }
}
