package com.callbackdev.thatstheword.feature.home

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.DENSE_TARGETS_TAG
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.ABSENT_SCALE
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.TILE_CORNER_PERCENT
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.designsystem.theme.tabular
import com.callbackdev.thatstheword.core.domain.daily.ArchiveDay
import com.callbackdev.thatstheword.core.domain.daily.ArchiveMonth
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The calendar of the dailies (VISION §8, PLANNING.md §4.6): a month of the game language, each
 * day drawn as the game draws a letter, by colour and shape together (VISION §6): found, the ink
 * square of a letter in its right place; missed, the sunk quiet tile of an absent one; under way,
 * the amber round of elsewhere; not played, the empty tile; a day caught up later from the
 * archive, a dot beneath its number. A day from the epoch to today is touched to play it, take it
 * up or see it; the others are drawn faint and say why.
 *
 * Its weeks start on the locale's first day. Its cells are a seventh of the column, under a
 * finger's 48dp on a narrow phone, as every calendar's are: each is its own labelled node, read
 * by TalkBack as a sentence ("Friday 20 November, found").
 */
@Composable
internal fun Calendar(
    month: ArchiveMonth,
    onMonth: (YearMonth) -> Unit,
    onDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val reduced = reducedMotion()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonthArrow(month.hasPrevious, R.string.home_calendar_previous, previous = true) {
                onMonth(month.month.minusMonths(1))
            }
            Text(
                text = monthTitle(month.month, locale),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .testTag(HomeTags.MONTH)
                    .semantics { heading() },
            )
            MonthArrow(month.hasNext, R.string.home_calendar_next, previous = false) {
                onMonth(month.month.plusMonths(1))
            }
        }
        Text(
            text = monthSummary(month),
            style = MaterialTheme.typography.bodyMedium.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        AnimatedContent(
            targetState = month,
            contentKey = { it.month },
            transitionSpec = { monthTransition(reduced, forward = targetState.month > initialState.month) },
            label = "month",
        ) { shown ->
            MonthGrid(shown, locale, onDay)
        }
        Spacer(Modifier.height(16.dp))
        CalendarKey()
    }
}

/** Another month slides in from its side; a fade under reduced motion. */
private fun monthTransition(reduced: Boolean, forward: Boolean): ContentTransform {
    if (reduced) return fadeIn(ThatsTheWordMotion.fade()) togetherWith fadeOut(ThatsTheWordMotion.fade())
    val sign = if (forward) 1 else -1
    return (
        fadeIn(ThatsTheWordMotion.spatial()) + slideInHorizontally(ThatsTheWordMotion.spatial()) {
            sign * it / 4
        }
        ) togetherWith
        (fadeOut(ThatsTheWordMotion.spatial()) + slideOutHorizontally(ThatsTheWordMotion.spatial()) { -sign * it / 4 })
}

/** A month's arrow, drawn only where there is a month to go to: no dead control. */
@Composable
private fun MonthArrow(visible: Boolean, label: Int, previous: Boolean, onClick: () -> Unit) {
    if (!visible) {
        Spacer(Modifier.size(48.dp))
        return
    }
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (previous) GameIcons.Previous else GameIcons.Next,
            contentDescription = stringResource(label),
        )
    }
}

/** "November 2026", "Novembre 2026", "Noviembre de 2026": the locale's own order, its first letter a capital. */
private fun monthTitle(month: YearMonth, locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMMMy")
    return DateTimeFormatter.ofPattern(pattern, locale).format(month).replaceFirstChar { it.titlecase(locale) }
}

/** The month in one sentence before its days: "3 words found, 1 missed. 1 of them caught up later." */
@Composable
private fun monthSummary(month: ArchiveMonth): String {
    if (month.found == 0 && month.missed == 0) return stringResource(R.string.home_month_none)
    val found = pluralStringResource(R.plurals.home_month_found, month.found, month.found)
    val missed = pluralStringResource(R.plurals.home_month_missed, month.missed, month.missed)
    val counts = when {
        month.found > 0 && month.missed > 0 -> stringResource(R.string.home_month_both, found, missed)
        month.found > 0 -> stringResource(R.string.home_month_one, found)
        else -> stringResource(R.string.home_month_one, missed)
    }
    if (month.caughtUp == 0) return counts
    return counts + " " + pluralStringResource(R.plurals.home_month_caught_up, month.caughtUp, month.caughtUp)
}

@Composable
private fun MonthGrid(month: ArchiveMonth, locale: Locale, onDay: (LocalDate) -> Unit) {
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val lead = (month.days.first().date.dayOfWeek.value - firstDay.value + 7) % 7
    val weeks = (List<ArchiveDay?>(lead) { null } + month.days).chunked(DAYS_A_WEEK)
    Column(
        Modifier
            .widthIn(max = GRID_MAX_WIDTH)
            .fillMaxWidth()
            .testTag(DENSE_TARGETS_TAG),
    ) {
        // The weekdays' initials: the days themselves say their whole date to TalkBack.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .clearAndSetSemantics {},
        ) {
            repeat(DAYS_A_WEEK) { i ->
                Text(
                    text = firstDay.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(DAYS_A_WEEK) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(CELL_GAP / 2),
                    ) {
                        week.getOrNull(i)?.let { DayTile(it, locale, onDay) }
                    }
                }
            }
        }
    }
}

/** How a day is drawn: the game's own tiles, colour and shape together. */
@Immutable
private data class DayLook(val ground: Color, val ink: Color, val border: Color?, val shape: Shape, val scale: Float)

private val SquareTile = RoundedCornerShape(CornerSize(TILE_CORNER_PERCENT))

