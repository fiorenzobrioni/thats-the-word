package com.callbackdev.thatstheword.feature.settings

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.ChoiceSegments
import com.callbackdev.thatstheword.core.designsystem.components.GameTile
import com.callbackdev.thatstheword.core.designsystem.components.PageTopBar
import com.callbackdev.thatstheword.core.designsystem.components.arrive
import com.callbackdev.thatstheword.core.designsystem.components.inkButtonColors
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.GroupShape
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.dressScheme
import com.callbackdev.thatstheword.core.designsystem.theme.fontFamilyOf
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.systemReducedMotion
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.callbackdev.thatstheword.core.reminder.ReminderBlock
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the page's rows ask for: the screen is a plain composable a test draws from a state. */
@Immutable
class SettingsActions(
    val gameLanguage: (Language) -> Unit = {},
    val level: (Level) -> Unit = {},
    val hard: (Boolean) -> Unit = {},
    val appLanguage: () -> Unit = {},
    val reminder: (Boolean) -> Unit = {},
    val reminderTime: (LocalTime) -> Unit = {},
    val fixReminder: () -> Unit = {},
    val sound: (Boolean) -> Unit = {},
    val haptics: (Boolean) -> Unit = {},
    val motion: (Boolean?) -> Unit = {},
    val theme: (ThemeMode) -> Unit = {},
    val palette: (AppPalette) -> Unit = {},
    val font: (AppFont) -> Unit = {},
    val wallpaperColors: (Boolean) -> Unit = {},
    val playGuided: () -> Unit = {},
    val data: () -> Unit = {},
    val licences: () -> Unit = {},
)

/**
 * Settings (VISION §7, PLANNING.md §11 Phase 7), drawn from its state alone. First, how to play
 * (the owner, PLANNING §15, Open 17): three lines and a sample row, and the guided game again. Then
 * the game (its language, free play's level, Hard), the app's language (the system's per-app
 * picker, opened, never copied: Open 14), the daily reminder (Open 15), sound, touch and motion,
 * the appearance (the family's: theme, dress, typeface, the wallpaper's colours), your data, and
 * about: what the game sends (nothing), the licences and the sources.
 *
 * [reminderBlock] is what stands between the reminder and the player, read by the route from the
 * system; a switch that is on while Android drops what it sends says so, and opens the page that
 * fixes it.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    reminderBlock: ReminderBlock = ReminderBlock.NONE,
    version: String = "",
) {
    val arrived = rememberSaveable { mutableStateOf(false) }
    val settings = state.settings
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
        ) {
            PageTopBar(
                title = stringResource(R.string.settings_title),
                backLabel = stringResource(R.string.settings_back),
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .testTag(SettingsTags.PAGE)
                    .verticalScroll(rememberScrollState())
                    .padding(pageGutter())
                    .padding(horizontal = ScreenMargin)
                    .padding(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                HowToPlay(state.interfaceLanguage, actions.playGuided, Modifier.arrive(0, arrived))
                GameSection(state, actions, Modifier.arrive(1, arrived))
                SettingsSection(stringResource(R.string.settings_app_language_section), Modifier.arrive(2, arrived)) {
                    LinkRow(
                        title = stringResource(languageName(state.interfaceLanguage)),
                        line = stringResource(R.string.settings_app_language_line),
                        onClick = actions.appLanguage,
                        tag = SettingsTags.APP_LANGUAGE,
                        external = true,
                    )
                }
                ReminderSection(state, reminderBlock, actions, Modifier.arrive(3, arrived))
                FeelSection(state, actions, Modifier.arrive(4, arrived))
                AppearanceSection(state, actions, Modifier.arrive(5, arrived))
                SettingsSection(stringResource(R.string.settings_data_section), Modifier.arrive(6, arrived)) {
                    LinkRow(
                        title = stringResource(R.string.settings_data),
                        line = stringResource(R.string.settings_data_line),
                        onClick = actions.data,
                        tag = SettingsTags.DATA,
                    )
                }
                AboutSection(version, actions, Modifier.arrive(7, arrived))
            }
        }
    }
}

/**
 * How to play (Open 17): for whoever comes back after a while, three lines and a row that shows
 * the marks as the game draws them, and the guided first game to play again. No page of rules.
 */
