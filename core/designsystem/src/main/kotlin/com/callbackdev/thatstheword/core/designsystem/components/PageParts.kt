package com.callbackdev.thatstheword.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.GroupShape
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import kotlinx.coroutines.delay

/** A section of a page on the family's group ground, its content centred. */
@Composable
fun PageGroup(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, GroupShape)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** A short line in capitals above a section: drawn so, read as words. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        text = text.uppercase(locale),
        style = ThatsTheWordTheme.type.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
    )
}

/**
 * A page's buttons in the dress's ink rather than its primary: the Vivid dress's azure would stand
 * beside the marks' ink blue and amber, and a neutral ink reads as the one thing to do without
 * taking a colour of the game's.
 */
@Composable
fun inkButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.inverseSurface,
    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
)

/**
 * A page's top: Back, and the page's name, with a short line in capitals above it when there is one
 * (how many words, which language). The name is the page's heading for TalkBack.
 */
@Composable
fun PageTopBar(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(pageGutter())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(GameIcons.Back, contentDescription = backLabel)
        }
        Column(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            eyebrow?.let { Eyebrow(it) }
            Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        // The width of Back, so the name stands in the page's middle.
        Spacer(Modifier.width(48.dp))
    }
}

/**
 * A section arriving: it rises a little and appears, [index] steps after the first, the first time
 * the page is drawn; a fade under reduced motion; at rest when it already arrived once. [arrived]
 * is the page's, saved, so the sections arrive once in its life, not at every return to it.
 */
@Composable
fun Modifier.arrive(index: Int, arrived: MutableState<Boolean>): Modifier {
    val reduced = reducedMotion()
    val progress = remember { Animatable(if (arrived.value) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(index * SECTION_STAGGER_MILLIS)
            progress.animateTo(1f, ThatsTheWordMotion.spatial(reduced))
            arrived.value = true
        }
    }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        if (!reduced) translationY = (1f - progress.value) * ARRIVAL_RISE.toPx()
    }
}

private val ARRIVAL_RISE = 16.dp
private const val SECTION_STAGGER_MILLIS = 60L
