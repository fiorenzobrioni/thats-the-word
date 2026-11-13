package com.callbackdev.thatstheword.feature.settings.data

import android.icu.text.ListFormatter
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.data.DataContents
import com.callbackdev.thatstheword.core.designsystem.components.Eyebrow
import com.callbackdev.thatstheword.core.designsystem.components.PageGroup
import com.callbackdev.thatstheword.core.designsystem.components.PageTopBar
import com.callbackdev.thatstheword.core.designsystem.components.arrive
import com.callbackdev.thatstheword.core.designsystem.components.inkButtonColors
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.RIGHT_PLACE_CORNER_PERCENT
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.designsystem.theme.tabular
import com.callbackdev.thatstheword.feature.settings.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** What the page's buttons ask for: the file pickers are the page's, their answers the view model's. */
@Immutable
class DataActions(
    val save: (android.net.Uri) -> Unit = {},
    val read: (android.net.Uri) -> Unit = {},
    val confirmRestore: () -> Unit = {},
    val cancelRestore: () -> Unit = {},
    val dismissOutcome: () -> Unit = {},
    val fileName: () -> String = { "thatstheword-backup.json" },
)

/**
 * Your data (PLANNING.md §5, §11 Phase 6), drawn from its state alone: why nothing leaves unless
 * the player moves it; what this phone holds; a copy to keep; a copy to bring back. The system's
 * file picker chooses where a copy goes and which is opened, so no storage permission is asked. A
 * copy read and checked is shown before anything is written, and replaces the phone's only on a
 * yes; what a copy came to is said on a card until it is put away.
 */
@Composable
fun DataScreen(state: DataUiState, onBack: () -> Unit, actions: DataActions, modifier: Modifier = Modifier) {
    val arrived = rememberSaveable { mutableStateOf(false) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        if (uri != null) actions.save(uri)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.read(uri)
    }
    val idle = state.task == null
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
        ) {
            PageTopBar(
                title = stringResource(R.string.data_title),
                backLabel = stringResource(R.string.data_back),
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .testTag(DataTags.PAGE)
                    .verticalScroll(rememberScrollState())
                    .padding(pageGutter())
                    .padding(horizontal = ScreenMargin)
                    .padding(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.data_intro),
                    style = MaterialTheme.typography.bodyLarge.balanced(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .arrive(0, arrived),
                )
                PhoneCard(state.contents, Modifier.arrive(1, arrived))
                val reduced = reducedMotion()
                // An outcome opens its room and fades in; put away, it leaves drawn as it was.
                AnimatedContent(
                    targetState = state.outcome,
                    transitionSpec = {
                        ThatsTheWordMotion.enter(reduced) togetherWith ThatsTheWordMotion.exit(reduced) using
                            SizeTransform(clip = false)
                    },
                    label = "outcome",
                ) { outcome ->
                    if (outcome != null) OutcomeCard(outcome, actions.dismissOutcome)
                }
                ActionCard(
                    icon = GameIcons.SaveCopy,
                    title = stringResource(R.string.data_save),
                    line = stringResource(R.string.data_save_line),
                    button = stringResource(R.string.data_save_button),
                    busy = if (state.task == DataTask.SAVING) stringResource(R.string.data_saving) else null,
                    enabled = idle,
                    filled = true,
                    // No file app to take the request is no crash: the button simply does nothing.
                    onClick = { runCatching { create.launch(actions.fileName()) } },
                    tag = DataTags.SAVE,
                    modifier = Modifier.arrive(2, arrived),
                )
                ActionCard(
                    icon = GameIcons.OpenCopy,
                    title = stringResource(R.string.data_restore),
                    line = stringResource(R.string.data_restore_line),
                    button = stringResource(R.string.data_restore_button),
                    busy = when (state.task) {
                        DataTask.READING -> stringResource(R.string.data_reading)
                        DataTask.RESTORING -> stringResource(R.string.data_restoring)
                        else -> null
                    },
                    enabled = idle,
                    filled = false,
                    // Some file apps call a .json file text, or nothing at all: all three are offered.
                    onClick = { runCatching { open.launch(arrayOf(JSON, "text/plain", "application/octet-stream")) } },
                    tag = DataTags.RESTORE,
                    modifier = Modifier.arrive(3, arrived),
                )
            }
        }
    }
    state.preview?.let { preview ->
        RestoreDialog(
            preview,
            here = state.contents,
            onRestore = actions.confirmRestore,
            onDismiss = actions.cancelRestore,
        )
    }
}