@Composable
@ReadOnlyComposable
private fun lookOf(status: DayStatus, playable: Boolean = true, today: Boolean = false): DayLook {
    val colors = MaterialTheme.colorScheme
    if (!playable) return DayLook(Color.Transparent, colors.onSurface.copy(alpha = FAINT), null, SquareTile, 1f)
    return when (status) {
        DayStatus.SOLVED -> DayLook(FeedbackColors.RightPlace, FeedbackColors.OnRightPlace, null, SquareTile, 1f)

        DayStatus.MISSED -> DayLook(
            colors.surfaceContainerHighest,
            colors.onSurfaceVariant,
            null,
            SquareTile,
            ABSENT_SCALE,
        )

        DayStatus.IN_PROGRESS -> DayLook(FeedbackColors.Elsewhere, FeedbackColors.OnElsewhere, null, CircleShape, 1f)

        DayStatus.NOT_PLAYED -> DayLook(
            Color.Transparent,
            colors.onSurface,
            if (today) colors.onSurface else colors.outlineVariant,
            SquareTile,
            1f,
        )
    }
}

@Composable
private fun DayTile(day: ArchiveDay, locale: Locale, onDay: (LocalDate) -> Unit) {
    val look = lookOf(day.status, day.playable, day.today)
    val description = dayDescription(day, locale)
    val action = stringResource(
        when (day.status) {
            DayStatus.NOT_PLAYED -> R.string.home_day_play
            DayStatus.IN_PROGRESS -> R.string.home_day_take_up
            DayStatus.SOLVED, DayStatus.MISSED -> R.string.home_day_see
        },
    )
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .testTag(HomeTags.day(day.date))
            .semantics { contentDescription = description }
            .then(
                if (day.playable) {
                    Modifier
                        .clip(look.shape)
                        .clickable(onClickLabel = action) { onDay(day.date) }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val side = maxWidth
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = look.scale
                    scaleY = look.scale
                }
                .background(look.ground, look.shape)
                .then(
                    look.border?.let {
                        Modifier.border(if (day.today) 2.dp else 1.5.dp, it, look.shape)
                    } ?: Modifier,
                ),
        )
        // Sized by the cell, not by the text size, as a tile's letter is: TalkBack reads the whole date.
        val size = with(LocalDensity.current) { (side * NUMBER_TO_CELL).toSp() }
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge.tabular().copy(
                fontSize = size,
                lineHeight = size,
                fontWeight = if (day.today) FontWeight.Bold else FontWeight.Medium,
            ),
            color = look.ink,
            modifier = Modifier.clearAndSetSemantics {},
        )
        if (day.fromArchive && day.status != DayStatus.NOT_PLAYED) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = side * DOT_FROM_BOTTOM)
                    .size(DOT)
                    .background(look.ink, CircleShape),
            )
        }
    }
}

/** "Friday 20 November, today, found": the date in the locale's words, then how it stands. */
@Composable
private fun dayDescription(day: ArchiveDay, locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    val date = DateTimeFormatter.ofPattern(pattern, locale).format(day.date)
    val status = stringResource(
        when {
            !day.playable && day.future -> R.string.home_day_not_yet
            !day.playable -> R.string.home_day_before
            day.status == DayStatus.SOLVED && day.fromArchive -> R.string.home_day_found_later
            day.status == DayStatus.SOLVED -> R.string.home_day_found
            day.status == DayStatus.MISSED && day.fromArchive -> R.string.home_day_missed_later
            day.status == DayStatus.MISSED -> R.string.home_day_missed
            day.status == DayStatus.IN_PROGRESS -> R.string.home_day_under_way
            else -> R.string.home_day_not_played
        },
    )
    return stringResource(if (day.today) R.string.home_day_today else R.string.home_day, date, status)
}

/** The calendar's key, under it: each look with its word. */
@Composable
private fun CalendarKey() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        KeyItem(R.string.home_key_found) { StatusGlyph(DayStatus.SOLVED) }
        KeyItem(R.string.home_key_missed) { StatusGlyph(DayStatus.MISSED) }
        KeyItem(R.string.home_key_under_way) { StatusGlyph(DayStatus.IN_PROGRESS) }
        KeyItem(R.string.home_key_not_played) { StatusGlyph(DayStatus.NOT_PLAYED) }
        KeyItem(R.string.home_key_later) { StatusGlyph(DayStatus.SOLVED, later = true) }
    }
}

@Composable
private fun KeyItem(label: Int, glyph: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        glyph()
        Spacer(Modifier.size(6.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A day's look in small, for the calendar's key and the languages' switch: drawing only, its
 * words beside it.
 */
@Composable
internal fun StatusGlyph(status: DayStatus, modifier: Modifier = Modifier, size: Dp = GLYPH, later: Boolean = false) {
    val look = lookOf(status)
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = look.scale
                scaleY = look.scale
            }
            .background(look.ground, look.shape)
            .then(look.border?.let { Modifier.border(1.5.dp, it, look.shape) } ?: Modifier)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (later) Box(Modifier.size(DOT).background(look.ink, CircleShape))
    }
}

private const val DAYS_A_WEEK = 7
private val GRID_MAX_WIDTH = 420.dp
private val CELL_GAP = 4.dp
private val GLYPH = 14.dp
private val DOT = 4.dp
private const val FAINT = 0.38f

/** A day's number as a share of its cell's side. */
private const val NUMBER_TO_CELL = 0.36f

/** How high the caught-up dot stands from the cell's bottom, as a share of its side. */
private const val DOT_FROM_BOTTOM = 0.12f
