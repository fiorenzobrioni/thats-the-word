package com.callbackdev.thatstheword.feature.settings.data

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.DataContents
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.domain.backup.Backup
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.callbackdev.thatstheword.core.model.UserSettings
import com.callbackdev.thatstheword.core.testing.assertAccessible
import com.callbackdev.thatstheword.core.testing.walkPage
import com.callbackdev.thatstheword.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate

/**
 * Your data drawn from its states (PLANNING.md §12): what the phone holds, a copy saved, a copy
 * refused, the question before a copy is brought back, a task under way; in the three languages,
 * dark, at twice the text size and on an open foldable.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class DataScreenTest {
    @get:Rule val compose = createComposeRule()

    private val contents = DataContents(games = 27, words = 41, milestones = 2)
    private var confirmed = false
    private var cancelled = false
    private var dismissed = false

    private val backup = Backup(
        exportedAt = Instant.parse("2026-11-13T09:00:00Z"),
        zone = "Europe/Rome",
        appVersion = "1.0.0",
        settings = UserSettings(
            Language.ITALIAN,
            emptyMap(),
            hard = false,
            sound = true,
            haptics = true,
            reducedMotion = null,
            themeMode = ThemeMode.SYSTEM,
            palette = AppPalette.VIVID,
            font = AppFont.GOOGLE_SANS,
            wallpaperColors = false,
            firstRunDone = true,
            tipsSeen = false,
        ),
        games = emptyList(),
        notebook = emptyList(),
        milestones = emptyMap(),
    )

    private fun draw(state: DataUiState, dark: Boolean = false) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                DataScreen(
                    state = state,
                    onBack = {},
                    actions = DataActions(
                        confirmRestore = { confirmed = true },
                        cancelRestore = { cancelled = true },
                        dismissOutcome = { dismissed = true },
                    ),
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
    }

    @Test
    fun `what the phone holds, and the two things to do with a copy`() {
        draw(DataUiState(contents))
        compose.onNodeWithContentDescription("27 games played").assertIsDisplayed()
        compose.onNodeWithContentDescription("41 words in the notebook").assertIsDisplayed()
        compose.onNodeWithContentDescription("2 milestones").assertIsDisplayed()
        compose.onNodeWithText("Save a copy").assertExists()
        compose.onNodeWithText("Open a copy").assertExists()
        compose.assertAccessible()
        compose.walkPage(hasTestTag(DataTags.PAGE), "data")
    }

    @Test
    fun `a phone with nothing yet says so`() {
        draw(DataUiState())
        compose.onNodeWithText("Nothing yet: no game finished, no word kept.").assertIsDisplayed()
        compose.writeScreenshot("data-empty")
    }

    @Test
    fun `a copy saved says where, and what it holds`() {
        draw(DataUiState(contents, outcome = DataOutcome.Saved("thatstheword-backup-2026-11-13.json", contents)))
        compose.onNodeWithText(
            "Copy saved as thatstheword-backup-2026-11-13.json. " +
                "It holds 27 games played, 41 words in the notebook and 2 milestones.",
        ).assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        assertThat(dismissed).isTrue()
        compose.assertAccessible()
        compose.writeScreenshot("data-saved")
    }

    @Test
    fun `a copy refused says why, and that nothing changed`() {
        draw(DataUiState(contents, outcome = DataOutcome.Failed(DataFailure.DAMAGED)))
        compose.onNodeWithText(
            "That copy is damaged, and cannot be brought back. Nothing was changed.",
        ).assertIsDisplayed()
        compose.writeScreenshot("data-damaged")
    }

    @Test
    fun `a task under way says what it does, and holds the buttons`() {
        draw(DataUiState(contents, task = DataTask.READING))
        compose.onNodeWithText("Reading the copy…").assertIsDisplayed()
        compose.onNodeWithTag("${DataTags.SAVE}_button").assertIsNotEnabled()
        compose.writeScreenshot("data-reading")
    }

    @Test
    fun `a copy is brought back only on the yes`() {
        val preview = RestorePreview(backup, DataContents(12, 30, 1), LocalDate.of(2026, 11, 13))
        draw(DataUiState(contents, preview = preview))
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithText(
            "Saved on 13 November 2026, it holds 12 games played, 30 words in the notebook and 1 milestone.",
        )
            .assertIsDisplayed()
        compose.onNodeWithText(
            "It will take the place of what this phone holds: 27 games played, 41 words in the notebook and 2 milestones.",
        ).assertIsDisplayed()
        compose.writeScreenshot("data-confirm")
        compose.onNodeWithTag(DataTags.CANCEL).performClick()
        assertThat(cancelled).isTrue()
        compose.onNodeWithTag(DataTags.CONFIRM).performClick()
        assertThat(confirmed).isTrue()
    }

    @Test
    fun `an empty phone loses nothing`() {
        val preview = RestorePreview(backup, DataContents(12, 30, 1), LocalDate.of(2026, 11, 13))
        draw(DataUiState(preview = preview))
        compose.onNodeWithText("This phone holds nothing yet: nothing is lost.").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        val preview = RestorePreview(backup, DataContents(12, 30, 1), LocalDate.of(2026, 11, 13))
        draw(DataUiState(contents, preview = preview))
        compose.onNodeWithText(
            "Salvata il 13 novembre 2026, contiene 12 partite giocate, 30 parole nel quaderno e 1 traguardo.",
        )
            .assertIsDisplayed()
        compose.writeScreenshot("data-confirm-it")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the page in Italian`() {
        draw(DataUiState(contents, outcome = DataOutcome.Restored(contents)))
        compose.walkPage(hasTestTag(DataTags.PAGE), "data-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `in Spanish`() {
        draw(DataUiState(contents, outcome = DataOutcome.Failed(DataFailure.TOO_NEW)))
        compose.walkPage(hasTestTag(DataTags.PAGE), "data-es")
    }

    @Test
    fun `dark`() {
        draw(DataUiState(contents, outcome = DataOutcome.Restored(contents)), dark = true)
        compose.assertAccessible()
        compose.walkPage(hasTestTag(DataTags.PAGE), "data-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw(DataUiState(contents))
        compose.walkPage(hasTestTag(DataTags.PAGE), "data-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw(DataUiState(contents))
        compose.walkPage(hasTestTag(DataTags.PAGE), "data-foldable")
    }
}
