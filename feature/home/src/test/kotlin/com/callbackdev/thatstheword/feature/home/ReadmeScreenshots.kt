package com.callbackdev.thatstheword.feature.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
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
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The README's pictures of the start page (docs/screenshots), from a player whose phone speaks
 * English, on Friday 13 November 2026: today's English word found, the Italian one under way, the
 * Spanish one waiting; a month of English dailies behind them. Run with
 * `./gradlew :feature:home:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("thatstheword.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val rome = ZoneId.of("Europe/Rome")
    private val today = LocalDate.of(2026, 11, 13)
    private val calendar =
        DailyCalendar(Clock.fixed(ZonedDateTime.of(2026, 11, 13, 19, 0, 0, 0, rome).toInstant(), rome))

    private fun day(n: Int) = LocalDate.of(2026, 11, n)

    private fun daily(
        date: LocalDate,
        state: GameState,
        language: Language = Language.ENGLISH,
        attemptsUsed: Int = 4,
        startedOn: LocalDate = date,
    ) = GameRecord(
        language = language,
        interfaceLanguage = Language.ENGLISH,
        mode = GameMode.Daily(date, calendar.number(date)),
        solution = Alphabet.of(language).parse(if (language == Language.ENGLISH) "brave" else "cuore")!!,
        state = state,
        attemptsUsed = if (state == GameState.LOST) 6 else attemptsUsed,
        attempts = 6,
        hints = 0,
        hard = false,
        startedOn = startedOn,
    )

    /** Since the first daily: two missed, one caught up from the archive, a run of six to today. */
    private val records = listOf(
        daily(day(1), GameState.WON, attemptsUsed = 5),
        daily(day(2), GameState.WON, attemptsUsed = 3),
        daily(day(3), GameState.LOST),
        daily(day(4), GameState.WON),
        daily(day(5), GameState.WON, attemptsUsed = 5, startedOn = day(9)),
        daily(day(6), GameState.LOST),
        daily(day(8), GameState.WON, attemptsUsed = 3),
        daily(day(9), GameState.WON),
        daily(day(10), GameState.WON, attemptsUsed = 5),
        daily(day(11), GameState.WON, attemptsUsed = 3),
        daily(day(12), GameState.WON),
        daily(today, GameState.WON, attemptsUsed = 4),
        daily(today, GameState.PLAYING, Language.ITALIAN, attemptsUsed = 2),
    )

    private val settings = UserSettings(
        gameLanguage = Language.ENGLISH,
        levels = mapOf(Language.ENGLISH to Level.INTERMEDIATE),
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

    private val state = HomeUiState.of(settings, records, calendar, null, scheduleLength = 3_654, notebookSize = 23)

    @Test
    fun home() {
        show()
        save("home")
    }

    /** The calendar of the dailies: found, missed, caught up later, not played. */
    @Test
    fun calendar() {
        show()
        // The month's title near the page's top, its section's heading just above it.
        val top = compose.onNodeWithTag(HomeTags.MONTH).fetchSemanticsNode().positionInRoot.y
        val above = 96 * context.resources.displayMetrics.density
        compose.onNodeWithTag(HomeTags.PAGE).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, top - above) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        save("calendar")
    }

    private fun show() {
        compose.setContent {
            ThatsTheWordTheme {
                HomeScreen(
                    state = state,
                    onLanguage = {},
                    onPlayToday = {},
                    onDay = {},
                    onMonth = {},
                    onLevel = {},
                    onPlayFree = {},
                    onHard = {},
                    onNotebook = {},
                    onProgress = {},
                    onSettings = {},
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
