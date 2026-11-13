package com.callbackdev.thatstheword.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The few icons the game draws, on Material's 24 grid, in rounded strokes like the family's: drawn
 * here rather than pulled from an icon library for a dozen shapes. Tinted by their caller.
 */
object GameIcons {
    private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()

    /** A stroke filled too: the one icon that says "on" by its ground (the notebook's mark kept). */
    private fun filledIcon(
        name: String,
        block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()

    /** Enter: the return arrow. */
    val Enter: ImageVector by lazy {
        icon("Enter") {
            moveTo(19f, 5f)
            verticalLineTo(12f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 17f, y1 = 14f)
            horizontalLineTo(6f)
            moveTo(10f, 10f)
            lineTo(6f, 14f)
            lineTo(10f, 18f)
        }
    }

    /** Delete: the backspace tag with its cross. */
    val Delete: ImageVector by lazy {
        icon("Delete") {
            moveTo(9f, 5f)
            horizontalLineTo(19f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 21f, y1 = 7f)
            verticalLineTo(17f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19f, y1 = 19f)
            horizontalLineTo(9f)
            lineTo(3f, 12f)
            close()
            moveTo(11.5f, 9.5f)
            lineTo(16.5f, 14.5f)
            moveTo(16.5f, 9.5f)
            lineTo(11.5f, 14.5f)
        }
    }

    /** Back: the arrow of a page that returns. */
    val Back: ImageVector by lazy {
        icon("Back") {
            moveTo(19f, 12f)
            horizontalLineTo(5f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    /** Share: the platform's three linked dots. */
    val Share: ImageVector by lazy {
        icon("Share") {
            moveTo(8.6f, 10.6f)
            lineTo(15.4f, 6.9f)
            moveTo(8.6f, 13.4f)
            lineTo(15.4f, 17.1f)
            moveTo(21f, 5f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15f, y1 = 5f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 21f, y1 = 5f)
            moveTo(9f, 12f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 3f, y1 = 12f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 9f, y1 = 12f)
            moveTo(21f, 19f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15f, y1 = 19f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 21f, y1 = 19f)
        }
    }

    /** The month before, in the calendar: a chevron. */
    val Previous: ImageVector by lazy {
        icon("Previous") {
            moveTo(15f, 6f)
            lineTo(9f, 12f)
            lineTo(15f, 18f)
        }
    }

    /** The month after, in the calendar. */
    val Next: ImageVector by lazy {
        icon("Next") {
            moveTo(9f, 6f)
            lineTo(15f, 12f)
            lineTo(9f, 18f)
        }
    }

    /** The hints: a light bulb. */
    val Hint: ImageVector by lazy {
        icon("Hint") {
            moveTo(9f, 17f)
            lineTo(8.6f, 14.8f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15.4f, y1 = 14.8f)
            lineTo(15f, 17f)
            close()
            moveTo(10f, 20.5f)
            horizontalLineTo(14f)
        }
    }

    /** The translation hint: a word going one way and coming back in another language. */
    val Translation: ImageVector by lazy {
        icon("Translation") {
            moveTo(4f, 8f)
            horizontalLineTo(18f)
            moveTo(15f, 5f)
            lineTo(18f, 8f)
            lineTo(15f, 11f)
            moveTo(20f, 16f)
            horizontalLineTo(6f)
            moveTo(9f, 13f)
            lineTo(6f, 16f)
            lineTo(9f, 19f)
        }
    }

    /** The meaning: an open book. */
    val Definition: ImageVector by lazy {
        icon("Definition") {
            moveTo(12f, 6.5f)
            curveTo(10f, 5f, 7f, 4.5f, 3f, 5f)
            verticalLineTo(18.5f)
            curveTo(7f, 18f, 10f, 18.5f, 12f, 20f)
            curveTo(14f, 18.5f, 17f, 18f, 21f, 18.5f)
            verticalLineTo(5f)
            curveTo(17f, 4.5f, 14f, 5f, 12f, 6.5f)
            close()
            moveTo(12f, 6.5f)
            verticalLineTo(20f)
        }
    }

    /** The letter hint: a tile with a letter in it. */
    val Letter: ImageVector by lazy {
        icon("Letter") {
            moveTo(6f, 3.5f)
            horizontalLineTo(18f)
            arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 20.5f, y1 = 6f)
            verticalLineTo(18f)
            arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18f, y1 = 20.5f)
            horizontalLineTo(6f)
            arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 3.5f, y1 = 18f)
            verticalLineTo(6f)
            arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 6f, y1 = 3.5f)
            close()
            moveTo(8.5f, 16.5f)
            lineTo(12f, 7.5f)
            lineTo(15.5f, 16.5f)
            moveTo(9.8f, 13.2f)
            horizontalLineTo(14.2f)
        }
    }

    private fun androidx.compose.ui.graphics.vector.PathBuilder.bookmark() {
        moveTo(7f, 3f)
        horizontalLineTo(17f)
        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18f, y1 = 4f)
        verticalLineTo(20.5f)
        lineTo(12f, 16.5f)
        lineTo(6f, 20.5f)
        verticalLineTo(4f)
        arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 7f, y1 = 3f)
        close()
    }

    /** The notebook, and a word not in it yet: a bookmark. */
    val Notebook: ImageVector by lazy { icon("Notebook") { bookmark() } }

    /** A word kept in the notebook: the bookmark filled. */
    val NotebookKept: ImageVector by lazy { filledIcon("NotebookKept") { bookmark() } }

    /** The notebook's search: a lens. */
    val Search: ImageVector by lazy {
        icon("Search") {
            moveTo(17.5f, 10.5f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 3.5f, y1 = 10.5f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 17.5f, y1 = 10.5f)
            moveTo(15.5f, 15.5f)
            lineTo(20.5f, 20.5f)
        }
    }

    /** What clears a search: a cross. */
    val Clear: ImageVector by lazy {
        icon("Clear") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }

    /** A milestone reached, a thing done: a check. */
    val Check: ImageVector by lazy {
        icon("Check") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 7f)
        }
    }

    /** A copy saved: an arrow down into a tray. */
    val SaveCopy: ImageVector by lazy {
        icon("SaveCopy") {
            moveTo(12f, 4f)
            verticalLineTo(14.5f)
            moveTo(7.5f, 10f)
            lineTo(12f, 14.5f)
            lineTo(16.5f, 10f)
            moveTo(4.5f, 15f)
            verticalLineTo(18f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6.5f, y1 = 20f)
            horizontalLineTo(17.5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 19.5f, y1 = 18f)
            verticalLineTo(15f)
        }
    }

    /** A copy brought back: an arrow up out of the tray. */
    val OpenCopy: ImageVector by lazy {
        icon("OpenCopy") {
            moveTo(12f, 14.5f)
            verticalLineTo(4f)
            moveTo(7.5f, 8.5f)
            lineTo(12f, 4f)
            lineTo(16.5f, 8.5f)
            moveTo(4.5f, 15f)
            verticalLineTo(18f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6.5f, y1 = 20f)
            horizontalLineTo(17.5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 19.5f, y1 = 18f)
            verticalLineTo(15f)
        }
    }
}
