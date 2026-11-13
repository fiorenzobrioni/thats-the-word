package com.callbackdev.thatstheword.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.WidgetCardColor

/*
 * The colours a home-screen card can wear ([WidgetCardColor]): the family's six, hex for hex
 * (`docs/adr/0004-home-screen-card.md`), each a dark ground under white ink, measured against the
 * inks they carry: white never under 7.8:1, the 75% quiet ink never under 5.2:1.
 *
 * They are not roles and they do not follow the dress: the player is choosing what colour a card
 * on their wallpaper is, which is a different act from the app's own colour. So this is a table
 * of hexes, and it lives in `theme/`, the one place hexes are allowed to.
 */

/** The ground each colour paints, at full solidity; the player's opacity thins it. */
fun widgetCardContainer(color: WidgetCardColor): Color = when (color) {
    WidgetCardColor.BLUE -> Color(0xFF0F3B6B)
    WidgetCardColor.AZURE -> Color(0xFF0F5580)
    WidgetCardColor.GREEN -> Color(0xFF17572E)
    WidgetCardColor.TEAL -> Color(0xFF0F5B5B)
    WidgetCardColor.PLUM -> Color(0xFF4A2C63)
    WidgetCardColor.CLAY -> Color(0xFF7A3320)
}

/**
 * One dress as a card needs it: both schemes at once. A card cannot ask the theme which mode it is
 * in (the launcher draws it, on a ground the app does not control), so it resolves every colour
 * itself against the ground it really has.
 */
data class WidgetDress(val lightScheme: ColorScheme, val darkScheme: ColorScheme) {
    fun scheme(dark: Boolean): ColorScheme = if (dark) darkScheme else lightScheme
}

/**
 * [palette]'s dress; with [dynamicLight] and [dynamicDark] (the wallpaper's schemes, when the
 * player turned the wallpaper's colours on) in place of its schemes. The feedback's colours keep
 * their own, as in the game.
 */
fun widgetDress(palette: AppPalette, dynamicLight: ColorScheme? = null, dynamicDark: ColorScheme? = null) = WidgetDress(
    lightScheme = dynamicLight ?: schemeFor(palette, dark = false),
    darkScheme = dynamicDark ?: schemeFor(palette, dark = true),
)

/**
 * The rim round a word found's tiles on a dark card: the right place's ink blue is as dark as the
 * card's colours, so the tiles carry a light edge, nearly white, 3:1 from every ground
 * (`ContrastTest`): crisp beside the faint frames of a word still waiting.
 */
const val CARD_TILE_RIM_ALPHA = 0.9f
