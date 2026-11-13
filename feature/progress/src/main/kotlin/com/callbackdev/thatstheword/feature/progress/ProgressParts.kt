package com.callbackdev.thatstheword.feature.progress

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.FeedbackColors
import com.callbackdev.thatstheword.core.designsystem.theme.RIGHT_PLACE_CORNER_PERCENT
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.markGround
import com.callbackdev.thatstheword.core.designsystem.theme.markStyle
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.designsystem.theme.rightPlaceRim
import com.callbackdev.thatstheword.core.designsystem.theme.tabular
import com.callbackdev.thatstheword.core.domain.progress.Statistics
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.Milestone
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * The games won by the attempts they used, a hint being one, and the games not found (an X, as a
 * shared result says it). Each row's label is a small tile, the latest game's filled as the grid
 * fills a letter found, so the eye finds where its last game went; the bars are the right place's
 * ink, the X's the absent tile's ground. The bars grow to their length, one after another, the
 * first time; to their new length when a game is added; at once under reduced motion.
 *
 * Each row is one sentence for TalkBack: "In 4 attempts: 9 games."
 */
@Composable
internal fun Distribution(
    statistics: Statistics,
    attempts: Int,
    latestWon: Boolean?,
    latestAttempts: Int?,
    modifier: Modifier = Modifier,
) {
    val rows = (1..attempts).map { n ->
        DistributionRow(n.toString(), statistics.distribution[n] ?: 0, latestWon == true && latestAttempts == n, n)
    } + DistributionRow(
        stringResource(R.string.progress_not_found_mark),
        statistics.lost,
        latestWon == false,
        attempts = null,
    )
    val most = rows.maxOf { it.count }.coerceAtLeast(1)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEachIndexed { index, row -> DistributionBar(row, row.count.toFloat() / most, index) }
    }
}

private data class DistributionRow(val label: String, val count: Int, val latest: Boolean, val attempts: Int?)

