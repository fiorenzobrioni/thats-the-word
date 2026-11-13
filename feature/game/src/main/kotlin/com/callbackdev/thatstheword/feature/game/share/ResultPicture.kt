package com.callbackdev.thatstheword.feature.game.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.callbackdev.thatstheword.core.designsystem.theme.ABSENT_SCALE
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.PictureColors
import com.callbackdev.thatstheword.core.designsystem.theme.TILE_CORNER_PERCENT
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.designsystem.R as DesignR

/**
 * What the shared picture says (PLANNING.md §7.6, VISION §11): the game's words, already in the
 * interface's language, the name and the eyebrow in the capitals the caller drew for its locale,
 * and its rows' marks, never a letter.
 *
 * @property rows each guess's marks, in order.
 * @property hintRows the rows spent on hints: drawn at the grid's foot, as the game draws them.
 * @property attempts the grid's rows; those not played are drawn as empty tiles, so "4/6" is seen.
 */
data class PictureSpec(
    val name: String,
    val eyebrow: String,
    val date: String,
    val rows: List<List<Mark>>,
    val hintRows: Int,
    val length: Int,
    val attempts: Int,
    val score: String,
    val extras: String?,
)

/**
 * The shared picture (PLANNING.md §7.6): the grid's shapes and colours, the date and the language,
 * no letter. Drawn on the Paper dress's light ground whatever the player wears, at a fixed size
 * (1080 × 1350, the portrait that every chat shows whole), so it looks the same wherever it lands.
 *
 * The marks keep their shapes as in the grid: an ink square for the right place, an amber disc for
 * elsewhere, a sunk tile for absent (VISION §6). A row spent on a hint is a quiet band, a row not
 * played an empty frame.
 */
object ResultPicture {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val MARGIN = 96f
    private const val MAX_TILE = 112f
    private const val GAP_SHARE = 0.13f

    fun draw(context: Context, spec: PictureSpec): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        canvas.drawColor(PictureColors.Ground.toArgb())
        val face = runCatching { ResourcesCompat.getFont(context, DesignR.font.google_sans_variable) }.getOrNull()
            ?: Typeface.SANS_SERIF

        val name = text(face, weight = 700, size = 50f, color = PictureColors.Ink, tracking = 0.12f)
        val eyebrow = text(face, weight = 500, size = 30f, color = PictureColors.QuietInk, tracking = 0.16f)
        val date = text(face, weight = 400, size = 40f, color = PictureColors.Ink)
        val score = text(face, weight = 700, size = 96f, color = PictureColors.Ink, tracking = -0.02f).apply {
            fontFeatureSettings = "tnum"
        }
        val extras = text(face, weight = 500, size = 32f, color = PictureColors.QuietInk)

        val tile = minOf(MAX_TILE, (WIDTH - 2 * MARGIN) / (spec.length + (spec.length - 1) * GAP_SHARE))
        val gap = tile * GAP_SHARE
        val gridWidth = spec.length * tile + (spec.length - 1) * gap
        val gridHeight = spec.attempts * tile + (spec.attempts - 1) * gap

        // The block's height first, so it stands in the picture's middle whatever the grid's size.
        val header = lineHeight(name) + 28f + ACCENT_HEIGHT + 44f + lineHeight(eyebrow) + 12f + lineHeight(date)
        val footer = lineHeight(score) + if (spec.extras != null) 8f + lineHeight(extras) else 0f
        val block = header + GRID_SPACE + gridHeight + GRID_SPACE + footer
        var y = ((HEIGHT - block) / 2f).coerceAtLeast(MARGIN / 2)
        val centre = WIDTH / 2f

        y = line(canvas, spec.name, name, centre, y)
        y += 28f
        val accent = RectF(centre - ACCENT_WIDTH / 2, y, centre + ACCENT_WIDTH / 2, y + ACCENT_HEIGHT)
        canvas.drawRoundRect(accent, ACCENT_HEIGHT / 2, ACCENT_HEIGHT / 2, fill(PictureColors.Berry))
        y += ACCENT_HEIGHT + 44f
        y = line(canvas, spec.eyebrow, eyebrow, centre, y)
        y += 12f
        y = line(canvas, spec.date, date, centre, y)
        y += GRID_SPACE

