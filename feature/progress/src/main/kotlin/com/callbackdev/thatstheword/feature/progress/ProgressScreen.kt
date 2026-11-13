package com.callbackdev.thatstheword.feature.progress

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.Eyebrow
import com.callbackdev.thatstheword.core.designsystem.components.GameTile
import com.callbackdev.thatstheword.core.designsystem.components.PageGroup
import com.callbackdev.thatstheword.core.designsystem.components.PageTopBar
import com.callbackdev.thatstheword.core.designsystem.components.arrive
import com.callbackdev.thatstheword.core.designsystem.components.inkButtonColors
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.designsystem.theme.tabular
import com.callbackdev.thatstheword.core.domain.progress.ProgressMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.Language
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The progress page (VISION §11, PLANNING.md §11 Phase 6), drawn from its state alone: the
 * language and the mode looked at, when there is a choice; a sentence, then the share found, the
 * streaks, the distribution of the attempts and the hints and Hard counted apart; the milestones,
 * each reached on its date or with how near it is; and the way to your data.
 *
 * Its sections arrive one after another the first time it is drawn, its bars grow to their length;
 * a fade under reduced motion; nothing waits for them.
 */
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onBack: () -> Unit,
    onLanguage: (Language) -> Unit,
    onMode: (ProgressMode) -> Unit,
    onOpenData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val arrived = rememberSaveable { mutableStateOf(false) }
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
        ) {
            PageTopBar(
                title = stringResource(R.string.progress_title),
                backLabel = stringResource(R.string.progress_back),
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .testTag(ProgressTags.PAGE)
                    .verticalScroll(rememberScrollState())
                    .padding(pageGutter())
                    .padding(horizontal = ScreenMargin)
                    .padding(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
            ) {
                if (state.languages.size > 1) {
                    Segments(
                        options = state.languages,
                        selected = state.language,
                        label = { stringResource(languageName(it)) },
                        onSelect = onLanguage,
                        tag = ProgressTags::language,
                        modifier = Modifier.arrive(0, arrived),
                    )
                }
                StatisticsCard(state, onMode, Modifier.arrive(1, arrived))
                MilestonesCard(state, Modifier.arrive(2, arrived))
                DataCard(onOpenData, Modifier.arrive(3, arrived))
            }
        }
    }
}

/**
 * The language's statistics in a mode (PLANNING.md §4.8): a sentence, the share found as the
 * page's one large number, the streaks for the daily, the distribution, and what is counted apart.
 * A language switched or a mode chosen fades its numbers into the new ones.
 */
@Composable
private fun StatisticsCard(state: ProgressUiState, onMode: (ProgressMode) -> Unit, modifier: Modifier) {
    val reduced = reducedMotion()
    PageGroup(modifier.testTag(ProgressTags.STATISTICS)) {
        if (state.nothingPlayed) {
            // A row of tiles still to fill: the grid before its first word, without a word of its own.
            Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(GameSetup.LENGTH) { GameTile(letter = null, mark = null, size = EMPTY_TILE) }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.progress_nothing),
                style = MaterialTheme.typography.bodyLarge.balanced(),
                textAlign = TextAlign.Center,
            )
            return@PageGroup
        }
        val language = stringResource(languageName(state.language))
        if (state.modes.size > 1) {
            Segments(
                options = state.modes,
                selected = state.mode,
                label = { stringResource(modeName(it)) },
                onSelect = onMode,
                tag = ProgressTags::mode,
            )
            Spacer(Modifier.height(20.dp))
        } else {
            Eyebrow(stringResource(R.string.progress_mode_language, stringResource(modeName(state.mode)), language))
            Spacer(Modifier.height(8.dp))
        }
        // A new language or mode fades in; a game finished meanwhile changes the numbers in place.
        AnimatedContent(
            targetState = state,
            contentKey = { it.language to it.mode },
            transitionSpec = {
                fadeIn(ThatsTheWordMotion.effects(reduced)) togetherWith fadeOut(ThatsTheWordMotion.effects(reduced))
            },
            label = "statistics",
        ) { shown ->
            ModeStatistics(shown)
        }
    }
}

