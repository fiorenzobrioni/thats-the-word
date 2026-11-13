package com.callbackdev.thatstheword.feature.progress

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Milestone
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * The README's picture of the progress page (docs/screenshots): the English dailies of the start
 * page's sample, from the first daily to Friday 13 November 2026, with a few free games and the
 * Italian practice. Run with `./gradlew :feature:progress:testDebugUnitTest -PupdateScreenshots`;
 * skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("thatstheword.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private val today = LocalDate.of(2026, 11, 13)

    private fun record(
        date: LocalDate?,
        state: GameState = GameState.WON,
        attempts: Int = 4,
        language: Language = Language.ENGLISH,
        hints: Int = 0,
    ) = GameRecord(
        language = language,
        interfaceLanguage = Language.ENGLISH,
        mode = date?.let { GameMode.Daily(it, it.dayOfMonth) } ?: GameMode.Free(Level.INTERMEDIATE),
        solution = Alphabet.of(language).parse(if (language == Language.ENGLISH) "brave" else "cuore")!!,
        state = state,
        attemptsUsed = if (state == GameState.LOST) 6 else attempts,
        attempts = 6,
        hints = hints,
        hard = false,
        startedOn = date ?: today,
    )

    private fun day(n: Int) = LocalDate.of(2026, 11, n)

    /** The start page's month, a run of six to today, and the free games of a few evenings. */
    private val records = listOf(
        record(day(1), attempts = 5),
        record(day(2), attempts = 3),
        record(day(3), GameState.LOST),
        record(day(4)),
        record(day(5), attempts = 5),
        record(day(6), GameState.LOST),
        record(day(8), attempts = 3),
        record(day(9)),
        record(day(10), attempts = 5),
        record(day(11), attempts = 3),
        record(day(12)),
        record(day(13)),
        record(null, attempts = 4),
        record(null, attempts = 5),
        record(null, attempts = 3),
        record(null, GameState.LOST),
        record(day(7), language = Language.ITALIAN, attempts = 5, hints = 1),
        record(day(10), language = Language.ITALIAN, attempts = 4),
    )

    @Test
    fun progress() {
        val state = ProgressUiState.of(
            records,
            23,
            mapOf(Milestone.FIRST_FOREIGN_WIN to day(7)),
            today,
            Language.ENGLISH,
            null,
            null,
        )
        compose.setContent {
            ThatsTheWordTheme {
                ProgressScreen(state = state, onBack = {}, onLanguage = {}, onMode = {}, onOpenData = {})
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "progress.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
