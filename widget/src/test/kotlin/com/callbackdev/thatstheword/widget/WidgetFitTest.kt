package com.callbackdev.thatstheword.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.domain.daily.DayStanding
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Nothing the card says is cut, at any of the family's reference grants, in any of the three
 * languages, at the default text size and at twice it: every line laid out as a launcher would,
 * 5% wider than this process measures it (a maker's face is often a little wider).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
class WidgetFitTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** The longest words a day can say: every status, a two-digit streak, the longest date. */
    private val longest = WidgetSamples.today.copy(
        date = WidgetSamples.today.date.plusDays(5),
        number = 18,
        days = listOf(
            DayStanding(Language.ITALIAN, DayStatus.SOLVED, 6, 6),
            DayStanding(Language.ENGLISH, DayStatus.IN_PROGRESS, 2, 6),
            DayStanding(Language.SPANISH, DayStatus.MISSED, 6, 6),
        ),
        streak = 128,
    )

    private fun assertNothingCut() {
        for (today in listOf(WidgetSamples.today, longest)) {
            for (size in Grants.all) {
                val cut =
                    cutTexts(context, size, stretch = 1.05f) { TodayCardContent(WidgetSamples.model(today = today)) }
                assertWithMessage("cut at ${size.width} × ${size.height}").that(cut).isEmpty()
            }
        }
    }

    @Test
    fun `in English`() = assertNothingCut()

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `in Italian`() = assertNothingCut()

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xhdpi")
    fun `in Spanish`() = assertNothingCut()

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi", fontScale = 2f)
    fun `in English at twice the text size`() = assertNothingCut()

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xhdpi", fontScale = 2f)
    fun `in Spanish at twice the text size`() = assertNothingCut()

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi", fontScale = 1.3f)
    fun `in Italian at a larger text size`() = assertNothingCut()

    @Test
    fun `the card that could not read the games`() {
        for (size in Grants.all) {
            val cut =
                cutTexts(context, size, stretch = 1.05f, mayEllipsize = {
                    true
                }) { TodayCardContent(WidgetModel.unavailable()) }
            assertWithMessage("cut at ${size.width} × ${size.height}").that(cut).isEmpty()
        }
    }
}