@Composable
private fun ModeStatistics(state: ProgressUiState) {
    val locale = LocalConfiguration.current.locales[0]
    val statistics = state.shown.statistics
    val daily = state.mode == ProgressMode.DAILY
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = pluralStringResource(
                if (daily) R.plurals.progress_daily_sentence else R.plurals.progress_free_sentence,
                statistics.won,
                statistics.won,
                statistics.played,
            ),
            style = MaterialTheme.typography.bodyLarge.balanced(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        val share =
            remember(statistics.winShare, locale) {
                NumberFormat.getPercentInstance(locale).format(statistics.winShare)
            }
        val shareLine = stringResource(if (daily) R.string.progress_share_daily else R.string.progress_share_free)
        Column(
            Modifier
                .clearAndSetSemantics { contentDescription = "$share, $shareLine" }
                .testTag(ProgressTags.SHARE),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = share, style = ThatsTheWordTheme.type.heroNumber)
            Text(
                text = shareLine,
                style = MaterialTheme.typography.bodyLarge.balanced(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (daily) {
            Spacer(Modifier.height(20.dp))
            FigureRow(
                Figure(state.progress.streak, stringResource(R.string.progress_streak_now)),
                Figure(state.progress.longestStreak, stringResource(R.string.progress_streak_longest)),
            )
        }
        Spacer(Modifier.height(24.dp))
        Eyebrow(stringResource(R.string.progress_found_in))
        Spacer(Modifier.height(12.dp))
        Distribution(
            statistics = statistics,
            attempts = state.attempts,
            latestWon = state.shown.latestWon,
            latestAttempts = state.shown.latestAttempts,
            modifier = Modifier.testTag(ProgressTags.DISTRIBUTION),
        )
        if (statistics.withHints > 0 || statistics.withHard > 0) {
            Spacer(Modifier.height(24.dp))
            Eyebrow(stringResource(R.string.progress_apart))
            Spacer(Modifier.height(12.dp))
            FigureRow(
                Figure(statistics.withHints, stringResource(R.string.progress_with_hints)),
                Figure(statistics.withHard, stringResource(R.string.progress_in_hard)),
            )
        }
    }
}

/** A number with the line that says what it counts. */
private data class Figure(val value: Int, val label: String)

/** Two numbers side by side, each with its line, each read as one phrase: "4, current streak". */
@Composable
private fun FigureRow(vararg figures: Figure) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        figures.forEach { figure ->
            val text = figure.value.toString()
            Column(
                Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = "$text, ${figure.label}" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = text, style = MaterialTheme.typography.headlineMedium.tabular())
                Text(
                    text = figure.label,
                    style = MaterialTheme.typography.bodyMedium.balanced(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The milestones (VISION §11): how many are reached, then each with its badge, its name, what it
 * asks, and the date it was reached or how near it is. Recognition, never a key.
 */
@Composable
private fun MilestonesCard(state: ProgressUiState, modifier: Modifier) {
    PageGroup(modifier.testTag(ProgressTags.MILESTONES)) {
        Eyebrow(stringResource(R.string.progress_milestones))
        Spacer(Modifier.height(8.dp))
        val reached = state.milestonesReached
        Text(
            text = if (reached == 0) {
                stringResource(R.string.progress_milestones_none)
            } else {
                pluralStringResource(R.plurals.progress_milestones_sentence, reached, reached, state.milestones.size)
            },
            style = MaterialTheme.typography.bodyLarge.balanced(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.milestones.forEach { MilestoneRow(it, state.today) }
        }
    }
}

/** The way to your data (PLANNING.md §5): a copy to keep, or one to bring back. */
@Composable
private fun DataCard(onOpen: () -> Unit, modifier: Modifier) {
    PageGroup(modifier.testTag(ProgressTags.DATA)) {
        Eyebrow(stringResource(R.string.progress_data))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.progress_data_line),
            style = MaterialTheme.typography.bodyLarge.balanced(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onOpen,
            colors = inkButtonColors(),
            modifier = Modifier
                .heightIn(min = 56.dp)
                .testTag(ProgressTags.OPEN_DATA),
        ) {
            Text(
                text = stringResource(R.string.progress_data_open),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@StringRes
internal fun languageName(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.progress_language_it
    Language.ENGLISH -> R.string.progress_language_en
    Language.SPANISH -> R.string.progress_language_es
}

@StringRes
private fun modeName(mode: ProgressMode): Int = when (mode) {
    ProgressMode.DAILY -> R.string.progress_mode_daily
    ProgressMode.FREE -> R.string.progress_mode_free
}

/** "13 November", "13 novembre", "13 de noviembre", with the year when it is not this one's. */
@Composable
internal fun reachedDate(date: LocalDate, today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, today, locale) {
        val skeleton = if (date.year == today.year) "dMMMM" else "dMMMMy"
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)
    }
}

/** What the tests find the page's parts by. */
object ProgressTags {
    const val PAGE = "progress_page"
    const val STATISTICS = "progress_statistics"
    const val SHARE = "progress_share"
    const val DISTRIBUTION = "progress_distribution"
    const val MILESTONES = "progress_milestones"
    const val DATA = "progress_data"
    const val OPEN_DATA = "progress_open_data"

    fun language(language: Language) = "progress_language_${language.tag}"

    fun mode(mode: ProgressMode) = "progress_mode_${mode.name.lowercase()}"

    fun milestone(name: String) = "progress_milestone_$name"

    fun row(label: String) = "progress_row_$label"
}

private val SECTION_GAP = 16.dp
private val EMPTY_TILE = 36.dp
