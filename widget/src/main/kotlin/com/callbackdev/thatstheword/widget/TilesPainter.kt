package com.callbackdev.thatstheword.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.callbackdev.thatstheword.core.designsystem.theme.ABSENT_SCALE
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.TILE_CORNER_PERCENT
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.domain.widget.CardFit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A language's word of today as the start page draws it, in tiles without letters (PLANNING.md
 * §7.8): a word found fills them with the right place's ink, square; a word missed sinks them; a
 * word waiting leaves them empty, framed; a word under way leads with the elsewhere's amber, round,
 * as the calendar's glyph for a game begun. One tile ([count] 1) is that glyph alone, where five
 * would be too small to read. The shapes say it with the colours, never the colours alone; the
 * row's words say it again.
 *
 * A bitmap, painted at the size shown: rounded corners in the game's proportion, a circle and an
 * outline are the drawings boxes cannot build, and the card's ground may be see-through.
 */
internal object TilesPainter {
    fun paint(status: DayStatus, count: Int, tileDp: Float, density: Float, palette: CardPalette): Bitmap {
        val tile = tileDp * density
        val gap = CardFit.tileGap(tileDp) * density
        val width = (CardFit.tilesWidth(tileDp, count) * density).roundToInt().coerceIn(1, MAX_SIDE_PX)
        val height = tile.roundToInt().coerceIn(1, MAX_SIDE_PX)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val radius = tile * TILE_CORNER_PERCENT / 100f
        val line = max(FRAME_MIN_DP * density, tile * FRAME_SHARE)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = line
        }
        repeat(count) { i ->
            val box = RectF(i * (tile + gap), 0f, i * (tile + gap) + tile, tile)
            when {
                status == DayStatus.SOLVED -> {
                    canvas.drawRoundRect(box, radius, radius, fill.ink(FeedbackColors.RightPlace))
                    palette.rim?.let { rim ->
                        // A little heavier than a frame: the word found is the row to see first.
                        val rimLine = line * RIM_WEIGHT
                        val edge = RectF(box).apply { inset(rimLine / 2, rimLine / 2) }
                        canvas.drawRoundRect(edge, radius, radius, stroke.ink(rim).apply { strokeWidth = rimLine })
                        stroke.strokeWidth = line
                    }
                }

                status == DayStatus.MISSED -> {
                    val sunk = RectF(box).apply { inset(tile * (1 - ABSENT_SCALE) / 2, tile * (1 - ABSENT_SCALE) / 2) }
                    canvas.drawRoundRect(sunk, radius, radius, fill.ink(palette.sunk))
                }

                status == DayStatus.IN_PROGRESS && i == 0 ->
                    canvas.drawOval(box, fill.ink(FeedbackColors.Elsewhere))

                else -> {
                    val edge = RectF(box).apply { inset(line / 2, line / 2) }
                    canvas.drawRoundRect(edge, radius, radius, stroke.ink(palette.frame))
                }
            }
        }
        return bitmap
    }

    private fun Paint.ink(color: Color): Paint = apply { this.color = color.toArgb() }

    /** An outline is never thinner than this, and grows with its tile. */
    private const val FRAME_MIN_DP = 1.25f
    private const val FRAME_SHARE = 0.07f
    private const val RIM_WEIGHT = 1.3f

    /** The launcher keeps every card's bitmaps in one parcel: nothing here needs to be larger. */
    private const val MAX_SIDE_PX = 1200
}
