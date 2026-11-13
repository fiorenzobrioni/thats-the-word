package com.callbackdev.thatstheword.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.markGround
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.designsystem.theme.rightPlaceRim
import com.callbackdev.thatstheword.core.model.Mark

/**
 * A key of the game's keyboard (PLANNING.md §7.3), in its letter's best mark so far: the mark's
 * colour, and a small drawing of its shape in the key's corner (a square for the right place, a
 * dot for elsewhere), so the keys too say it by shape as well as colour. An absent letter's key
 * sinks into the page; a letter not yet tried stands on the raised ground.
 *
 * At least [KeyHeight] tall and a row's share wide: narrower than a finger's 48dp on a phone, as
 * every phone keyboard's keys are, so the keyboard is marked [DENSE_TARGETS_TAG].
 *
 * @param label what TalkBack reads: the letter and its mark ("C, elsewhere").
 */
@Composable
fun GameKey(letter: Char, mark: Mark?, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val colors = MaterialTheme.colorScheme
    val (ground, ink) = when (mark) {
        null -> colors.surfaceContainerHighest to colors.onSurface
        Mark.RIGHT_PLACE -> FeedbackColors.RightPlace to FeedbackColors.OnRightPlace
        Mark.ELSEWHERE -> FeedbackColors.Elsewhere to FeedbackColors.OnElsewhere
        Mark.ABSENT -> colors.surfaceContainerLow to colors.onSurfaceVariant
    }
    val animatedGround by animateColorAsState(ground, ThatsTheWordMotion.effects(reduced), label = "key")
    val animatedInk by animateColorAsState(ink, ThatsTheWordMotion.effects(reduced), label = "keyInk")
    // On a dark page, the right place's key carries the tile's rim (PLANNING.md §7.2).
    val rim = if (mark == Mark.RIGHT_PLACE) rightPlaceRim() else null
    KeySurface(label = label, onClick = onClick, ground = animatedGround, rim = rim, modifier = modifier) {
        val textSize = with(LocalDensity.current) { KEY_LETTER.toSp() }
        Text(
            text = letter.toString(),
            style = ThatsTheWordTheme.type.tileLetter.copy(
                fontSize = textSize,
                lineHeight = textSize,
                fontWeight = FontWeight.SemiBold,
            ),
            color = animatedInk,
        )
        val markShape = when (mark) {
            Mark.RIGHT_PLACE -> RectangleShape
            Mark.ELSEWHERE -> CircleShape
            else -> null
        }
        if (markShape != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 5.dp)
                    .size(MARK_SIZE)
                    .background(animatedInk, markShape),
            )
        }
    }
}

/** Enter or Delete: an icon, read by its [label]. */
@Composable
fun GameActionKey(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // On the letters' untried ground, never the dress's primary: the Vivid dress's blue would sit
    // beside the right place's ink and read as a mark.
    val colors = MaterialTheme.colorScheme
    KeySurface(label = label, onClick = onClick, ground = colors.surfaceContainerHighest, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun KeySurface(
    label: String,
    onClick: () -> Unit,
    ground: Color,
    modifier: Modifier,
    rim: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(KEY_CORNER)
    Box(
        modifier
            .height(KeyHeight)
            .clip(shape)
            .markGround(ground, shape, rim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** A key's height: more than a finger's 48dp, so a row is easy to hit even where its keys are narrow. */
val KeyHeight: Dp = 54.dp

/** The gap between two keys, across and down. */
val KeyGap: Dp = 6.dp

private val KEY_CORNER = 8.dp
private val KEY_LETTER = 20.dp
private val MARK_SIZE = 6.dp