/** What this phone holds: three numbers, each with its line; one sentence when it holds nothing. */
@Composable
private fun PhoneCard(contents: DataContents, modifier: Modifier) {
    PageGroup(modifier.testTag(DataTags.PHONE)) {
        Eyebrow(stringResource(R.string.data_phone))
        Spacer(Modifier.height(12.dp))
        if (contents.isEmpty) {
            Text(
                text = stringResource(R.string.data_nothing),
                style = MaterialTheme.typography.bodyLarge.balanced(),
                textAlign = TextAlign.Center,
            )
            return@PageGroup
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Figure(contents.games, pluralStringResource(R.plurals.data_games_line, contents.games), Modifier.weight(1f))
            Figure(contents.words, pluralStringResource(R.plurals.data_words_line, contents.words), Modifier.weight(1f))
            Figure(
                contents.milestones,
                pluralStringResource(R.plurals.data_milestones_line, contents.milestones),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Figure(value: Int, line: String, modifier: Modifier) {
    Column(
        modifier.clearAndSetSemantics { contentDescription = "$value $line" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium.tabular())
        Text(
            text = line,
            style = MaterialTheme.typography.bodySmall.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A thing to do with a copy: its mark on a tile, what it is and what it carries, and its button,
 * which says what it is doing while it does it, and holds until it is done.
 */
@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    line: String,
    button: String,
    busy: String?,
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier,
) {
    PageGroup(modifier.testTag(tag)) {
        Box(
            Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(RIGHT_PLACE_CORNER_PERCENT.toInt()),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        Eyebrow(title)
        Spacer(Modifier.height(8.dp))
        Text(text = line, style = MaterialTheme.typography.bodyLarge.balanced(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        val content: @Composable () -> Unit = {
            if (busy != null) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(busy ?: button, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        val buttonModifier = Modifier
            .heightIn(min = 56.dp)
            .semantics { if (busy != null) liveRegion = LiveRegionMode.Polite }
            .testTag("${tag}_button")
        if (filled) {
            Button(onClick = onClick, enabled = enabled, colors = inkButtonColors(), modifier = buttonModifier) {
                content()
            }
        } else {
            OutlinedButton(onClick = onClick, enabled = enabled, modifier = buttonModifier) { content() }
        }
    }
}

/**
 * What the last copy came to: a copy saved (where, and what it holds), a copy back, or why not.
 * A polite live region, so TalkBack says it once; put away by its cross.
 */
@Composable
private fun OutcomeCard(outcome: DataOutcome, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val failed = outcome is DataOutcome.Failed
    val text = outcomeText(outcome)
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerHigh, RoundedCornerShape(20.dp))
            .testTag(DataTags.OUTCOME)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(RIGHT_PLACE_CORNER_PERCENT.toInt())
        Box(
            Modifier
                .size(28.dp)
                .background(if (failed) colors.surfaceContainerHighest else FeedbackColors.RightPlace, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (failed) {
                Text("!", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            } else {
                Icon(
                    GameIcons.Check,
                    contentDescription = null,
                    tint = FeedbackColors.OnRightPlace,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        IconButton(onClick = onDismiss) {
            Icon(GameIcons.Clear, contentDescription = stringResource(R.string.data_close))
        }
    }
}

@Composable
private fun outcomeText(outcome: DataOutcome): String = when (outcome) {
    is DataOutcome.Saved -> {
        val saved =
            outcome.file?.let { stringResource(R.string.data_saved_as, it) } ?: stringResource(R.string.data_saved)
        val holds = if (outcome.contents.isEmpty) {
            stringResource(R.string.data_holds_choices)
        } else {
            stringResource(R.string.data_holds, contentsWords(outcome.contents))
        }
        "$saved $holds"
    }

    is DataOutcome.Restored -> if (outcome.contents.isEmpty) {
        stringResource(R.string.data_restored_choices)
    } else {
        stringResource(R.string.data_restored, contentsWords(outcome.contents))
    }

    is DataOutcome.Failed -> stringResource(
        when (outcome.reason) {
            DataFailure.WRITE -> R.string.data_failed_write
            DataFailure.READ -> R.string.data_failed_read
            DataFailure.NOT_A_BACKUP -> R.string.data_failed_not_a_backup
            DataFailure.TOO_NEW -> R.string.data_failed_too_new
            DataFailure.DAMAGED -> R.string.data_failed_damaged
            DataFailure.RESTORE -> R.string.data_failed_restore
        },
    )
}

/**
 * The question before a copy takes the phone's place (PLANNING.md §5): when it was saved and what
 * it holds, and what it replaces here. Nothing is written before the yes.
 */
@Composable
private fun RestoreDialog(preview: RestorePreview, here: DataContents, onRestore: () -> Unit, onDismiss: () -> Unit) {
    val saved = longDate(preview.savedOn)
    val holds = if (preview.contents.isEmpty) {
        stringResource(R.string.data_confirm_holds_choices, saved)
    } else {
        stringResource(R.string.data_confirm_holds, saved, contentsWords(preview.contents))
    }
    val replaces = if (here.isEmpty) {
        stringResource(R.string.data_confirm_nothing_lost)
    } else {
        stringResource(R.string.data_confirm_replaces, contentsWords(here))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(GameIcons.OpenCopy, contentDescription = null) },
        // In the dress's ink, as the app's buttons: its primary would stand beside the marks' colours.
        iconContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource(R.string.data_confirm_title), textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(holds, style = MaterialTheme.typography.bodyMedium)
                Text(replaces, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onRestore, colors = inkText(), modifier = Modifier.testTag(DataTags.CONFIRM)) {
                Text(stringResource(R.string.data_confirm_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = inkText(), modifier = Modifier.testTag(DataTags.CANCEL)) {
                Text(stringResource(R.string.data_confirm_no))
            }
        },
        modifier = Modifier.testTag(DataTags.DIALOG),
    )
}

@Composable
private fun inkText() = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)

/** "27 games played, 41 words in the notebook and 2 milestones": the locale's own list, a count left out when it is none. */
@Composable
private fun contentsWords(contents: DataContents): String {
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val parts = buildList {
        if (contents.games > 0) add(resources.getQuantityString(R.plurals.data_games, contents.games, contents.games))
        if (contents.words > 0) add(resources.getQuantityString(R.plurals.data_words, contents.words, contents.words))
        if (contents.milestones > 0) {
            add(resources.getQuantityString(R.plurals.data_milestones, contents.milestones, contents.milestones))
        }
    }
    return remember(parts, locale) { ListFormatter.getInstance(locale).format(parts) }
}

/** "13 November 2026", in the locale's own order. */
@Composable
private fun longDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMMy"), locale).format(date)
    }
}

/** What the tests find the page's parts by. */
object DataTags {
    const val PAGE = "data_page"
    const val PHONE = "data_phone"
    const val SAVE = "data_save"
    const val RESTORE = "data_restore"
    const val OUTCOME = "data_outcome"
    const val DIALOG = "data_dialog"
    const val CONFIRM = "data_confirm"
    const val CANCEL = "data_cancel"
}

private const val JSON = "application/json"
