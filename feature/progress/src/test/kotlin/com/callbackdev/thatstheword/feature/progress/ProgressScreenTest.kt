package com.callbackdev.thatstheword.feature.progress

import androidx.compose.ui.test.assertIsDisplayed
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
import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.progress.ProgressMode
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Milestone
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

/**
 * The progress page drawn from its states (PLANNING.md §12): the statistics of a language and
 * mode, the distribution with its latest game, the streaks, the milestones reached and near, the
 * way to your data, and a phone with nothing played; in the three languages, dark, at twice the
 * text size and on an open foldable.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class ProgressScreenTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 20)

    private fun record(
        date: LocalDate?,
        state: GameState = GameState.WON,
        attempts: Int = 4,
        language: Language = Language.ITALIAN,
        hints: Int = 0,
        hard: Boolean = false,
    ) = GameRecord(
        language = language,
        interfaceLanguage = Language.ENGLISH,
        mode = date?.let { GameMode.Daily(it, 1) } ?: GameMode.Free(Level.BASE),
        solution = Alphabet.of(language).parse("carta")!!,
        state = state,
        attemptsUsed = if (state == GameState.LOST) 6 else attempts,
        attempts = 6,
        hints = hints,
        hard = hard,
        startedOn = date ?: today,
    )

    /** Three weeks of an Italian player, with a few English dailies and some free play. */
    private val records: List<GameRecord> = buildList {
        val attempts = listOf(4, 3, 5, 4, 2, 4, 6, 3, 4, 5, 3, 4, 4, 5, 3, 4)
        attempts.forEachIndexed { i, n ->
            add(
                record(
                    today.minusDays(20L - i),
                    attempts = n,
                    hints = if (i % 5 ==
                        0
                    ) {
                        1
                    } else {
                        0
                    },
                ),
            )
        }
        add(record(today.minusDays(4), GameState.LOST))
        add(record(today.minusDays(3), attempts = 3, hard = true))
        add(record(today.minusDays(2), attempts = 4))
        add(record(today.minusDays(1), attempts = 5))
        add(record(today, attempts = 3))
        add(record(null, attempts = 4))
        add(record(null, GameState.LOST))
        repeat(3) { add(record(today.minusDays(it.toLong()), language = Language.ENGLISH, attempts = 3 + it)) }
    }

    private fun state(
        records: List<GameRecord> = this.records,
        language: Language? = null,
        mode: ProgressMode? = null,
        unlocked: Map<Milestone, LocalDate> = mapOf(
            Milestone.FIRST_FOREIGN_WIN to today.minusDays(20),
            Milestone.SEVEN_DAILIES_IN_A_ROW to today.minusDays(14),
        ),
        notebook: Int = 42,
    ) = ProgressUiState.of(records, notebook, unlocked, today, Language.ITALIAN, language, mode)

    private var language: Language? = null
    private var mode: ProgressMode? = null
    private var data = false

    private fun draw(state: ProgressUiState, dark: Boolean = false) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                ProgressScreen(
                    state = state,
                    onBack = {},
                    onLanguage = { language = it },
                    onMode = { mode = it },
                    onOpenData = { data = true },
                )
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
    }

    @Test
    fun `a language's daily, its share found, its streaks and where its attempts went`() {
        draw(state())
        compose.onNodeWithText("Daily words: 20 found out of 21.").assertIsDisplayed()
        compose.onNodeWithContentDescription("95%, of the daily words found").assertIsDisplayed()
        compose.onNodeWithContentDescription("4, current streak").assertIsDisplayed()
        compose.onNodeWithContentDescription("16, longest streak").assertExists()
        // Today's daily was found in 3: its row is the latest.
        compose.onNodeWithContentDescription("In 3 attempts: 6 games. Your latest game.").assertExists()
        compose.onNodeWithContentDescription("Not found: 1 game.").assertExists()
        compose.onNodeWithContentDescription("4, with hints").assertExists()
        compose.onNodeWithContentDescription("1, in Hard").assertExists()
        compose.onNodeWithTag(ProgressTags.language(Language.ITALIAN)).assertIsSelected()
        compose.onNodeWithTag(ProgressTags.mode(ProgressMode.DAILY)).assertIsSelected()
        compose.assertAccessible()
        compose.walkPage(hasTestTag(ProgressTags.PAGE), "progress")
    }

    @Test
    fun `the language, the mode and your data are a touch away`() {
        draw(state())
        compose.onNodeWithTag(ProgressTags.language(Language.ENGLISH)).performClick()
        assertThat(language).isEqualTo(Language.ENGLISH)
        compose.onNodeWithTag(ProgressTags.mode(ProgressMode.FREE)).performClick()
        assertThat(mode).isEqualTo(ProgressMode.FREE)
        compose.onNodeWithTag(ProgressTags.OPEN_DATA).performScrollTo().performClick()
        assertThat(data).isTrue()
    }

    @Test
    fun `free play has no streak, and its latest game was not found`() {
        draw(state(mode = ProgressMode.FREE))
        compose.onNodeWithText("Free words: 1 found out of 2.").assertIsDisplayed()
        compose.onNodeWithContentDescription("50%, of the free words found").assertIsDisplayed()
        compose.onNodeWithContentDescription("4, current streak").assertDoesNotExist()
        compose.onNodeWithContentDescription("Not found: 1 game. Your latest game.").assertExists()
        compose.writeScreenshot("progress-free")
    }

    @Test
    fun `a language of dailies alone has no choice of mode`() {
        val shown = state(language = Language.ENGLISH)
        assertThat(shown.modes).containsExactly(ProgressMode.DAILY)
        draw(shown)
        compose.onNodeWithContentDescription("The daily word · English").assertIsDisplayed()
        compose.onNodeWithTag(ProgressTags.mode(ProgressMode.FREE)).assertDoesNotExist()
    }

    @Test
    fun `the milestones, each reached on its date or with how near it is`() {
        draw(state())
        compose.onNodeWithTag(ProgressTags.MILESTONES).performScrollTo()
        compose.onNodeWithText("2 of 4 reached. They stay for good.").assertIsDisplayed()
        compose.onNodeWithText("Reached on 6 November").assertExists()
        compose.onNodeWithText("42 of 100").assertExists()
        // One date with Italian's and English's dailies found: two of three.
        compose.onNodeWithText("2 of 3").assertExists()
        compose.writeScreenshot("progress-milestones")
    }

    @Test
    fun `nothing played yet says so, and keeps the milestones and your data`() {
        val empty = state(records = emptyList(), unlocked = emptyMap(), notebook = 0)
        assertThat(empty.nothingPlayed).isTrue()
        draw(empty)
        compose.onNodeWithText(
            "Nothing played yet. Your statistics gather here, one game after another.",
        ).assertIsDisplayed()
        compose.onNodeWithText("None reached yet. Each one stays, once it is.").assertExists()
        compose.onNodeWithTag(ProgressTags.language(Language.ITALIAN)).assertDoesNotExist()
        compose.assertAccessible()
        compose.walkPage(hasTestTag(ProgressTags.PAGE), "progress-empty")
    }

    @Test
    fun `a language chosen before stays chosen, one never played does not`() {
        assertThat(state(language = Language.ENGLISH).language).isEqualTo(Language.ENGLISH)
        assertThat(state(language = Language.SPANISH).language).isEqualTo(Language.ITALIAN)
        assertThat(state(mode = ProgressMode.FREE, language = Language.ENGLISH).mode).isEqualTo(ProgressMode.DAILY)
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw(state())
        compose.onNodeWithText("Parole del giorno: 20 trovate su 21.").assertIsDisplayed()
        compose.walkPage(hasTestTag(ProgressTags.PAGE), "progress-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `in Spanish`() {
        draw(state())
        compose.onNodeWithText("Palabras del día: 20 encontradas de 21.").assertIsDisplayed()
        compose.walkPage(hasTestTag(ProgressTags.PAGE), "progress-es")
    }

    @Test
    fun `dark`() {
        draw(state(), dark = true)
        compose.assertAccessible()
        compose.walkPage(hasTestTag(ProgressTags.PAGE), "progress-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw(state())
        compose.walkPage(hasTestTag(ProgressTags.PAGE), "progress-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw(state())
        compose.walkPage(hasTestTag(ProgressTags.PAGE), "progress-foldable")
    }
}
