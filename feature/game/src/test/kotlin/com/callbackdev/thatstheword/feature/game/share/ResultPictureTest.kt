package com.callbackdev.thatstheword.feature.game.share

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.PictureColors
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Mark.ABSENT
import com.callbackdev.thatstheword.core.model.Mark.ELSEWHERE
import com.callbackdev.thatstheword.core.model.Mark.RIGHT_PLACE
import com.callbackdev.thatstheword.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * The shared picture (PLANNING.md §7.6): drawn at its fixed size on its own ground, the marks in
 * their colours, no letter; its words in the interface's language. Each picture is written to
 * `build/screenshots` for a person to look at.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class ResultPictureTest {
    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val won = ResultFacts(
        language = Language.ITALIAN,
        mode = GameMode.Daily(LocalDate.of(2026, 11, 13), 13),
        date = LocalDate.of(2026, 11, 13),
        rows = listOf(
            listOf(ABSENT, ELSEWHERE, ABSENT, ABSENT, RIGHT_PLACE),
            listOf(RIGHT_PLACE, ABSENT, ELSEWHERE, ABSENT, RIGHT_PLACE),
            listOf(RIGHT_PLACE, RIGHT_PLACE, RIGHT_PLACE, RIGHT_PLACE, RIGHT_PLACE),
        ),
        hints = 1,
        hard = true,
        length = 5,
        attempts = 6,
        attemptsUsed = 4,
        won = true,
    )

    private val lost = ResultFacts(
        language = Language.SPANISH,
        mode = GameMode.Free(Level.ADVANCED),
        date = LocalDate.of(2026, 11, 13),
        rows = List(6) { i ->
            List(5) { j ->
                if ((i + j) % 3 ==
                    0
                ) {
                    ELSEWHERE
                } else if (j == i % 5) {
                    RIGHT_PLACE
                } else {
                    ABSENT
                }
            }
        },
        hints = 0,
        hard = false,
        length = 5,
        attempts = 6,
        attemptsUsed = 6,
        won = false,
    )

    private fun picture(facts: ResultFacts): Bitmap {
        var spec: PictureSpec? = null
        compose.setContent { ThatsTheWordTheme { spec = pictureSpec(facts) } }
        compose.waitForIdle()
        return ResultPicture.draw(context, checkNotNull(spec))
    }

    private fun write(bitmap: Bitmap, name: String) {
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun Bitmap.has(color: androidx.compose.ui.graphics.Color): Boolean {
        val argb = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
        )
        for (y in 0 until height step 4) for (x in 0 until width step 4) if (getPixel(x, y) == argb) return true
        return false
    }

    @Test
    fun `a daily won, with Hard and a hint, says so without a letter`() {
        val spec = run {
            var shown: PictureSpec? = null
            compose.setContent { ThatsTheWordTheme { shown = pictureSpec(won) } }
            compose.waitForIdle()
            checkNotNull(shown)
        }
        assertThat(spec.name).isEqualTo("THAT’S THE WORD")
        assertThat(spec.eyebrow).isEqualTo("ITALIAN · #13")
        assertThat(spec.date).isEqualTo("Friday 13 November 2026")
        assertThat(spec.score).isEqualTo("4/6")
        assertThat(spec.extras).isEqualTo("Hard · 1 hint")
        val bitmap = ResultPicture.draw(context, spec)
        assertThat(bitmap.width).isEqualTo(ResultPicture.WIDTH)
        assertThat(bitmap.height).isEqualTo(ResultPicture.HEIGHT)
        // The same ground whatever the phone wears.
        assertThat(bitmap.getPixel(2, 2)).isEqualTo(PictureColors.Ground.toArgb())
        assertThat(bitmap.has(FeedbackColors.RightPlace)).isTrue()
        assertThat(bitmap.has(FeedbackColors.Elsewhere)).isTrue()
        assertThat(bitmap.has(PictureColors.Berry)).isTrue()
        write(bitmap, "share-picture")
    }

    @Test
    fun `a free word not found says X`() {
        var spec: PictureSpec? = null
        compose.setContent { ThatsTheWordTheme { spec = pictureSpec(lost) } }
        compose.waitForIdle()
        assertThat(spec?.score).isEqualTo("X/6")
        assertThat(spec?.eyebrow).isEqualTo("SPANISH · FREE PLAY")
        assertThat(spec?.extras).isNull()
        write(ResultPicture.draw(context, checkNotNull(spec)), "share-picture-lost")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        write(picture(won), "share-picture-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `in Spanish`() {
        write(picture(won.copy(language = Language.ENGLISH)), "share-picture-es")
    }

    @Test
    fun `the sheet shows the picture as it will be sent`() {
        compose.setContent { ThatsTheWordTheme { ShareSheet(won, text = { "text" }, onDismiss = {}) } }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag(ShareTags.PICTURE)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("The picture to share: your grid, without the word.").assertIsDisplayed()
        compose.onNodeWithTag(ShareTags.PICTURE_BUTTON).assertIsDisplayed()
        compose.onNodeWithTag(ShareTags.TEXT_BUTTON).assertIsDisplayed()
        compose.writeScreenshot("share-sheet")
    }
}
