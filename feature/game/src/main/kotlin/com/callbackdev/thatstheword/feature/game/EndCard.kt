package com.callbackdev.thatstheword.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.model.GameState

/**
 * The end of a game (PLANNING.md §7.4, VISION §9): the line the name stands for («That's the
 * word.») or the line for a word not found, then the card: the word as it is written, with its
 * accents (*città*), how it was found, the words tried, and the result to share. The meanings and
 * the notebook join it in Phase 5.
 */
@Composable
internal fun EndCard(state: GameUiState, onShare: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val won = state.state == GameState.WON
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Headline(stringResource(if (won) R.string.game_found else R.string.game_not_found))
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(GameTags.END_CARD)
                .background(MaterialTheme.colorScheme.surfaceContainer, GroupShape)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val theWord = stringResource(R.string.end_the_word)
            Text(
                text = theWord.uppercase(locale),
                style = ThatsTheWordTheme.type.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics { contentDescription = theWord },
            )
            // As it is written, with its accents: the key form is only how it is typed.
            Text(
                text = state.solutionWritten,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = if (won) {
                    pluralStringResource(R.plurals.end_found_in, state.attemptsUsed, state.attemptsUsed, state.attempts)
                } else {
                    pluralStringResource(R.plurals.end_not_found, state.attempts, state.attempts)
                },
                style = MaterialTheme.typography.bodyLarge.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (state.guessesWritten.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                val yours = stringResource(R.string.end_your_words)
                Text(
                    text = yours.uppercase(locale),
                    style = ThatsTheWordTheme.type.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = yours },
                )
                state.guessesWritten.forEach { written ->
                    Text(written, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(14.dp))
            // Outlined: a filled button would take the dress's colour, and its amber or blue would
            // sit beside the marks' own.
            OutlinedButton(onClick = onShare) {
                Icon(GameIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.end_share))
            }
        }
    }
}

/** The end's line: it rises a little and settles, as on the start page; a fade under reduced motion. */
@Composable
private fun Headline(text: String) {
    val reduced = reducedMotion()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, ThatsTheWordMotion.playful(reduced)) }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall.balanced(),
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            if (!reduced) translationY = (1f - progress.value) * 12.dp.toPx()
        },
    )
}
