package com.callbackdev.thatstheword.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.callbackdev.thatstheword.core.designsystem.theme.CARD_TILE_RIM_ALPHA
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.PictureColors
import com.callbackdev.thatstheword.core.designsystem.theme.schemeFor
import com.callbackdev.thatstheword.core.designsystem.theme.widgetCardContainer
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.WidgetCardColor
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Every ink the app sets text in reads on every ground it stands on, in both dresses and both
 * themes: WCAG's 4.5:1 for text, the Accessibility Scanner's threshold. The generated schemes are
 * never hand-edited, so this pins the pairs the screens use. A screen that sets text on a new pair
 * adds it here; the feedback's colours have their own pairs.
 */
class ContrastTest {
    private val dresses = AppPalette.entries.flatMap { palette ->
        listOf(false, true).map { dark ->
            Dress("$palette ${if (dark) "dark" else "light"}", schemeFor(palette, dark))
        }
    }

    private class Dress(val name: String, val scheme: ColorScheme)

    @Test
    fun `text reads on every ground it is set on`() {
        for (dress in dresses) {
            val s = dress.scheme
            val grounds = listOf(
                "surface" to s.surface,
                "surfaceContainerLow" to s.surfaceContainerLow,
                "surfaceContainer" to s.surfaceContainer,
                // A tile before its feedback: the start page's word, the letters being typed.
                "surfaceContainerHigh" to s.surfaceContainerHigh,
                "surfaceContainerHighest" to s.surfaceContainerHighest,
            )
            for ((ground, color) in grounds) {
                expect(dress, "onSurface on $ground", s.onSurface, color)
                expect(dress, "onSurfaceVariant on $ground", s.onSurfaceVariant, color)
                expect(dress, "primary on $ground", s.primary, color)
                expect(dress, "error on $ground", s.error, color)
            }
            expect(dress, "onPrimary on primary", s.onPrimary, s.primary)
            expect(dress, "onPrimaryContainer on primaryContainer", s.onPrimaryContainer, s.primaryContainer)
            expect(dress, "onSecondaryContainer on secondaryContainer", s.onSecondaryContainer, s.secondaryContainer)
            expect(dress, "onErrorContainer on errorContainer", s.onErrorContainer, s.errorContainer)
            expect(dress, "inverseOnSurface on inverseSurface", s.inverseOnSurface, s.inverseSurface)
        }
    }

    @Test
    fun `every letter reads on its mark`() {
        // The feedback's own colours, the same in every dress and theme (PLANNING.md §7.2).
        expect(null, "a letter in its right place", FeedbackColors.OnRightPlace, FeedbackColors.RightPlace)
        expect(null, "a letter elsewhere", FeedbackColors.OnElsewhere, FeedbackColors.Elsewhere)
        for (dress in dresses) {
            val s = dress.scheme
            // An absent letter's tile, and an absent letter's key, sunk into the page.
            expect(dress, "an absent letter", s.onSurfaceVariant, s.surfaceContainerHighest)
            expect(dress, "an absent key", s.onSurfaceVariant, s.surfaceContainerLow)
            // A key not yet tried, and Enter and Delete.
            expect(dress, "an untried key", s.onSurface, s.surfaceContainerHighest)
        }
    }

    @Test
    fun `the shared picture reads`() {
        // One look on every phone (PLANNING.md §7.6): its words and its numbers on its ground.
        expect(null, "the picture's ink", PictureColors.Ink, PictureColors.Ground)
        expect(null, "the picture's quiet ink", PictureColors.QuietInk, PictureColors.Ground)
    }

    @Test
    fun `the home-screen card's inks read on each of its colours`() {
        // The card's colour grounds carry white, and its quiet ink at 75% (docs/adr/0004).
        for (color in WidgetCardColor.entries) {
            val ground = widgetCardContainer(color)
            expect(null, "white on the $color card", Color.White, ground)
            expect(null, "the quiet ink on the $color card", Color.White.copy(alpha = 0.75f), ground)
            // A word found's tiles are the right place's ink blue, as dark as the cards: their
            // rim (the card's tile rim, `TilesPainter`) stands at 3:1 from the ground, the
            // contrast a shape needs.
            val rim = Color.White.copy(alpha = CARD_TILE_RIM_ALPHA).compositeOver(ground)
            assertWithMessage("the found tiles' rim on the $color card")
                .that(contrast(rim, ground))
                .isAtLeast(3.0)
        }
    }

    private fun expect(dress: Dress?, what: String, ink: Color, ground: Color) {
        val ratio = contrast(ink.compositeOver(ground), ground)
        assertWithMessage(
            "${dress?.name ?: "feedback"}: $what is ${"%.2f".format(ratio)}:1",
        ).that(ratio).isAtLeast(TEXT_CONTRAST)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val TEXT_CONTRAST = 4.5
    }
}
