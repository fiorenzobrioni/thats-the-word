package com.callbackdev.thatstheword.widget

import androidx.compose.ui.graphics.Color
import com.callbackdev.thatstheword.core.designsystem.theme.widgetDress
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.WidgetGround
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The ink rule (`docs/adr/0004-home-screen-card.md`), every ground, solid and see-through. */
class WidgetInkTest {
    @Test
    fun `a solid card is its own ground`() {
        assertThat(
            widgetInk(WidgetGround.COLOR, 100, night = false, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.OVER_COLOR)
        assertThat(
            widgetInk(WidgetGround.LIGHT, 100, night = true, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(
            widgetInk(WidgetGround.DARK, 50, night = false, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.ON_DARK)
        assertThat(
            widgetInk(WidgetGround.SYSTEM, 100, night = true, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.ON_DARK)
        assertThat(
            widgetInk(WidgetGround.SYSTEM, 100, night = false, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_LIGHT)
    }

    @Test
    fun `see-through, a light or dark card keeps its ink, the others ask the wallpaper`() {
        assertThat(
            widgetInk(WidgetGround.LIGHT, 20, night = true, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(
            widgetInk(WidgetGround.DARK, 20, night = false, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.ON_DARK)
        assertThat(
            widgetInk(WidgetGround.COLOR, 20, night = false, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(
            widgetInk(WidgetGround.SYSTEM, 0, night = false, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_DARK)
    }

    @Test
    fun `a word found's tiles carry a rim wherever the ground is dark, and only there`() {
        val dress = widgetDress(AppPalette.VIVID)
        assertThat(cardPaletteFor(WidgetInk.OVER_COLOR, dress).rim).isNotNull()
        assertThat(cardPaletteFor(WidgetInk.ON_DARK, dress).rim).isNotNull()
        assertThat(cardPaletteFor(WidgetInk.ON_LIGHT, dress).rim).isNull()
        assertThat(cardPaletteFor(WidgetInk.OVER_COLOR, dress).primary).isEqualTo(Color.White)
        assertThat(cardPaletteFor(WidgetInk.ON_LIGHT, dress).primary).isEqualTo(dress.lightScheme.onSurface)
    }
}
