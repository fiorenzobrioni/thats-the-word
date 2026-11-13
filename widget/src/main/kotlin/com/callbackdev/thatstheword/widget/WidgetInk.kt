package com.callbackdev.thatstheword.widget

import com.callbackdev.thatstheword.core.model.WidgetGround

/**
 * Which set of inks a card writes with, named after the ground it is written on
 * (`docs/adr/0004-home-screen-card.md`), kept pure so a test pins it: [OVER_COLOR] is the white
 * pair every card colour is picked dark enough to carry; the other two are the dresses' own.
 */
enum class WidgetInk {
    OVER_COLOR,
    ON_LIGHT,
    ON_DARK,
    ;

    /** Whether the ground is dark, so the tiles and the dress's colours pick the set for dark. */
    val darkGround: Boolean get() = this != ON_LIGHT
}

/** Below this solidity the card stops being the ink's ground. */
const val INK_TRUST_FLOOR_PCT: Int = 50

/**
 * The whole ink rule. Above [INK_TRUST_FLOOR_PCT] the card is its own ground. Below it the card is
 * see-through: a light or dark card is the player naming an ink, so it keeps deciding; the phone's
 * card and a colour hand the question to the wallpaper, whose hint is read as the affirmative
 * signal it is (dark ink only where the system says the ground is bright). [night] is the phone's
 * mode, which only the phone's card at full solidity asks for.
 */
fun widgetInk(ground: WidgetGround, opacityPct: Int, night: Boolean, wallpaperCarriesDarkInk: Boolean): WidgetInk {
    if (opacityPct < INK_TRUST_FLOOR_PCT) {
        return when (ground) {
            WidgetGround.LIGHT -> WidgetInk.ON_LIGHT

            WidgetGround.DARK -> WidgetInk.ON_DARK

            WidgetGround.SYSTEM, WidgetGround.COLOR ->
                if (wallpaperCarriesDarkInk) WidgetInk.ON_LIGHT else WidgetInk.ON_DARK
        }
    }
    return when (ground) {
        WidgetGround.COLOR -> WidgetInk.OVER_COLOR
        WidgetGround.LIGHT -> WidgetInk.ON_LIGHT
        WidgetGround.DARK -> WidgetInk.ON_DARK
        WidgetGround.SYSTEM -> if (night) WidgetInk.ON_DARK else WidgetInk.ON_LIGHT
    }
}
