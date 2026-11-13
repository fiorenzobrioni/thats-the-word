package com.callbackdev.thatstheword.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion

/**
 * A choice among two to four (the languages, the modes, a setting's options): a track with the chosen one's ink
 * sliding under it on the family's spring; in place under reduced motion. Each option is a tab for
 * TalkBack, at least 48dp high, its words wrapping at twice the text size.
 */
@Composable
fun <T> ChoiceSegments(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    tag: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val reduced = reducedMotion()
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var width by remember { mutableIntStateOf(0) }
    val segment = with(density) { (width / options.size.coerceAtLeast(1)).toDp() }
    val index = options.indexOf(selected).coerceAtLeast(0)
    val offset by animateDpAsState(
        targetValue = segment * index,
        animationSpec = if (reduced) snap() else ThatsTheWordMotion.spatial(),
        label = "segment",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(CircleShape)
            .background(colors.surfaceContainerHighest)
            .onSizeChanged { width = it.width },
    ) {
        if (width > 0) {
            Box(
                Modifier
                    .offset { IntOffset(offset.roundToPx(), 0) }
                    .width(segment)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(colors.inverseSurface),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            options.forEach { option ->
                val chosen = option == selected
                val ink by animateColorAsState(
                    if (chosen) colors.inverseOnSurface else colors.onSurface,
                    ThatsTheWordMotion.effects(reduced),
                    label = "ink",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .selectable(selected = chosen, onClick = { onSelect(option) }, role = Role.Tab)
                        .testTag(tag(option))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.titleSmall.balanced(),
                        color = ink,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
