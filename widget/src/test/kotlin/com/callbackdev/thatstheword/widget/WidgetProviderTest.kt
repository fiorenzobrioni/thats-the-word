package com.callbackdev.thatstheword.widget

import androidx.compose.ui.graphics.toArgb
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.widgetCardContainer
import com.callbackdev.thatstheword.core.model.WidgetCardColor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * What no other test reaches, because the launcher reads it: the provider (PLANNING.md §7.8:
 * `updatePeriodMillis` 0, resizable both ways from one cell, no maximum, the settings optional)
 * and its static preview, which may only use views `RemoteViews` can inflate.
 */
class WidgetProviderTest {
    private val info = attributes(File("src/main/res/xml/widget_today_info.xml").readText())
    private val layout = File("src/main/res/layout")

    private val remoteViewsTags = setOf(
        "FrameLayout", "LinearLayout", "RelativeLayout", "GridLayout",
        "TextView", "ImageView", "Button", "ImageButton", "ProgressBar",
    )

    private fun attributes(text: String): Map<String, String> =
        Regex("""android:(\w+)="([^"]*)"""").findAll(text).associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `the card is pushed, resizable from one cell, with no maximum, the settings optional`() {
        assertThat(info["updatePeriodMillis"]).isEqualTo("0")
        assertThat(info["minResizeWidth"]).isEqualTo("40dp")
        assertThat(info["minResizeHeight"]).isEqualTo("40dp")
        assertThat(info["resizeMode"]).isEqualTo("horizontal|vertical")
        assertThat(info["widgetCategory"]).isEqualTo("home_screen")
        assertThat(info["widgetFeatures"]).isEqualTo("reconfigurable|configuration_optional")
        assertThat(info["configure"]).isEqualTo("com.callbackdev.thatstheword.widget.config.WidgetConfigActivity")
        assertThat(info.keys).containsNoneOf("maxResizeWidth", "maxResizeHeight")
        assertThat(info["targetCellWidth"] to info["targetCellHeight"]).isEqualTo("4" to "2")
    }

    @Test
    fun `the static preview exists and inflates in RemoteViews`() {
        val name = checkNotNull(info["previewLayout"]).removePrefix("@layout/")
        val preview = File(layout, "$name.xml")
        assertThat(preview.isFile).isTrue()
        val tags = Regex("""<([A-Za-z][A-Za-z0-9_.]*)[\s>]""").findAll(preview.readText())
            .map { it.groupValues[1] }
            .filterNot { it == "xml" }
            .toSet()
        assertThat(remoteViewsTags).containsAtLeastElementsIn(tags)
    }

    @Test
    fun `the static preview's colours are the card's and the feedback's`() {
        val colors = File("src/main/res/values/colors.xml").readText()
        fun hex(argb: Int) = "#%08X".format(argb)
        assertThat(
            colors,
        ).contains(
            "<color name=\"widget_preview_card\">${hex(widgetCardContainer(WidgetCardColor.BLUE).toArgb())}</color>",
        )
        assertThat(
            colors,
        ).contains("<color name=\"widget_preview_found\">${hex(FeedbackColors.RightPlace.toArgb())}</color>")
        assertThat(
            colors,
        ).contains("<color name=\"widget_preview_amber\">${hex(FeedbackColors.Elsewhere.toArgb())}</color>")
    }
}
