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
 * here rather than pulled from an icon library for six shapes. Tinted by their caller.
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
}