@Composable
private fun DistributionBar(row: DistributionRow, fraction: Float, index: Int) {
    val reduced = reducedMotion()
    val resources = LocalResources.current
    val colors = MaterialTheme.colorScheme
    val won = row.attempts != null
    val absent = markStyle(Mark.ABSENT)
    val rim = rightPlaceRim()
    val length = remember { Animatable(0f) }
    LaunchedEffect(fraction, reduced) {
        if (reduced) {
            length.snapTo(fraction)
        } else {
            // The first time, one row after another; afterwards each at once to its new length.
            if (length.value == 0f) delay(BAR_DELAY_MILLIS + index * BAR_STAGGER_MILLIS)
            length.animateTo(fraction, ThatsTheWordMotion.spatial())
        }
    }
    val heading = row.attempts?.let { resources.getQuantityString(R.plurals.progress_row_attempts, it, it) }
        ?: stringResource(R.string.progress_row_not_found)
    val games = resources.getQuantityString(R.plurals.progress_row_games, row.count, row.count)
    val sentence = stringResource(R.string.progress_row, heading, games).let {
        if (row.latest) stringResource(R.string.progress_row_latest, it) else it
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clearAndSetSemantics { contentDescription = sentence }
            .testTag(ProgressTags.row(row.label)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLabel(row.label, latest = row.latest, won = won)
        Spacer(Modifier.width(12.dp))
        BoxWithConstraints(Modifier.weight(1f)) {
            val room = (maxWidth - COUNT_ROOM).coerceAtLeast(0.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.count > 0) {
                    Box(
                        Modifier
                            .width((room * length.value).coerceAtLeast(MIN_BAR))
                            .height(BAR_HEIGHT)
                            .markGround(
                                if (won) FeedbackColors.RightPlace else absent.ground,
                                RoundedCornerShape(6.dp),
                                if (won) rim else null,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = row.count.toString(),
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = if (row.count > 0) colors.onSurface else colors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A row's label on a small tile: empty, with its frame, as a tile still to fill; the latest
 * game's row filled as a letter found is (the right place's ink square), or sunk as a letter absent.
 */
@Composable
private fun RowLabel(label: String, latest: Boolean, won: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(RIGHT_PLACE_CORNER_PERCENT.toInt())
    val absent = markStyle(Mark.ABSENT)
    val ground = when {
        !latest -> null
        won -> FeedbackColors.RightPlace
        else -> absent.ground
    }
    Box(
        Modifier
            .size(LABEL_TILE)
            .then(
                if (ground != null) {
                    Modifier.markGround(ground, shape, if (won) rightPlaceRim() else null, 1.dp)
                } else {
                    Modifier.border(1.5.dp, colors.outlineVariant, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Sized by its tile, not by the text size, as a tile's letter is: at twice the text size the
        // figure still fits its tile, and the row's sentence says it for whoever needs it larger.
        val size = with(LocalDensity.current) { (LABEL_TILE * LABEL_SHARE).toSp() }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.tabular().copy(fontSize = size, lineHeight = size),
            color = when {
                latest && won -> FeedbackColors.OnRightPlace
                latest -> absent.ink
                else -> colors.onSurface
            },
        )
    }
}

/**
 * A milestone (VISION §11): its badge (a tile filled with a check once reached, an empty tile
 * until then), its name and what it asks, then the date it was reached, or how near it is, drawn as
 * the tiles it fills (seven, three) or a line (a hundred words). Read as one phrase.
 */
@Composable
internal fun MilestoneRow(milestone: MilestoneUi, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val reachedOn = milestone.reachedOn
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag(ProgressTags.milestone(milestone.milestone.name.lowercase()))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Badge(reached = reachedOn != null)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(milestoneName(milestone.milestone)), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(milestoneLine(milestone.milestone)),
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            if (reachedOn != null) {
                Text(
                    text = stringResource(R.string.progress_reached_on, reachedDate(reachedOn, today)),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface,
                )
            } else {
                Nearness(milestone)
            }
        }
    }
}

@Composable
private fun Badge(reached: Boolean) {
    val shape = RoundedCornerShape(RIGHT_PLACE_CORNER_PERCENT.toInt())
    Box(
        Modifier
            .size(BADGE)
            .then(
                if (reached) {
                    Modifier.markGround(FeedbackColors.RightPlace, shape, rightPlaceRim())
                } else {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (reached) {
            Icon(
                GameIcons.Check,
                contentDescription = null,
                tint = FeedbackColors.OnRightPlace,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** How near a milestone is: its tiles filled so far, or its line, and the words "3 of 7"; "Not yet" for a first. */
@Composable
private fun Nearness(milestone: MilestoneUi) {
    val colors = MaterialTheme.colorScheme
    if (milestone.target <= 1) {
        Text(
            stringResource(R.string.progress_not_yet),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (milestone.target <= MAX_TILES) {
            Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val shape = RoundedCornerShape(RIGHT_PLACE_CORNER_PERCENT.toInt())
                val rim = rightPlaceRim()
                repeat(milestone.target) { i ->
                    Box(
                        Modifier
                            .size(MINI_TILE)
                            .then(
                                if (i < milestone.reached) {
                                    Modifier.markGround(FeedbackColors.RightPlace, shape, rim, 1.dp)
                                } else {
                                    Modifier.border(1.5.dp, colors.outlineVariant, shape)
                                },
                            ),
                    )
                }
            }
        } else {
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .width(LINE_WIDTH)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHighest)
                    .clearAndSetSemantics {},
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(milestone.reached.toFloat() / milestone.target)
                        .fillMaxHeight()
                        .markGround(FeedbackColors.RightPlace, CircleShape, rightPlaceRim(), 1.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.progress_near, milestone.reached, milestone.target),
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = colors.onSurfaceVariant,
        )
    }
}

@StringRes
private fun milestoneName(milestone: Milestone): Int = when (milestone) {
    Milestone.SEVEN_DAILIES_IN_A_ROW -> R.string.progress_milestone_seven
    Milestone.FIRST_FOREIGN_WIN -> R.string.progress_milestone_foreign
    Milestone.HUNDRED_WORDS -> R.string.progress_milestone_hundred
    Milestone.ONE_DAY_THREE_LANGUAGES -> R.string.progress_milestone_three
}

@StringRes
private fun milestoneLine(milestone: Milestone): Int = when (milestone) {
    Milestone.SEVEN_DAILIES_IN_A_ROW -> R.string.progress_milestone_seven_line
    Milestone.FIRST_FOREIGN_WIN -> R.string.progress_milestone_foreign_line
    Milestone.HUNDRED_WORDS -> R.string.progress_milestone_hundred_line
    Milestone.ONE_DAY_THREE_LANGUAGES -> R.string.progress_milestone_three_line
}

private val LABEL_TILE = 28.dp

/** A row label's figure, as a share of its tile's side. */
private const val LABEL_SHARE = 0.5f
private val BAR_HEIGHT = 20.dp
private val MIN_BAR = 6.dp

/** What a bar leaves after it for its count. */
private val COUNT_ROOM = 44.dp
private val BADGE = 40.dp
private val MINI_TILE = 14.dp
private val LINE_WIDTH = 160.dp

/** A milestone of up to seven is drawn as its tiles; past that, as a line. */
private const val MAX_TILES = 7
private const val BAR_DELAY_MILLIS = 250L
private const val BAR_STAGGER_MILLIS = 45L
