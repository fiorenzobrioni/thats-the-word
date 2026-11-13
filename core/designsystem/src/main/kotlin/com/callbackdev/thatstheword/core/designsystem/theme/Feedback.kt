package com.callbackdev.thatstheword.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.callbackdev.thatstheword.core.model.Mark

/**
 * The feedback's colours (PLANNING.md §7.2): the game's own, chosen by the owner on the board of
 * 12 Nov 2026 (proposal B, «Ink and amber»). They follow neither the dress nor the wallpaper nor
 * the theme, so a letter in its right place looks the same on every phone. Ink against amber is
 * the pair that stays apart under all three colour blindnesses, tritanopia included; the shapes
 * say the same thing again (VISION §6).
 *
 * Each ink is pinned at 4.5:1 on its ground by `ContrastTest`.
 */
object FeedbackColors {
    /** Right place: a deep ink blue. */
    val RightPlace = Color(0xFF233A7A)
    val OnRightPlace = Color(0xFFFFFFFF)

    /** Elsewhere: the family's amber. */
    val Elsewhere = Color(0xFFFDB528)
    val OnElsewhere = Color(0xFF2B1B00)
}

/** A mark's drawing: its ground, its letter's ink, its corner and its size. */
@Immutable
data class MarkStyle(val ground: Color, val ink: Color, val cornerPercent: Float, val scale: Float)

/** Square for *here*: the tile keeps its corner. */
const val RIGHT_PLACE_CORNER_PERCENT = TILE_CORNER_PERCENT.toFloat()

/** Round for *somewhere in the word*. */
const val ELSEWHERE_CORNER_PERCENT = 50f

/** An absent letter's tile sinks: a little smaller, on the quietest ground, without an outline. */
const val ABSENT_SCALE = 0.96f

/**
 * How [mark] is drawn on a tile. Absent takes the dress's quietest ground and its quiet ink (the
 * only part of the feedback that follows the dress: it is the absence of a colour).
 */
@Composable
@ReadOnlyComposable
fun markStyle(mark: Mark): MarkStyle = when (mark) {
    Mark.RIGHT_PLACE -> MarkStyle(
        FeedbackColors.RightPlace,
        FeedbackColors.OnRightPlace,
        RIGHT_PLACE_CORNER_PERCENT,
        1f,
    )

    Mark.ELSEWHERE -> MarkStyle(FeedbackColors.Elsewhere, FeedbackColors.OnElsewhere, ELSEWHERE_CORNER_PERCENT, 1f)

    Mark.ABSENT -> MarkStyle(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.onSurfaceVariant,
        RIGHT_PLACE_CORNER_PERCENT,
        ABSENT_SCALE,
    )
}

/**
 * The shared picture's colours (PLANNING.md §7.6): one look on every phone and in every chat, so
 * it is drawn on the Paper dress's light ground whatever the player wears, with the feedback's own
 * colours and the app's berry, the launcher mark's ground, as its one accent. Each ink is pinned on
 * its ground by `ContrastTest`.
 */
object PictureColors {
    val Ground = PaperLightScheme.surface
    val Ink = PaperLightScheme.onSurface
    val QuietInk = PaperLightScheme.onSurfaceVariant

    /** A letter absent: the tile sunk on the quietest ground, as on a light phone. */
    val Absent = PaperLightScheme.surfaceContainerHighest

    /** A row not played: the empty tile's frame. */
    val EmptyFrame = PaperLightScheme.outlineVariant

    /** A row spent on a hint: a band on the hint's quiet ground. */
    val HintBand = PaperLightScheme.surfaceContainerHigh

    /** The app's berry (`tools/draw_launcher_icon.py`, `BERRY`'s middle tone). */
    val Berry = Color(0xFFAE3790)
}
