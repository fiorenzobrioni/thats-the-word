package com.callbackdev.thatstheword.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.theme.RIGHT_PLACE_CORNER_PERCENT
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.markStyle
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.model.Mark
import kotlinx.coroutines.delay
import kotlin.math.hypot

/**
 * A tile of the grid (PLANNING.md §7.2 to §7.4): empty, a letter typed, or a letter with its mark,
 * by colour and shape together (square for the right place, round for elsewhere, sunk for absent).
 *
 * The motion is the game's: a letter typed pops (1.08 and back) on the game's spring; a mark is
 * revealed by its colour filling the tile from its centre while the corner moves to its shape,
 * never a flip; [celebrateDelayMillis] lifts the tile and lets it settle, for a word found. Under
 * reduced motion every one of these is a fade at the same moment. Everything happens on the tile's
 * layer: no measure, no layout, no allocation a frame.
 *
 * The tile draws only: its row says it to TalkBack, as one sentence.
 *
 * @param revealDelayMillis when set, the mark is revealed that long after the tile is first drawn
 *   with it; when null, a mark is drawn at rest (a game taken up).
 */
@Composable
fun GameTile(
    letter: Char?,
    mark: Mark?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    revealDelayMillis: Long? = null,
    celebrateDelayMillis: Long? = null,
) {
    val reduced = reducedMotion()
    val colors = MaterialTheme.colorScheme
    val style = mark?.let { markStyle(it) }
    val reveal = remember(mark) { Animatable(if (mark == null || revealDelayMillis == null) 1f else 0f) }
    if (mark != null && revealDelayMillis != null) {
        LaunchedEffect(mark, reduced) {
            delay(revealDelayMillis)
            reveal.animateTo(1f, ThatsTheWordMotion.playful(reduced))
        }
    }
    // A letter typed pops; deleted, the tile is empty again at once.
    val pop = remember { Animatable(1f) }
    val typed = letter != null && mark == null
    LaunchedEffect(letter) {
        if (letter != null && mark == null && !reduced) {
            pop.snapTo(1f)
            pop.animateTo(POP_SCALE, spring(dampingRatio = 1f, stiffness = 2400f))
            pop.animateTo(1f, ThatsTheWordMotion.playful())
        }
    }
    val lift = remember { Animatable(0f) }
    if (celebrateDelayMillis != null) {
        LaunchedEffect(Unit) {
            delay(celebrateDelayMillis)
            if (!reduced) {
                lift.animateTo(1f, spring(dampingRatio = 1f, stiffness = 900f))
                lift.animateTo(0f, ThatsTheWordMotion.playful())
            }
        }
    }

    val ground = if (typed) colors.surfaceContainerHigh else colors.surfaceContainerLow
    val outline = if (typed) colors.outline else colors.outlineVariant
    val beforeInk = colors.onSurface
    val currentStyle by rememberUpdatedState(style)
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val p = reveal.value
                val s = currentStyle
                val corner = if (s == null) {
                    RIGHT_PLACE_CORNER_PERCENT
                } else if (reduced) {
                    s.cornerPercent
                } else {
                    RIGHT_PLACE_CORNER_PERCENT + (s.cornerPercent - RIGHT_PLACE_CORNER_PERCENT) * p.coerceIn(0f, 1f)
                }
                shape = RoundedCornerShape(CornerSize(corner.coerceIn(0f, 50f)))
                clip = true
                val markScale = if (s == null) 1f else 1f + (s.scale - 1f) * p.coerceIn(0f, 1f)
                val scale = markScale * pop.value
                scaleX = scale
                scaleY = scale
                translationY = -lift.value * LIFT.toPx()
            }
            .drawBehind {
                drawRect(ground)
                val p = reveal.value
                val s = currentStyle
                val outlineShape = RoundedCornerShape(CornerSize(RIGHT_PLACE_CORNER_PERCENT))
                    .createOutline(this.size, layoutDirection, this)
                if (s == null || p < 1f) drawBorder(outlineShape, outline, BORDER.toPx())
                if (s != null) {
                    if (reduced) {
                        drawRect(s.ground, alpha = p.coerceIn(0f, 1f))
                    } else {
                        val radius = hypot(this.size.width, this.size.height) / 2f * p.coerceAtLeast(0f)
                        drawCircle(s.ground, radius, Offset(this.size.width / 2f, this.size.height / 2f))
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            // Sized by the tile, not by the text size: a tile cannot grow without breaking its row,
            // and its letter already stands larger than any body text at twice its size.
            val letterSize = with(LocalDensity.current) { (size * LETTER_TO_TILE).toSp() }
            val ink = if (style == null) beforeInk else lerp(beforeInk, style.ink, reveal.value.coerceIn(0f, 1f))
            Text(
                text = letter.toString(),
                style = ThatsTheWordTheme.type.tileLetter.copy(fontSize = letterSize, lineHeight = letterSize),
                color = ink,
            )
        }
    }
}

private fun DrawScope.drawBorder(outline: Outline, color: Color, width: Float) {
    // The stroke is centred on the edge and the tile clips it: twice the width, half of it seen.
    drawOutline(outline, color, style = Stroke(width * 2f))
}

/** The letter's height as a share of its tile's side. */
private const val LETTER_TO_TILE = 0.5f

/** A letter typed: the tile pops this much and comes back. */
private const val POP_SCALE = 1.08f

/** A word found: how high each tile of its row lifts. */
private val LIFT = 10.dp

private val BORDER = 2.dp
