package com.callbackdev.thatstheword.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.Eyebrow
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.GroupShape
import com.callbackdev.thatstheword.core.designsystem.theme.balanced

/*
 * The settings' rows, in the family's shape: a section's short line in capitals above its group,
 * the group on the family's ground, each row a title and the line that says what it does, the
 * control at the row's end. A whole row is the touch target, so a finger never has to find the
 * switch itself, and TalkBack reads it as one control with its state.
 */

/** A section: its name in capitals above its group of rows. */
@Composable
internal fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(
            title,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(start = 20.dp, bottom = 8.dp)
                .semantics { heading() },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GroupShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            content = content,
        )
    }
}

/** The hairline between two rows of a group, inset to the rows' words. */
@Composable
internal fun RowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/** A row's words: its title, and the line under it when there is one. */
@Composable
private fun RowWords(title: String, line: String?, modifier: Modifier = Modifier, titleFamily: FontFamily? = null) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.let {
                if (titleFamily !=
                    null
                ) {
                    it.copy(fontFamily = titleFamily)
                } else {
                    it
                }
            },
        )
        if (line != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A choice that is on or off: the whole row toggles it. */
@Composable
internal fun SwitchRow(
    title: String,
    line: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    tag: String,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, enabled = enabled, onValueChange = onChange, role = Role.Switch)
            .testTag(tag)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowWords(title, line, Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/**
 * A row that opens something: a page of the app (a chevron) or one of the system's (an arrow
 * leaving its box, [external]). Its [line] says what is there, or what is chosen there now.
 */
@Composable
internal fun LinkRow(title: String, line: String?, onClick: () -> Unit, tag: String, external: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowWords(title, line, Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        Icon(
            imageVector = if (external) GameIcons.External else GameIcons.Next,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A row that only says something: its words, no control. */
@Composable
internal fun TextRow(title: String, line: String?, tag: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag(tag)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowWords(title, line, Modifier.weight(1f))
    }
}

/** One of a few choices, each with its line: a radio row, the whole row its target. */
@Composable
internal fun RadioRow(
    title: String,
    line: String?,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    titleFamily: FontFamily? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(tag)
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
        RowWords(title, line, Modifier.weight(1f), titleFamily)
    }
}

/** A choice's name above its segments, inside a group. */
@Composable
internal fun ChoiceBlock(title: String, line: String?, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowWords(title, line)
        content()
    }
}

/** A short note inside a group: something the player should know before choosing. */
@Composable
internal fun NoteCard(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(NoteShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.balanced(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        action?.invoke()
    }
}

/** A selected swatch's frame: the dress's ink, as the start page frames the chosen language. */
@Composable
internal fun Modifier.choiceFrame(selected: Boolean): Modifier = border(
    width = 2.dp,
    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
    shape = SwatchShape,
)

/** A small round mark of a chosen swatch. */
@Composable
internal fun ChosenDot(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
                if (selected) colors.inverseSurface else colors.surfaceContainerHighest,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                GameIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

internal val NoteShape = RoundedCornerShape(16.dp)
internal val SwatchShape = RoundedCornerShape(20.dp)
