package com.callbackdev.thatstheword.feature.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.DENSE_TARGETS_TAG
import com.callbackdev.thatstheword.core.designsystem.components.GameActionKey
import com.callbackdev.thatstheword.core.designsystem.components.GameKey
import com.callbackdev.thatstheword.core.designsystem.components.KeyGap
import com.callbackdev.thatstheword.core.designsystem.components.KeyHeight
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.model.Letter

/**
 * The game's keyboard (PLANNING.md §7.3): QWERTY, Spanish's Ñ beside L, Enter and Delete on the
 * bottom row; each key a row's share wide and in its letter's best mark. Its keys are narrower than
 * a finger's 48dp, as a phone keyboard's are: marked dense, each still its own labelled key.
 */
@Composable
internal fun Keyboard(
    state: GameUiState,
    onLetter: (Letter) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val widest = state.keyboard.maxOf { it.size }
    Column(
        modifier
            .fillMaxWidth()
            .height(KEYBOARD_KEYS_HEIGHT)
            .testTag(DENSE_TARGETS_TAG),
        verticalArrangement = Arrangement.spacedBy(KeyGap),
    ) {
        state.keyboard.forEachIndexed { index, row ->
            val last = index == state.keyboard.lastIndex
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                // A shorter row is centred, as on a phone's keyboard; the last has Enter and Delete
                // at its ends, each a key and a half wide.
                val keysAndActions = row.size + if (last) 3 else 0
                val spare = (widest - keysAndActions).coerceAtLeast(0) / 2f
                if (spare > 0f) Spacer(Modifier.weight(spare))
                if (last) {
                    GameActionKey(GameIcons.Enter, stringResource(R.string.key_enter), onEnter, Modifier.weight(1.5f))
                }
                row.forEach { key ->
                    val mark = state.keyMarks[key.char]
                    val label =
                        mark?.let {
                            stringResource(R.string.letter_mark, key.char.toString(), stringResource(markName(it)))
                        }
                            ?: key.char.toString()
                    GameKey(key.char, mark, label, { onLetter(key.letter) }, Modifier.weight(1f))
                }
                if (last) {
                    GameActionKey(
                        GameIcons.Delete,
                        stringResource(R.string.key_delete),
                        onDelete,
                        Modifier.weight(1.5f),
                    )
                }
                if (spare > 0f) Spacer(Modifier.weight(spare))
            }
        }
    }
}

/** Three rows of keys and the gaps between them. */
private val KEYBOARD_KEYS_HEIGHT = KeyHeight * 3 + KeyGap * 2

/** The keyboard with its padding, as the page lays it out: what the grid gives back when it leaves. */
internal val KEYBOARD_HEIGHT = KEYBOARD_KEYS_HEIGHT + 16.dp

/** The keyboard reaches nearer the edges than the page's text, as a phone keyboard does. */
internal val KEYBOARD_MARGIN = 6.dp
