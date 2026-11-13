package com.callbackdev.thatstheword.feature.game

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

/**
 * The question before a free word tried is left for a new one (PLANNING.md §4.7; the owner, §15,
 * 13 Nov 2026): asked over the word itself, its rows in sight, since leaving it counts as missed.
 * Keeping it is staying in the game.
 */
@Composable
internal fun LeaveWordDialog(onLeave: () -> Unit, onKeep: () -> Unit) {
    // In the dress's ink, as the app's buttons: its primary would stand beside the marks' colours.
    val ink = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.leave_title), textAlign = TextAlign.Center) },
        text = { Text(stringResource(R.string.leave_text), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onLeave, colors = ink, modifier = Modifier.testTag(GameTags.LEAVE_YES)) {
                Text(stringResource(R.string.leave_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep, colors = ink, modifier = Modifier.testTag(GameTags.LEAVE_NO)) {
                Text(stringResource(R.string.leave_no))
            }
        },
        modifier = Modifier.testTag(GameTags.LEAVE_DIALOG),
    )
}
