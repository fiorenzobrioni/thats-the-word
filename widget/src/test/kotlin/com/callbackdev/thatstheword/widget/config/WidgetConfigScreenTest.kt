package com.callbackdev.thatstheword.widget.config

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.model.WidgetCardColor
import com.callbackdev.thatstheword.core.model.WidgetGround
import com.callbackdev.thatstheword.core.model.WidgetLook
import com.callbackdev.thatstheword.core.testing.assertAccessible
import com.callbackdev.thatstheword.core.testing.walkPage
import com.callbackdev.thatstheword.core.testing.writeScreenshot
import com.callbackdev.thatstheword.widget.WidgetSamples
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The card's settings page, drawn from its state: the card itself above, the grounds, the six
 * colours, the opacity; every choice saved as made. Checked for TalkBack's labels and a finger's
 * targets, walked at twice the text size and on an open foldable.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class WidgetConfigScreenTest {
    @get:Rule val compose = createComposeRule()

    private var look by mutableStateOf(WidgetLook())
    private var done = 0

    private fun draw(dark: Boolean = false, placed: DpSize? = null) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                WidgetConfigScreen(
                    look = look,
                    preview = WidgetSamples.model(),
                    placed = placed,
                    onLook = { look = it },
                    onOpacityDrag = { look = look.copy(opacityPct = it) },
                    onOpacityDone = {},
                    onDone = { done++ },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `a ground and a colour, chosen and kept`() {
        draw(placed = DpSize(340.dp, 189.dp))
        compose.onNodeWithText("As placed").assertIsSelected()
        compose.onNodeWithTag(WidgetConfigTags.color(WidgetCardColor.BLUE)).assertIsSelected()
        compose.onNodeWithTag(WidgetConfigTags.color(WidgetCardColor.PLUM)).performScrollTo().performClick()
        assertThat(look.cardColor).isEqualTo(WidgetCardColor.PLUM)
        compose.onNodeWithText("Plum").assertExists()
        compose.onNodeWithTag(WidgetConfigTags.ground(WidgetGround.LIGHT)).performScrollTo().performClick()
        assertThat(look.ground).isEqualTo(WidgetGround.LIGHT)
        // The colour is kept while another ground is worn.
        assertThat(look.cardColor).isEqualTo(WidgetCardColor.PLUM)
        compose.assertAccessible()
        compose.writeScreenshot("widget-config")
        compose.onNodeWithTag(WidgetConfigTags.RESET).performScrollTo().performClick()
        assertThat(look).isEqualTo(WidgetLook())
        compose.onNodeWithTag(WidgetConfigTags.DONE).performScrollTo().performClick()
        assertThat(done).isEqualTo(1)
    }

    @Test
    fun `in the dark, the whole page`() {
        draw(dark = true)
        compose.walkPage(hasTestTag(WidgetConfigTags.PAGE), "widget-config-dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw()
        compose.walkPage(hasTestTag(WidgetConfigTags.PAGE), "widget-config-it")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw()
        compose.walkPage(hasTestTag(WidgetConfigTags.PAGE), "widget-config-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw()
        compose.walkPage(hasTestTag(WidgetConfigTags.PAGE), "widget-config-foldable")
    }
}
