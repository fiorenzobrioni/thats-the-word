package com.callbackdev.thatstheword.feature.home

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
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
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
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The start page drawn from its states (PLANNING.md §11 Phase 4, §12): today's word in each
 * language, the streak, a month of the calendar with solved, missed, unplayed and archive days,
 * free play and Hard; in the three languages, dark, on a small phone, at twice the text size and
 * on an open foldable; each checked for TalkBack's labels and a finger's targets.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    private val rome = ZoneId.of("Europe/Rome")
    private val today = LocalDate.of(2026, 11, 28)
    private val calendar =
        DailyCalendar(Clock.fixed(ZonedDateTime.of(2026, 11, 28, 9, 0, 0, 0, rome).toInstant(), rome))

    private fun day(n: Int) = LocalDate.of(2026, 11, n)

    private fun daily(
        date: LocalDate,
        state: GameState,
        language: Language = Language.ITALIAN,
        startedOn: LocalDate = date,
        attemptsUsed: Int = 4,
    ) = GameRecord(
        language = language,
        interfaceLanguage = Language.ENGLISH,
        mode = GameMode.Daily(date, calendar.number(date)),
        solution = Alphabet.of(language).parse("cuore")!!,
        state = state,
        attemptsUsed = if (state == GameState.LOST) 6 else attemptsUsed,
        attempts = 6,
        hints = 0,
        hard = false,
        startedOn = startedOn,
    )

    /** A month of Italian dailies with every kind of day, English's found today, Spanish's under way. */
    private val records = listOf(
        daily(day(12), GameState.WON, attemptsUsed = 3),
        daily(day(13), GameState.WON),
        daily(day(14), GameState.LOST),
        daily(day(15), GameState.WON, startedOn = day(22)),
        daily(day(16), GameState.LOST, startedOn = day(22)),
        daily(day(18), GameState.WON),
        daily(day(19), GameState.WON),
        daily(day(24), GameState.PLAYING, attemptsUsed = 2),
        daily(day(25), GameState.WON),
        daily(day(26), GameState.WON),
        daily(day(27), GameState.WON),
        daily(today, GameState.WON, Language.ENGLISH, attemptsUsed = 4),
        daily(today, GameState.PLAYING, Language.SPANISH, attemptsUsed = 2),
    )

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
    )

    private fun state(
        settings: UserSettings = this.settings,
        records: List<GameRecord> = this.records,
        month: YearMonth? = null,
    ) = HomeUiState.of(settings, records, calendar, month, scheduleLength = 3_654)

    private val opened = mutableListOf<LocalDate>()
    private val languages = mutableListOf<Language>()
    private var free = 0
    private var hard: Boolean? = null

    private fun draw(state: HomeUiState = state(), dark: Boolean = false) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                HomeScreen(
                    state = state,
                    onLanguage = { languages += it },
                    onPlayToday = { opened += state.today },
                    onDay = { opened += it },
                    onMonth = {},
                    onLevel = {},
                    onPlayFree = { free++ },
                    onHard = { hard = it },
                )
            }
        }
        // Past the sections' arrival and the tiles' reveal: the test clock runs every spring to its end.
        compose.mainClock.advanceTimeBy(3_000)
    }

    @Test
    fun `the state reads the day right`() {
        val state = state()
        assertThat(state.number).isEqualTo(28)
        assertThat(state.streak).isEqualTo(3)
        assertThat(state.month.found).isEqualTo(8)
        assertThat(state.month.missed).isEqualTo(2)
        assertThat(state.month.caughtUp).isEqualTo(2)
        assertThat(state.level).isEqualTo(Level.INTERMEDIATE)
        assertThat(state.languages.map { it.status.name })
            .containsExactly("NOT_PLAYED", "SOLVED", "IN_PROGRESS").inOrder()
    }

    @Test
    fun `today's word in each language, and one touch plays in another`() {
        draw()
        compose.onNodeWithContentDescription("That’s The Word").assertIsDisplayed()
        compose.onNodeWithTag(HomeTags.language(Language.ITALIAN)).assertIsSelected()
        compose.onNodeWithContentDescription("Italian: waiting").assertIsDisplayed()
        compose.onNodeWithContentDescription("English: found in 4 attempts").assertIsDisplayed()
        compose.onNodeWithContentDescription("Spanish: under way").performClick()
        assertThat(languages).containsExactly(Language.SPANISH)
        compose.onNodeWithText("A new word is waiting for you.").assertIsDisplayed()
        compose.onNodeWithText("Play today’s word").performClick()
        assertThat(opened).containsExactly(today)
        compose.assertAccessible()
        compose.writeScreenshot("home")
    }

    @Test
    fun `the streak is the page's one number, with its line`() {
        draw()
        compose.onNodeWithContentDescription("A streak of 3 days").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("days in a row, each word found on its own day.").assertIsDisplayed()
        compose.onNodeWithText("Find today’s word to keep it going.").assertIsDisplayed()
    }

    @Test
    fun `the calendar of a month with solved, missed, unplayed and archive days reads right`() {
        draw()
        compose.onNodeWithText("November 2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("8 words found, 2 missed. 2 of them caught up later.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Thursday 12 November, found").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Saturday 14 November, missed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "Sunday 15 November, found later, from the archive",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "Monday 16 November, missed later, from the archive",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Tuesday 17 November, not played").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Tuesday 24 November, under way").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "Saturday 28 November, today, not played",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Sunday 29 November, not yet").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "Wednesday 11 November, not played",
        ).performScrollTo().assertIsDisplayed()
        // A past day is played from the archive; a day ahead is not a door.
        compose.onNodeWithTag(HomeTags.day(day(17))).performClick()
        compose.onNodeWithTag(HomeTags.day(day(29))).performClick()
        assertThat(opened).containsExactly(day(17))
        compose.assertAccessible()
        compose.writeScreenshot("home-calendar")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the calendar in Italian`() {
        draw()
        compose.onNodeWithText("Novembre 2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("8 parole trovate, 2 sfuggite. 2 recuperate dopo.").assertIsDisplayed()
        compose.onNodeWithContentDescription("giovedì 12 novembre, trovata").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "domenica 15 novembre, trovata dopo, dall’archivio",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("martedì 17 novembre, non giocata").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "sabato 28 novembre, oggi, non giocata",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Inglese: trovata in 4 tentativi").performScrollTo().assertIsDisplayed()
        compose.writeScreenshot("home-calendar-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `the calendar in Spanish`() {
        draw()
        compose.onNodeWithText("Noviembre de 2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("8 palabras encontradas, 2 se escaparon. 2 recuperadas después.").assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "jueves, 12 de noviembre, encontrada",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "lunes, 16 de noviembre, se escapó después, desde el archivo",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "sábado, 28 de noviembre, hoy, sin jugar",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Español: en curso").performScrollTo().assertIsDisplayed()
        compose.writeScreenshot("home-calendar-es")
    }

    @Test
    fun `free play at a level, and Hard for the next game`() {
        draw()
        compose.onNodeWithText("Intermediate").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Play a word").performScrollTo().performClick()
        assertThat(free).isEqualTo(1)
        compose.onNodeWithTag(HomeTags.HARD).performScrollTo().performClick()
        assertThat(hard).isTrue()
        compose.assertAccessible()
        compose.writeScreenshot("home-free")
    }

    @Test
    fun `a free word under way is taken up`() {
        val underWay = records + GameRecord(
            language = Language.ITALIAN,
            interfaceLanguage = Language.ENGLISH,
            mode = GameMode.Free(Level.ADVANCED),
            solution = Alphabet.of(Language.ITALIAN).parse("cuore")!!,
            state = GameState.PLAYING,
            attemptsUsed = 1,
            attempts = 6,
            hints = 0,
            hard = false,
            startedOn = today,
        )
        draw(state(records = underWay))
        compose.onNodeWithText("A word is under way, at Advanced.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Take up your word").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `today found, the streak with it`() {
        val found = listOf(daily(today, GameState.WON, attemptsUsed = 3), daily(day(26), GameState.WON))
        draw(state(records = found))
        compose.onNodeWithText("Found in 3 attempts of 6.").assertIsDisplayed()
        compose.onNodeWithText("See how it went").assertIsDisplayed()
        compose.onNodeWithContentDescription("A streak of 1 day").performScrollTo().assertIsDisplayed()
        compose.writeScreenshot("home-found")
    }

    @Test
    fun `today missed ends the streak`() {
        val missed = listOf(daily(today, GameState.LOST), daily(day(27), GameState.WON))
        draw(state(records = missed))
        compose.onNodeWithText("Not this time. Tomorrow brings another.").assertIsDisplayed()
        compose.onNodeWithText(
            "No streak running. Find a word on its own day, and one begins.",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `dark`() {
        draw(dark = true)
        compose.writeScreenshot("home-dark")
        compose.onNodeWithTag(HomeTags.MONTH).performScrollTo()
        compose.writeScreenshot("home-dark-calendar")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw()
        compose.walkPage(hasTestTag(HomeTags.PAGE), "home-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `in Spanish`() {
        draw()
        compose.walkPage(hasTestTag(HomeTags.PAGE), "home-es")
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h640dp-xhdpi")
    fun `on a small phone`() {
        draw()
        compose.walkPage(hasTestTag(HomeTags.PAGE), "home-small")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw()
        compose.walkPage(hasTestTag(HomeTags.PAGE), "home-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw()
        compose.walkPage(hasTestTag(HomeTags.PAGE), "home-foldable")
    }
}
