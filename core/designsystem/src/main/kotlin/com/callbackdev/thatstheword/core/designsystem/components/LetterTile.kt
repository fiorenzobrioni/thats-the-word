package com.callbackdev.thatstheword.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.theme.TILE_CORNER_PERCENT
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import kotlinx.coroutines.delay

/**
 * One letter on its tile, before any feedback: the start page's word, and from Phase 3 the grid's
 * letters as they are typed. The feedback's colours and shapes are not drawn here yet: they are
 * the owner's to choose (PLANNING.md §7, §15 Open).
 *
 * @param appearDelayMillis when set, the tile arrives that long after it is first drawn, rising
 *   and settling with the game's spring; under reduced motion it fades in, at the same moment.
 */
@Composable
fun LetterTile(letter: Char, modifier: Modifier = Modifier, size: Dp = 56.dp, appearDelayMillis: Long? = null) {
    val reduced = reducedMotion()
    val shape = RoundedCornerShape(TILE_CORNER_PERCENT)
    val progress = remember { Animatable(if (appearDelayMillis == null) 1f else 0f) }
    if (appearDelayMillis != null) {
        LaunchedEffect(reduced) {
            delay(appearDelayMillis)
            progress.animateTo(1f, ThatsTheWordMotion.playful(reduced))
        }
    }
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                if (!reduced) {
                    val scale = 0.6f + 0.4f * p
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - p) * size.toPx() * 0.35f
                }
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        // Sized by the tile, not by the text size: the letter already stands larger than any
        // body text at twice its size, and a tile cannot grow without breaking its row.
        val letterSize = with(LocalDensity.current) { (size * LETTER_TO_TILE).toSp() }
        Text(
            text = letter.toString(),
            style = ThatsTheWordTheme.type.tileLetter.copy(fontSize = letterSize, lineHeight = letterSize),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The letter's height as a share of its tile's side. */
private const val LETTER_TO_TILE = 0.5f
