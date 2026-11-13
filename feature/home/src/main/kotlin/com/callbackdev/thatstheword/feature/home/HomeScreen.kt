package com.callbackdev.thatstheword.feature.home

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.Eyebrow
import com.callbackdev.thatstheword.core.designsystem.components.GameTile
import com.callbackdev.thatstheword.core.designsystem.components.PageGroup
import com.callbackdev.thatstheword.core.designsystem.components.arrive
import com.callbackdev.thatstheword.core.designsystem.components.inkButtonColors
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.GroupShape
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Mark
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * The start page (VISION §7, PLANNING.md §11 Phase 4), drawn from its state alone: the name; the
 * three game languages, each with how its word of today stands, one touch to play in another; the
 * game language's word of today, to play, take up or see; the streak; the calendar of the dailies,
 * where a past day is played from the archive; free play at a level; the notebook, once it holds a
 * word; and Hard, for the next game.
 *
 * Its sections arrive one after another the first time the page is drawn, on the family's spring;
 * a fade under reduced motion; nothing waits for them.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onLanguage: (Language) -> Unit,
    onPlayToday: () -> Unit,
    onDay: (LocalDate) -> Unit,
    onMonth: (YearMonth) -> Unit,
    onLevel: (Level) -> Unit,
    onPlayFree: () -> Unit,
    onHard: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onNewFree: () -> Unit = {},
    onNotebook: () -> Unit = {},
    onProgress: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    // Saveable, so the sections arrive once in the page's life, not at every return from a game.
    val arrived = rememberSaveable { mutableStateOf(false) }
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .testTag(HomeTags.PAGE)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                .padding(pageGutter())
                .padding(horizontal = ScreenMargin)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            Header(onSettings, Modifier.arrive(0, arrived))
            LanguageSwitch(state, onLanguage, Modifier.arrive(1, arrived))
            TodayCard(state, onPlayToday, Modifier.arrive(2, arrived))
            StreakCard(state, onProgress, Modifier.arrive(3, arrived))
            PageGroup(Modifier.arrive(4, arrived)) {
                Eyebrow(stringResource(R.string.home_calendar_eyebrow))
                Spacer(Modifier.height(4.dp))
                Calendar(state.month, onMonth, onDay)
            }
            FreePlayCard(state, onLevel, onPlayFree, onNewFree, Modifier.arrive(5, arrived))
            if (state.notebookSize > 0) NotebookCard(state.notebookSize, onNotebook, Modifier.arrive(6, arrived))
            HardRow(state.hard, state.hardStanding, onHard, Modifier.arrive(7, arrived))
        }
    }
}

/**
 * The name, and the line under it: capitals drawn, the words kept for TalkBack. Settings is the
 * quiet button in the page's upper corner, above the name, so the name keeps the page's middle.
 */
@Composable
private fun Header(onSettings: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val name = stringResource(R.string.home_name)
    val tagline = stringResource(R.string.home_tagline)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            IconButton(onClick = onSettings, modifier = Modifier.testTag(HomeTags.SETTINGS)) {
                Icon(
                    GameIcons.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = name.uppercase(locale),
            style = ThatsTheWordTheme.type.wordmark,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = name
                heading()
            },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = tagline.uppercase(locale),
            style = ThatsTheWordTheme.type.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { contentDescription = tagline },
        )
    }
}

/**
 * The quick switch of the game language (VISION §7): the three languages, each with how its word
 * of today stands, the chosen one marked by its frame. One touch plays in another language.
 */
