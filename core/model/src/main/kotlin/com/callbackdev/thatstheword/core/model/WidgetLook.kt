package com.callbackdev.thatstheword.core.model

/**
 * The six colours a card can wear (PLANNING.md §7.8), the family's: their hexes are the design
 * system's (`WidgetPalette.kt`), the one place hexes live.
 */
enum class WidgetCardColor { BLUE, AZURE, GREEN, TEAL, PLUM, CLAY }

/**
 * What a card is painted on: the app's light or dark surface, whichever the phone is in, or one of
 * the six colours ([WidgetLook.cardColor]).
 */
enum class WidgetGround { LIGHT, DARK, SYSTEM, COLOR }

/**
 * One card's look, chosen in its settings and kept per card (PLANNING.md §5, the `widgets` file,
 * not backed up: a restore renews the cards' ids), so the same card can sit on a home screen twice,
 * dressed twice. The default is the family's: a solid blue card, worn before anybody configures
 * anything.
 *
 * @property opacityPct the ground's opacity, 0 to 100; the ink never thins with it.
 * @property cardColor kept while another ground is picked, so coming back finds it.
 */
data class WidgetLook(
    val ground: WidgetGround = WidgetGround.COLOR,
    val opacityPct: Int = DEFAULT_OPACITY,
    val cardColor: WidgetCardColor = WidgetCardColor.BLUE,
) {
    companion object {
        /** Solid: below half, the ink has to ask the wallpaper, a different conversation. */
        const val DEFAULT_OPACITY: Int = 100
    }
}
