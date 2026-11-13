package com.callbackdev.thatstheword.feature.game

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.sound.GameSound
import com.callbackdev.thatstheword.core.designsystem.theme.GroupShape
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Milestone

/**
 * The end of a game (PLANNING.md §7.4, VISION §9): the line the name stands for («That's the
 * word.») or the line for a word not found, then the card: the word as it is written, with its
 * accents (*città*), how it was found, the words tried, the result to share, and what comes next:
 * the day again as practice, or another free word (PLANNING.md §4.6, §4.7). A replay's result is
 * not shared: it is no day's result.
 *
 * Each word is explained in the interface's language (VISION §9): the solution's translation and
 * meaning under it, each word tried with its own and a mark to keep it in the notebook (the owner,
 * PLANNING §15, Open 8). A milestone the game reached is said here, once, with a light across it.
 */
@Composable
internal fun EndCard(
    state: GameUiState,
    onShare: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleNotebook: (Int) -> Unit = {},
    onSound: (GameSound) -> Unit = {},
) {
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
                } else if (state.conceded) {
                    pluralStringResource(R.plurals.end_left, state.attemptsUsed, state.attemptsUsed, state.attempts)
                } else {
                    pluralStringResource(R.plurals.end_not_found, state.attempts, state.attempts)
                },
                style = MaterialTheme.typography.bodyLarge.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            state.solutionMeaning?.let { SolutionMeaning(it) }
            if (state.solutionKept && !state.practice) {
                KeptLine()
            }
            if (state.practice) {
                Text(
                    text = stringResource(R.string.end_practice),
                    style = MaterialTheme.typography.bodyMedium.balanced(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            state.milestones.forEach { MilestoneNote(it, onSound) }
            if (state.guided) {
                // The guided game's last lesson: what the game is from now on.
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.lesson_done),
                    style = MaterialTheme.typography.titleMedium.balanced(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(GameTags.LESSON_DONE),
                )
            }
            if (state.tried.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                val yours = stringResource(R.string.end_your_words)
                Text(
                    text = yours.uppercase(locale),
                    style = ThatsTheWordTheme.type.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = yours },
                )
                Spacer(Modifier.height(4.dp))
                state.tried.forEach { word -> TriedWord(word, onToggleNotebook) }
            }
            Spacer(Modifier.height(14.dp))
            state.next?.let { next ->
                // In the dress's ink, not its primary: the Vivid dress's azure would sit beside
                // the marks' ink blue and amber.
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(GameTags.NEXT),
                ) {
                    Text(
                        text = stringResource(
                            when (next) {
                                NextGame.PRACTICE -> R.string.end_play_again
                                NextGame.ANOTHER_WORD -> R.string.end_another_word
                                NextGame.START_PLAYING -> R.string.end_start_playing
                            },
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (state.shareable) {
                // Outlined: a filled button would take the dress's colour, and its amber or blue
                // would sit beside the marks' own.
                OutlinedButton(onClick = onShare) {
                    Icon(GameIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.end_share))
                }
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

/**
 * The solution explained under it (VISION §9): its translation in the interface's language, then
 * its meaning; in the game's own language the definition alone. A word the sources say nothing
 * reliable about says nothing here.
 */
@Composable
private fun SolutionMeaning(meaning: MeaningUi) {
    if (!meaning.known) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag(GameTags.MEANING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        meaning.translations?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium.balanced(),
                textAlign = TextAlign.Center,
            )
        }
        meaning.meaning?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The solution written down: one quiet line, the notebook's mark beside it. */
@Composable
private fun KeptLine() {
    Row(
        Modifier
            .padding(top = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            GameIcons.NotebookKept,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.end_kept),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A word tried: as it is written, the lemma it is a form of, its translation and meaning, and the
 * mark that keeps it in the notebook. The mark is a toggle while the word was added by hand, so a
 * slip is undone; a word a game hid stays, and its mark says so.
 */
@Composable
private fun TriedWord(word: TriedWordUi, onToggle: (Int) -> Unit) {
    val meaning = word.meaning
    val label = when {
        !word.inNotebook -> stringResource(R.string.notebook_add, meaning.written)
        word.removable -> stringResource(R.string.notebook_remove, meaning.written)
        else -> stringResource(R.string.notebook_in, meaning.written)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(meaning.written, style = MaterialTheme.typography.titleMedium)
            meaning.lemma?.let {
                Text(
                    text = stringResource(R.string.end_form_of, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            meaning.translations?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            meaning.meaning?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        val enabled = !word.inNotebook || word.removable
        IconToggleButton(
            checked = word.inNotebook,
            onCheckedChange = { onToggle(word.index) },
            enabled = enabled,
            modifier = Modifier
                .testTag(GameTags.notebook(word.index))
                .semantics { contentDescription = label },
        ) {
            Icon(
                if (word.inNotebook) GameIcons.NotebookKept else GameIcons.Notebook,
                contentDescription = null,
                tint = if (word.inNotebook) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/**
 * A milestone reached (PLANNING.md §4.8, §7.4): its sentence arrives with a soft light across it,
 * once, while the page that saw it is open; under reduced motion it only fades in. Recognition,
 * never a key: nothing is unlocked by it.
 */
@Composable
private fun MilestoneNote(milestone: Milestone, onSound: (GameSound) -> Unit) {
    val reduced = reducedMotion()
    val locale = LocalConfiguration.current.locales[0]
    val light = remember { Animatable(0f) }
    val arrival = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        onSound(GameSound.MILESTONE)
        arrival.animateTo(1f, ThatsTheWordMotion.playful(reduced))
        if (!reduced) light.animateTo(1f, tween(LIGHT_MILLIS, easing = FastOutSlowInEasing))
    }
    val glow = MaterialTheme.colorScheme.surfaceBright
    val eyebrow = stringResource(R.string.milestone_eyebrow)
    val sentence = stringResource(milestoneSentence(milestone))
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = arrival.value.coerceIn(0f, 1f)
                if (!reduced) translationY = (1f - arrival.value) * 8.dp.toPx()
            }
            .clip(MilestoneShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .drawWithContent {
                drawContent()
                // The light: a soft band crossing the note once, left to right.
                if (light.value in 0.001f..0.999f) {
                    val band = size.width * 0.45f
                    val x = -band + (size.width + band * 2) * light.value
                    drawRect(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.5f to glow.copy(alpha = 0.55f),
                            1f to Color.Transparent,
                            startX = x - band,
                            endX = x + band,
                        ),
                    )
                }
            }
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag(GameTags.MILESTONE)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = eyebrow.uppercase(locale),
            style = ThatsTheWordTheme.type.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { contentDescription = eyebrow },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = sentence,
            style = MaterialTheme.typography.titleMedium.balanced(),
            textAlign = TextAlign.Center,
        )
    }
}

@StringRes
internal fun milestoneSentence(milestone: Milestone): Int = when (milestone) {
    Milestone.SEVEN_DAILIES_IN_A_ROW -> R.string.milestone_seven_dailies
    Milestone.FIRST_FOREIGN_WIN -> R.string.milestone_first_foreign_win
    Milestone.HUNDRED_WORDS -> R.string.milestone_hundred_words
    Milestone.ONE_DAY_THREE_LANGUAGES -> R.string.milestone_three_languages
}

private val MilestoneShape = RoundedCornerShape(20.dp)
private const val LIGHT_MILLIS = 1_400