@Composable
private fun LanguageSwitch(state: HomeUiState, onLanguage: (Language) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            // The three as tall as the tallest, whatever their words.
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.languages.forEach { today ->
            LanguageChoice(
                today = today,
                selected = today.language == state.gameLanguage,
                onClick = { onLanguage(today.language) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun LanguageChoice(today: LanguageToday, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val reduced = reducedMotion()
    val name = stringResource(languageName(today.language))
    val description = stringResource(R.string.home_language_description, name, statusSentence(today))
    val ground by animateColorAsState(
        if (selected) colors.surfaceContainerHighest else colors.surfaceContainerLow,
        ThatsTheWordMotion.effects(reduced),
        label = "ground",
    )
    val frame by animateColorAsState(
        if (selected) colors.onSurface else Color.Transparent,
        ThatsTheWordMotion.effects(reduced),
        label = "frame",
    )
    Column(
        modifier
            .heightIn(min = 48.dp)
            .clip(ChoiceShape)
            .background(ground)
            .border(2.dp, frame, ChoiceShape)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .semantics { contentDescription = description }
            .testTag(HomeTags.language(today.language))
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusGlyph(today.status)
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = statusShort(today),
            style = MaterialTheme.typography.bodySmall.balanced(),
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@StringRes
private fun languageName(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.home_language_it
    Language.ENGLISH -> R.string.home_language_en
    Language.SPANISH -> R.string.home_language_es
}

/** "Found in 4", "Waiting": a language's word of today, in its choice's few words. */
@Composable
private fun statusShort(today: LanguageToday): String = when (today.status) {
    DayStatus.NOT_PLAYED -> stringResource(R.string.home_status_waiting)
    DayStatus.IN_PROGRESS -> stringResource(R.string.home_status_under_way)
    DayStatus.SOLVED -> stringResource(R.string.home_status_found, today.attemptsUsed)
    DayStatus.MISSED -> stringResource(R.string.home_status_missed)
}

/** The same, as TalkBack says it: "found in 4 attempts". */
@Composable
private fun statusSentence(today: LanguageToday): String = when (today.status) {
    DayStatus.SOLVED -> pluralStringResource(
        R.plurals.home_status_found_description,
        today.attemptsUsed,
        today.attemptsUsed,
    )

    else -> statusShort(today).lowercase(LocalConfiguration.current.locales[0])
}

/**
 * The game language's word of today (VISION §7, "in evidence"): its number and date, a row of
 * tiles that says how it stands without a letter (ink squares for a word found, sunk tiles for one
 * missed, empty ones for a word to find), a sentence, and the button that plays it.
 */
@Composable
private fun TodayCard(state: HomeUiState, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    PageGroup(modifier.testTag(HomeTags.TODAY)) {
        AnimatedContent(
            targetState = state.gameLanguage,
            transitionSpec = {
                fadeIn(ThatsTheWordMotion.effects(reduced)) togetherWith
                    fadeOut(ThatsTheWordMotion.effects(reduced))
            },
            label = "today",
        ) { language ->
            val today = state.languages.first { it.language == language }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Eyebrow(
                    stringResource(
                        if (state.hardStanding.today) R.string.home_today_eyebrow_hard else R.string.home_today_eyebrow,
                        state.number,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = longDate(state.today),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(20.dp))
                TodayTiles(today.status)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = todaySentence(today),
                    style = MaterialTheme.typography.bodyLarge.balanced(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onPlay,
                    colors = inkButtonColors(),
                    modifier = Modifier
                        .heightIn(min = 56.dp)
                        .testTag(HomeTags.PLAY_TODAY),
                ) {
                    Text(
                        text = stringResource(
                            when (today.status) {
                                DayStatus.NOT_PLAYED -> R.string.home_play
                                DayStatus.IN_PROGRESS -> R.string.home_take_up
                                DayStatus.SOLVED, DayStatus.MISSED -> R.string.home_see
                            },
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Five tiles without letters: the word found fills them with the right place's ink, one by one
 * as a row is revealed; a word missed sinks them; a word to find leaves them empty. Drawing only:
 * the sentence under them says it.
 */
@Composable
private fun TodayTiles(status: DayStatus) {
    val mark = when (status) {
        DayStatus.SOLVED -> Mark.RIGHT_PLACE
        DayStatus.MISSED -> Mark.ABSENT
        DayStatus.NOT_PLAYED, DayStatus.IN_PROGRESS -> null
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics {},
    ) {
        repeat(GameSetup.LENGTH) { i ->
            GameTile(
                letter = null,
                mark = mark,
                size = TODAY_TILE,
                revealDelayMillis = TILES_DELAY_MILLIS + i * ThatsTheWordMotion.TILE_STAGGER_MILLIS,
            )
        }
    }
}

@Composable
private fun todaySentence(today: LanguageToday): String = when (today.status) {
    DayStatus.NOT_PLAYED -> stringResource(R.string.home_today_waiting)

    DayStatus.IN_PROGRESS -> if (today.attemptsUsed == 0) {
        stringResource(R.string.home_today_opened)
    } else {
        pluralStringResource(R.plurals.home_today_under_way, today.attemptsUsed, today.attemptsUsed, today.attempts)
    }

    DayStatus.SOLVED -> pluralStringResource(
        R.plurals.home_today_found,
        today.attemptsUsed,
        today.attemptsUsed,
        today.attempts,
    )

    DayStatus.MISSED -> stringResource(R.string.home_today_missed)
}

/** "Friday 20 November", "Venerdì 20 novembre": the locale's own order, its first letter a capital. */
@Composable
private fun longDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        DateTimeFormatter.ofPattern(pattern, locale).format(date).replaceFirstChar { it.titlecase(locale) }
    }
}

/**
 * The streak (PLANNING.md §4.8), the one number of the page, under the line that names it and
 * over the line that says what it counts.
 */
@Composable
private fun StreakCard(state: HomeUiState, onProgress: () -> Unit, modifier: Modifier = Modifier) {
    PageGroup(modifier.testTag(HomeTags.STREAK)) {
        Eyebrow(stringResource(R.string.home_streak_eyebrow))
        Spacer(Modifier.height(8.dp))
        StreakLines(state)
        Spacer(Modifier.height(16.dp))
        // Outlined: the page's filled buttons are the games'; this one opens a page to read.
        OutlinedButton(
            onClick = onProgress,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(HomeTags.OPEN_PROGRESS),
        ) {
            Text(stringResource(R.string.home_progress_open), textAlign = TextAlign.Center)
        }
    }
}

/** The streak's number with its lines, or the line that says none is running. */
@Composable
private fun StreakLines(state: HomeUiState) {
    val streak = state.streak
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (streak == 0) {
            Text(
                text = stringResource(R.string.home_streak_none),
                style = MaterialTheme.typography.bodyLarge.balanced(),
                textAlign = TextAlign.Center,
            )
            return@Column
        }
        val description = pluralStringResource(R.plurals.home_streak_description, streak, streak)
        Text(
            text = streak.toString(),
            style = ThatsTheWordTheme.type.heroNumber,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        )
        Text(
            text = pluralStringResource(R.plurals.home_streak_line, streak),
            style = MaterialTheme.typography.bodyLarge.balanced(),
            textAlign = TextAlign.Center,
        )
        val today = state.gameLanguageToday.status
        if (today == DayStatus.NOT_PLAYED || today == DayStatus.IN_PROGRESS) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_streak_keep),
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Free play (VISION §8, §9): the level of the game language, chosen among four, and a word to play.
 * A word under way is taken up; or left for a new one at the level chosen (PLANNING.md §4.7; the
 * owner, §15, 13 Nov 2026): a word tried opens with the question, over its rows, since it then
 * counts as missed.
 */
@Composable
private fun FreePlayCard(
    state: HomeUiState,
    onLevel: (Level) -> Unit,
    onPlay: () -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val underWay = state.freeUnderWay
    PageGroup(modifier.testTag(HomeTags.FREE)) {
        Eyebrow(
            stringResource(
                if (state.hardStanding.free) R.string.home_free_eyebrow_hard else R.string.home_free_eyebrow,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_free_line),
            style = MaterialTheme.typography.bodyLarge.balanced(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            Level.entries.forEach { level ->
                LevelRow(level, selected = level == state.level, onClick = { onLevel(level) })
            }
        }
        Spacer(Modifier.height(12.dp))
        if (underWay != null) {
            val level = stringResource(levelName(underWay.level))
            Text(
                text = if (underWay.attemptsUsed == 0) {
                    stringResource(R.string.home_free_under_way, level)
                } else {
                    pluralStringResource(
                        R.plurals.home_free_under_way_tried,
                        underWay.attemptsUsed,
                        level,
                        underWay.attemptsUsed,
                        underWay.attempts,
                    )
                },
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(HomeTags.FREE_UNDER_WAY),
            )
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = onPlay,
            colors = inkButtonColors(),
            modifier = Modifier
                .heightIn(min = 56.dp)
                .testTag(HomeTags.PLAY_FREE),
        ) {
            Text(
                text = stringResource(if (underWay != null) R.string.home_free_take_up else R.string.home_free_play),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        if (underWay != null) {
            Spacer(Modifier.height(8.dp))
            // Outlined: the word under way is the first offer; leaving it is the second.
            OutlinedButton(
                onClick = onNew,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(HomeTags.NEW_FREE),
            ) {
                Text(
                    text = stringResource(R.string.home_free_new, stringResource(levelName(state.level))),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LevelRow(level: Level, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(levelName(level)), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(levelLine(level)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun levelName(level: Level): Int = when (level) {
    Level.BASE -> R.string.home_level_base
    Level.INTERMEDIATE -> R.string.home_level_intermediate
    Level.ADVANCED -> R.string.home_level_advanced
    Level.COMPLETE -> R.string.home_level_complete
}

@StringRes
private fun levelLine(level: Level): Int = when (level) {
    Level.BASE -> R.string.home_level_base_line
    Level.INTERMEDIATE -> R.string.home_level_intermediate_line
    Level.ADVANCED -> R.string.home_level_advanced_line
    Level.COMPLETE -> R.string.home_level_complete_line
}

/**
 * The notebook (VISION §9): how many words it holds, and the way in. Drawn once it holds one: a
 * notebook with nothing in it has nothing to say yet.
 */
@Composable
private fun NotebookCard(size: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    PageGroup(modifier.testTag(HomeTags.NOTEBOOK)) {
        Eyebrow(stringResource(R.string.home_notebook_eyebrow))
        Spacer(Modifier.height(8.dp))
        Text(
            text = pluralStringResource(R.plurals.home_notebook_line, size, size),
            style = MaterialTheme.typography.bodyLarge.balanced(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOpen,
            colors = inkButtonColors(),
            modifier = Modifier
                .heightIn(min = 56.dp)
                .testTag(HomeTags.OPEN_NOTEBOOK),
        ) {
            Text(
                text = stringResource(R.string.home_notebook_open),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The Hard option (PLANNING.md §4.4): a switch for every word not yet begun; a word begun keeps the
 * rule it began with, and the line under the switch says so when one does. The cards above say
 * which of the page's words are played in Hard.
 */
@Composable
private fun HardRow(hard: Boolean, standing: HardStanding, onHard: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val status = when {
        standing.todayKept && standing.freeKept -> R.string.home_hard_kept_both
        standing.todayKept -> R.string.home_hard_kept_today
        standing.freeKept -> R.string.home_hard_kept_free
        else -> R.string.home_hard_applies
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .toggleable(value = hard, onValueChange = onHard, role = Role.Switch)
            .testTag(HomeTags.HARD)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_hard), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.home_hard_line),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(HomeTags.HARD_STATUS),
            )
        }
        Spacer(Modifier.size(16.dp))
        Switch(checked = hard, onCheckedChange = null)
    }
}

/** What the tests find the page's parts by. */
object HomeTags {
    const val PAGE = "home_page"
    const val SETTINGS = "home_settings"
    const val TODAY = "home_today"
    const val PLAY_TODAY = "home_play_today"
    const val STREAK = "home_streak"
    const val FREE = "home_free"
    const val PLAY_FREE = "home_play_free"
    const val FREE_UNDER_WAY = "home_free_under_way"
    const val NEW_FREE = "home_new_free"
    const val HARD = "home_hard"
    const val HARD_STATUS = "home_hard_status"
    const val MONTH = "home_month"
    const val NOTEBOOK = "home_notebook"
    const val OPEN_NOTEBOOK = "home_open_notebook"
    const val OPEN_PROGRESS = "home_open_progress"

    fun language(language: Language) = "home_language_${language.tag}"

    fun day(date: LocalDate) = "home_day_$date"
}

private val SECTION_GAP = 16.dp
private val ChoiceShape = RoundedCornerShape(20.dp)
private val TODAY_TILE = 40.dp

/** The tiles fill once the card has arrived. */
private const val TILES_DELAY_MILLIS = 350L
