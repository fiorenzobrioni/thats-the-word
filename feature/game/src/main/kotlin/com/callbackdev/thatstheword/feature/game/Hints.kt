package com.callbackdev.thatstheword.feature.game

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.GameTile
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Mark

internal fun hintIcon(kind: HintKind): ImageVector = when (kind) {
    HintKind.TRANSLATION -> GameIcons.Translation
    HintKind.DEFINITION -> GameIcons.Definition
    HintKind.LETTER -> GameIcons.Letter
}

@StringRes
internal fun hintName(kind: HintKind): Int = when (kind) {
    HintKind.TRANSLATION -> R.string.hint_translation
    HintKind.DEFINITION -> R.string.hint_definition
    HintKind.LETTER -> R.string.hint_letter
}

@StringRes
private fun hintLine(kind: HintKind): Int = when (kind) {
    HintKind.TRANSLATION -> R.string.hint_translation_line
    HintKind.DEFINITION -> R.string.hint_definition_line
    HintKind.LETTER -> R.string.hint_letter_line
}

@StringRes
private fun statusName(status: HintStatus): Int = when (status) {
    HintStatus.OFFERED -> R.string.hint_costs
    HintStatus.TAKEN -> R.string.hint_taken
    HintStatus.NOT_FOR_THIS_WORD -> R.string.hint_not_for_this_word
    HintStatus.LAST_ATTEMPT -> R.string.hint_last_attempt
    HintStatus.ALL_FOUND -> R.string.hint_all_found
}

/**
 * The hints (PLANNING.md §4.5), in the keyboard's place while they are open: the three, each with
 * what it gives and where it stands, the cost said before anything is taken. A touch takes one, and
 * the keyboard comes back with the hint drawn in the row it spent: the touch that opened the panel
 * was the first of two, so no hint is taken by a slip.
 */
@Composable
internal fun HintsPanel(
    state: GameUiState,
    onTake: (HintKind) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val title = stringResource(R.string.hints_open)
    Column(
        modifier
            .fillMaxWidth()
            .testTag(GameTags.HINTS_PANEL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title.uppercase(locale),
            style = ThatsTheWordTheme.type.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { contentDescription = title },
        )
        Text(
            text = stringResource(R.string.hints_cost),
            style = MaterialTheme.typography.bodyMedium.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.size(2.dp))
        state.hintChoices.forEach { choice -> HintChoice(choice, onTake) }
        TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.hints_close))
        }
    }
}

@Composable
private fun HintChoice(choice: HintChoiceUi, onTake: (HintKind) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val offered = choice.status == HintStatus.OFFERED
    val name = stringResource(hintName(choice.kind))
    val line = stringResource(hintLine(choice.kind))
    val status = stringResource(statusName(choice.status))
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(ChoiceShape)
            .background(if (offered) colors.surfaceContainerHigh else colors.surfaceContainerLow)
            .clickable(enabled = offered, role = Role.Button) { onTake(choice.kind) }
            .semantics(mergeDescendants = true) {}
            .testTag(GameTags.hint(choice.kind))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            hintIcon(choice.kind),
            contentDescription = null,
            tint = if (offered) colors.onSurface else colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(line, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            color = if (offered) colors.onSurface else colors.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * A row spent on a hint (PLANNING.md §7.3): the cost in sight, and what the hint gave in its
 * place. The letter is drawn as a tile in its right place, the row's other tiles faint; the
 * translation and the definition as a band the grid's width, with the hint's icon. Taken while the
 * page is open, it arrives on the game's spring, as a row is revealed; a fade under reduced
 * motion; at rest when the game was taken up with it.
 */
@Composable
internal fun HintRow(hint: HintUi, number: Int, length: Int, tile: Dp, animate: Boolean, reduced: Boolean) {
    val sentence = hintSentence(number, hint)
    val arrival = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) { arrival.animateTo(1f, ThatsTheWordMotion.playful(reduced)) }
    val modifier = Modifier
        .testTag(GameTags.hintRow(hint.kind))
        .clearAndSetSemantics { contentDescription = sentence }
    if (hint.kind == HintKind.LETTER) {
        Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP), modifier = modifier) {
            repeat(length) { i ->
                if (i == hint.position) {
                    GameTile(
                        letter = hint.letter,
                        mark = Mark.RIGHT_PLACE,
                        size = tile,
                        revealDelayMillis = if (animate) 0L else null,
                    )
                } else {
                    GameTile(
                        letter = null,
                        mark = null,
                        size = tile,
                        modifier = Modifier.graphicsLayer { alpha = SPENT_ALPHA },
                    )
                }
            }
        }
        return
    }
    val width = tile * length + TILE_GAP * (length - 1)
    Row(
        modifier
            .width(width)
            .heightIn(min = tile)
            .graphicsLayer {
                alpha = arrival.value.coerceIn(0f, 1f)
                if (!reduced) {
                    val scale = 0.94f + 0.06f * arrival.value
                    scaleX = scale
                    scaleY = scale
                }
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(tile * BAND_CORNER))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            hintIcon(hint.kind),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            Text(
                text = hint.text.orEmpty(),
                style = if (hint.kind == HintKind.TRANSLATION) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodySmall
                },
            )
        }
    }
}

/** A hint's row as TalkBack reads it: "Row 6, spent on a hint. Translation: cat." */
@Composable
internal fun hintSentence(number: Int, hint: HintUi): String = when (hint.kind) {
    HintKind.LETTER -> {
        val ordinals = stringArrayResource(R.array.ordinals)
        stringResource(
            R.string.row_hint_letter,
            number,
            ordinals.getOrElse(hint.position ?: 0) { "" },
            hint.letter?.toString().orEmpty(),
        )
    }

    else -> stringResource(R.string.row_hint_text, number, stringResource(hintName(hint.kind)), hint.text.orEmpty())
}

private val ChoiceShape = RoundedCornerShape(16.dp)

/** The band's corner, as a share of the tile's side: the right place's own corner (22%). */
private const val BAND_CORNER = 0.22f