        val left = centre - gridWidth / 2
        for (row in 0 until spec.attempts) {
            val top = y + row * (tile + gap)
            val marks = spec.rows.getOrNull(row)
            val hint = row >= spec.attempts - spec.hintRows
            when {
                marks != null -> marks.forEachIndexed { i, mark ->
                    tile(canvas, mark, left + i * (tile + gap), top, tile)
                }

                hint -> {
                    val band = RectF(left, top, left + gridWidth, top + tile)
                    val corner = tile * TILE_CORNER_PERCENT / 100f
                    canvas.drawRoundRect(band, corner, corner, fill(PictureColors.HintBand))
                }

                else -> repeat(spec.length) { i -> empty(canvas, left + i * (tile + gap), top, tile) }
            }
        }
        y += gridHeight + GRID_SPACE
        y = line(canvas, spec.score, score, centre, y)
        spec.extras?.let {
            y += 8f
            line(canvas, it, extras, centre, y)
        }
        return bitmap
    }

    /** A mark's tile, as the grid draws it at rest: by its colour and its shape together. */
    private fun tile(canvas: Canvas, mark: Mark, x: Float, y: Float, side: Float) {
        when (mark) {
            Mark.RIGHT_PLACE -> {
                val corner = side * TILE_CORNER_PERCENT / 100f
                canvas.drawRoundRect(RectF(x, y, x + side, y + side), corner, corner, fill(FeedbackColors.RightPlace))
            }

            Mark.ELSEWHERE -> canvas.drawCircle(x + side / 2, y + side / 2, side / 2, fill(FeedbackColors.Elsewhere))

            Mark.ABSENT -> {
                val inset = side * (1 - ABSENT_SCALE) / 2
                val sunk = side - 2 * inset
                val corner = sunk * TILE_CORNER_PERCENT / 100f
                canvas.drawRoundRect(
                    RectF(x + inset, y + inset, x + inset + sunk, y + inset + sunk),
                    corner,
                    corner,
                    fill(PictureColors.Absent),
                )
            }
        }
    }

    private fun empty(canvas: Canvas, x: Float, y: Float, side: Float) {
        val stroke = side * 0.035f
        val corner = side * TILE_CORNER_PERCENT / 100f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = PictureColors.EmptyFrame.toArgb()
        }
        val half = stroke / 2
        canvas.drawRoundRect(RectF(x + half, y + half, x + side - half, y + side - half), corner, corner, paint)
    }

    /**
     * A line of text centred at [y]'s top, made smaller until it fits the picture's width with its
     * margins: a long language name or date never runs off the edge. Returns the next line's top.
     */
    private fun line(canvas: Canvas, text: String, paint: Paint, centre: Float, y: Float): Float {
        val room = WIDTH - 2 * MARGIN
        val fitted = Paint(paint)
        while (fitted.measureText(text) > room && fitted.textSize > MIN_TEXT) fitted.textSize -= 1f
        val metrics = paint.fontMetrics
        canvas.drawText(text, centre, y - metrics.ascent, fitted)
        return y + lineHeight(paint)
    }

    private fun lineHeight(paint: Paint): Float = paint.fontMetrics.let { it.descent - it.ascent }

    private fun text(face: Typeface, weight: Int, size: Float, color: Color, tracking: Float = 0f) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            // The bundled face is a variable font: its weight is an axis, not a second file.
            fontVariationSettings = "'wght' $weight"
            textSize = size
            letterSpacing = tracking
            textAlign = Paint.Align.CENTER
            this.color = color.toArgb()
        }

    private fun fill(color: Color) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }

    private const val ACCENT_WIDTH = 72f
    private const val ACCENT_HEIGHT = 8f
    private const val GRID_SPACE = 64f
    private const val MIN_TEXT = 18f
}