@Composable
private fun HowToPlay(speaks: Language, onPlayGuided: () -> Unit, modifier: Modifier = Modifier) {
    val locale = Locale.forLanguageTag(speaks.tag)
    val word = stringResource(R.string.how_sample_word).uppercase(locale)
    val marks = SAMPLE_MARKS
    val sample = stringResource(
        R.string.how_sample_description,
        word.mapIndexed { i, c ->
            stringResource(R.string.letter_mark, c.toString(), stringResource(markName(marks[i])))
        }
            .joinToString("; "),
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag(SettingsTags.HOW_TO_PLAY)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val title = stringResource(R.string.how_title)
        Text(
            text = title.uppercase(LocalConfiguration.current.locales[0]),
            style = ThatsTheWordTheme.type.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics { contentDescription = title },
        )
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clearAndSetSemantics { contentDescription = sample },
        ) {
            word.take(SAMPLE_MARKS.size).forEachIndexed { i, c -> GameTile(letter = c, mark = marks[i], size = 44.dp) }
        }
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(R.string.how_line_1, R.string.how_line_2, R.string.how_line_3).forEach { line ->
                Text(
                    text = stringResource(line),
                    style = MaterialTheme.typography.bodyLarge.balanced(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onPlayGuided,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(SettingsTags.PLAY_GUIDED),
        ) {
            Text(stringResource(R.string.how_again), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GameSection(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    val settings = state.settings
    SettingsSection(stringResource(R.string.settings_game), modifier) {
        ChoiceBlock(
            title = stringResource(R.string.settings_game_language),
            line = stringResource(R.string.settings_game_language_line),
        ) {
            ChoiceSegments(
                options = Language.entries,
                selected = settings.gameLanguage,
                label = { stringResource(languageName(it)) },
                onSelect = actions.gameLanguage,
                tag = { SettingsTags.gameLanguage(it) },
            )
        }
        RowDivider()
        Column(Modifier.selectableGroup().padding(vertical = 8.dp)) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.settings_level), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.settings_level_line,
                        stringResource(languageInSentence(settings.gameLanguage)),
                    ),
                    style = MaterialTheme.typography.bodyMedium.balanced(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Level.entries.forEach { level ->
                RadioRow(
                    title = stringResource(levelName(level)),
                    line = stringResource(levelLine(level)),
                    selected = state.level == level,
                    onClick = { actions.level(level) },
                    tag = SettingsTags.level(level),
                )
            }
        }
        RowDivider()
        SwitchRow(
            title = stringResource(R.string.settings_hard),
            line = stringResource(R.string.settings_hard_line),
            checked = settings.hard,
            onChange = actions.hard,
            tag = SettingsTags.HARD,
        )
    }
}

@Composable
private fun ReminderSection(
    state: SettingsUiState,
    block: ReminderBlock,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    var choosing by rememberSaveable { mutableStateOf(false) }
    SettingsSection(stringResource(R.string.settings_reminder_section), modifier) {
        SwitchRow(
            title = stringResource(R.string.settings_reminder),
            line = stringResource(R.string.settings_reminder_line),
            checked = settings.reminder,
            onChange = actions.reminder,
            tag = SettingsTags.REMINDER,
        )
        if (settings.reminder) {
            RowDivider()
            LinkRow(
                title = stringResource(R.string.settings_reminder_time),
                line = stringResource(R.string.settings_reminder_time_line, timeText(settings.reminderTime)),
                onClick = { choosing = true },
                tag = SettingsTags.REMINDER_TIME,
            )
            if (block != ReminderBlock.NONE) {
                NoteCard(
                    text = stringResource(
                        if (block == ReminderBlock.CHANNEL) {
                            R.string.settings_reminder_blocked_channel
                        } else {
                            R.string.settings_reminder_blocked_app
                        },
                    ),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .testTag(SettingsTags.REMINDER_BLOCKED),
                ) {
                    Button(
                        onClick = actions.fixReminder,
                        colors = inkButtonColors(),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(GameIcons.External, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_reminder_fix))
                    }
                }
            }
        }
    }
    if (choosing) {
        TimeDialog(
            initial = settings.reminderTime,
            onPick = {
                choosing = false
                actions.reminderTime(it)
            },
            onDismiss = { choosing = false },
        )
    }
}

/** The reminder's hour, on the phone's own clock dial, in its 12- or 24-hour form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val picker = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_time_title)) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = picker) }
        },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(picker.hour, picker.minute)) }) {
                Text(stringResource(R.string.settings_time_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_time_cancel)) }
        },
    )
}

/** A time as the phone writes it: its own pattern, 12 or 24 hours (never a hard-coded order). */
@Composable
private fun timeText(time: LocalTime): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(time)
}

@Composable
private fun FeelSection(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    val settings = state.settings
    SettingsSection(stringResource(R.string.settings_feel), modifier) {
        SwitchRow(
            title = stringResource(R.string.settings_sound),
            line = stringResource(R.string.settings_sound_line),
            checked = settings.sound,
            onChange = actions.sound,
            tag = SettingsTags.SOUND,
        )
        RowDivider()
        SwitchRow(
            title = stringResource(R.string.settings_haptics),
            line = stringResource(R.string.settings_haptics_line),
            checked = settings.haptics,
            onChange = actions.haptics,
            tag = SettingsTags.HAPTICS,
        )
        RowDivider()
        // What "as the phone" means on this phone now, so the choice is not a guess.
        val phone = stringResource(
            if (systemReducedMotion()) R.string.settings_motion_phone_less else R.string.settings_motion_phone_full,
        )
        ChoiceBlock(
            title = stringResource(R.string.settings_motion),
            line = stringResource(R.string.settings_motion_line, phone),
        ) {
            ChoiceSegments(
                options = MOTIONS,
                selected = settings.reducedMotion,
                label = { stringResource(motionName(it)) },
                onSelect = actions.motion,
                tag = { SettingsTags.motion(it) },
            )
        }
    }
}

@Composable
private fun AppearanceSection(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    val settings = state.settings
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    SettingsSection(stringResource(R.string.settings_appearance), modifier) {
        ChoiceBlock(title = stringResource(R.string.settings_theme), line = null) {
            ChoiceSegments(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                label = { stringResource(themeName(it)) },
                onSelect = actions.theme,
                tag = { SettingsTags.theme(it) },
            )
        }
        RowDivider()
        ChoiceBlock(
            title = stringResource(R.string.settings_dress),
            line = stringResource(
                if (settings.wallpaperColors) R.string.settings_dress_line_wallpaper else R.string.settings_dress_line,
            ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppPalette.entries.forEach { palette ->
                    DressSwatch(
                        palette = palette,
                        dark = dark,
                        selected = settings.palette == palette,
                        quiet = settings.wallpaperColors,
                        onClick = { actions.palette(palette) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
        RowDivider()
        Column(Modifier.selectableGroup().padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.settings_font),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            AppFont.entries.forEach { font ->
                RadioRow(
                    title = stringResource(fontName(font)),
                    line = stringResource(fontLine(font)),
                    selected = settings.font == font,
                    onClick = { actions.font(font) },
                    tag = SettingsTags.font(font),
                    titleFamily = fontFamilyOf(font),
                )
            }
        }
        RowDivider()
        SwitchRow(
            title = stringResource(R.string.settings_wallpaper),
            line = stringResource(R.string.settings_wallpaper_line),
            checked = settings.wallpaperColors,
            onChange = actions.wallpaperColors,
            tag = SettingsTags.WALLPAPER,
        )
    }
}

/**
 * A dress, as it would look: its ground, a title's line in its ink, a tile in its primary and two
 * lines of text, in the light or dark the app wears now. Faded while the wallpaper's colours stand
 * in for the dresses ([quiet]), still a choice for when they are off.
 */
@Composable
private fun DressSwatch(
    palette: AppPalette,
    dark: Boolean,
    selected: Boolean,
    quiet: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = dressScheme(palette, dark)
    val name = stringResource(dressName(palette))
    val line = stringResource(dressLine(palette))
    Column(
        modifier
            .clip(SwatchShape)
            .choiceFrame(selected)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics(mergeDescendants = true) {}
            .testTag(SettingsTags.dress(palette))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surface)
                .padding(12.dp)
                .clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val alpha = if (quiet) 0.45f else 1f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(scheme.primary.copy(alpha = alpha)),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .height(8.dp)
                        .fillMaxWidth(0.7f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(scheme.onSurface.copy(alpha = alpha)),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceContainerHigh.copy(alpha = alpha)),
            )
            Box(
                Modifier
                    .height(6.dp)
                    .fillMaxWidth(0.55f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(scheme.onSurfaceVariant.copy(alpha = alpha * 0.7f)),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(6.dp))
            ChosenDot(selected)
        }
    }
}

@Composable
private fun AboutSection(version: String, actions: SettingsActions, modifier: Modifier = Modifier) {
    SettingsSection(stringResource(R.string.settings_about), modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .testTag(SettingsTags.PRIVACY)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(stringResource(R.string.settings_privacy_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_privacy_line),
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RowDivider()
        LinkRow(
            title = stringResource(R.string.settings_licences),
            line = stringResource(R.string.settings_licences_line),
            onClick = actions.licences,
            tag = SettingsTags.LICENCES,
        )
        if (version.isNotEmpty()) {
            RowDivider()
            Text(
                text = stringResource(R.string.settings_version, version),
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

/** The motion's three answers: as the phone says (null), less, full. */
private val MOTIONS: List<Boolean?> = listOf(null, true, false)

/** The sample row's marks: a letter in its place, one elsewhere, and the rest. */
private val SAMPLE_MARKS = listOf(Mark.RIGHT_PLACE, Mark.ABSENT, Mark.ELSEWHERE, Mark.ABSENT, Mark.RIGHT_PLACE)

@StringRes
internal fun languageName(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.settings_language_it
    Language.ENGLISH -> R.string.settings_language_en
    Language.SPANISH -> R.string.settings_language_es
}

/** A language's name inside a sentence: Italian and Spanish write it in lower case. */
@StringRes
private fun languageInSentence(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.settings_language_it_in_sentence
    Language.ENGLISH -> R.string.settings_language_en_in_sentence
    Language.SPANISH -> R.string.settings_language_es_in_sentence
}

@StringRes
private fun levelName(level: Level): Int = when (level) {
    Level.BASE -> R.string.settings_level_base
    Level.INTERMEDIATE -> R.string.settings_level_intermediate
    Level.ADVANCED -> R.string.settings_level_advanced
    Level.COMPLETE -> R.string.settings_level_complete
}

@StringRes
private fun levelLine(level: Level): Int = when (level) {
    Level.BASE -> R.string.settings_level_base_line
    Level.INTERMEDIATE -> R.string.settings_level_intermediate_line
    Level.ADVANCED -> R.string.settings_level_advanced_line
    Level.COMPLETE -> R.string.settings_level_complete_line
}

@StringRes
private fun motionName(reduced: Boolean?): Int = when (reduced) {
    null -> R.string.settings_motion_as_phone
    true -> R.string.settings_motion_less
    false -> R.string.settings_motion_full
}

@StringRes
private fun themeName(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@StringRes
private fun dressName(palette: AppPalette): Int = when (palette) {
    AppPalette.PAPER -> R.string.settings_dress_paper
    AppPalette.VIVID -> R.string.settings_dress_vivid
}

@StringRes
private fun dressLine(palette: AppPalette): Int = when (palette) {
    AppPalette.PAPER -> R.string.settings_dress_paper_line
    AppPalette.VIVID -> R.string.settings_dress_vivid_line
}

@StringRes
private fun fontName(font: AppFont): Int = when (font) {
    AppFont.GOOGLE_SANS -> R.string.settings_font_google_sans
    AppFont.INTER -> R.string.settings_font_inter
    AppFont.SYSTEM -> R.string.settings_font_system
}

@StringRes
private fun fontLine(font: AppFont): Int = when (font) {
    AppFont.GOOGLE_SANS -> R.string.settings_font_google_sans_line
    AppFont.INTER -> R.string.settings_font_inter_line
    AppFont.SYSTEM -> R.string.settings_font_system_line
}

@StringRes
private fun markName(mark: Mark): Int = when (mark) {
    Mark.RIGHT_PLACE -> R.string.mark_right_place
    Mark.ELSEWHERE -> R.string.mark_elsewhere
    Mark.ABSENT -> R.string.mark_absent
}

/** What the tests find the page's parts by. */
object SettingsTags {
    const val PAGE = "settings_page"
    const val HOW_TO_PLAY = "settings_how_to_play"
    const val PLAY_GUIDED = "settings_play_guided"
    const val HARD = "settings_hard"
    const val APP_LANGUAGE = "settings_app_language"
    const val REMINDER = "settings_reminder"
    const val REMINDER_TIME = "settings_reminder_time"
    const val REMINDER_BLOCKED = "settings_reminder_blocked"
    const val SOUND = "settings_sound"
    const val HAPTICS = "settings_haptics"
    const val WALLPAPER = "settings_wallpaper"
    const val DATA = "settings_data"
    const val PRIVACY = "settings_privacy"
    const val LICENCES = "settings_licences"
    const val LICENCES_PAGE = "settings_licences_page"

    fun gameLanguage(language: Language) = "settings_game_language_${language.tag}"

    fun level(level: Level) = "settings_level_${level.name.lowercase()}"

    fun motion(reduced: Boolean?) = "settings_motion_$reduced"

    fun theme(mode: ThemeMode) = "settings_theme_${mode.name.lowercase()}"

    fun dress(palette: AppPalette) = "settings_dress_${palette.name.lowercase()}"

    fun font(font: AppFont) = "settings_font_${font.name.lowercase()}"
}
