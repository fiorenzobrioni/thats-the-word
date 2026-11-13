package com.callbackdev.thatstheword.feature.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.domain.game.Lesson
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.Mark

/**
 * The guided first game's voice (VISION §12, PLANNING.md §11 Phase 7): under the grid, where the
 * key of the marks stands in every other game, the rule just met, said in a sentence or two. A
 * mark's rule comes with its tile in small, as the row just drew it, so the words point at the
 * drawing. A new lesson arrives on the game's spring as the last one leaves; a fade under reduced
 * motion. With nothing to say, the key of the marks stands there, as in any game.
 *
 * TalkBack hears each lesson once it arrives (a polite live region), after the row's own sentence.
 */
@Composable
internal fun Coach(lessons: List<Lesson>, visible: Boolean) {
    val reduced = reducedMotion()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(ThatsTheWordMotion.effects(reduced)),
        exit = fadeOut(ThatsTheWordMotion.effects(reduced)),
    ) {
        AnimatedContent(
            targetState = lessons,
            transitionSpec = {
                val enter = if (reduced) {
                    fadeIn(ThatsTheWordMotion.fade())
                } else {
                    fadeIn(ThatsTheWordMotion.spatial()) + slideInVertically(ThatsTheWordMotion.playful()) { it / 4 }
                }
                (enter togetherWith fadeOut(ThatsTheWordMotion.effects(reduced)))
                    .using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.Center,
            label = "coach",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = COACH_HEIGHT)
                .padding(top = 6.dp),
        ) { said ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (said.isEmpty()) {
                    MarksKeyLine()
                } else {
                    Lessons(said)
                }
            }
        }
    }
}

@Composable
private fun Lessons(lessons: List<Lesson>) {
    val sentences = lessons.map { lessonText(it) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CoachShape)
            .clearAndSetSemantics {
                contentDescription = sentences.joinToString(" ")
                liveRegion = LiveRegionMode.Polite
                testTag = GameTags.COACH
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val marks = lessons.all { it.mark != null }
        lessons.forEachIndexed { i, lesson ->
            val mark = lesson.mark
            val icon = lesson.icon
            if (mark != null || icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(LESSON_GLYPH), contentAlignment = Alignment.Center) {
                        if (mark != null) {
                            MarkSample(mark, size = LESSON_GLYPH)
                        } else if (icon != null) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(LESSON_GLYPH),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = sentences[i],
                        style = MaterialTheme.typography.bodyMedium.balanced(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Text(
                    text = sentences[i],
                    style = MaterialTheme.typography.bodyMedium.balanced(),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (marks) TextAlign.Start else TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** The key of the marks, as every game shows it, while the guided game has nothing new to say. */
@Composable
private fun MarksKeyLine() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        listOf(Mark.RIGHT_PLACE, Mark.ELSEWHERE, Mark.ABSENT).forEach { mark ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                MarkSample(mark)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(keyName(mark)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The tile a mark's lesson points at. */
private val Lesson.mark: Mark?
    get() = when (this) {
        Lesson.RIGHT_PLACE -> Mark.RIGHT_PLACE
        Lesson.ELSEWHERE -> Mark.ELSEWHERE
        Lesson.ABSENT -> Mark.ABSENT
        else -> null
    }

/** The drawing a lesson about a control points at: the bulb for the hints. */
private val Lesson.icon: ImageVector?
    get() = when (this) {
        Lesson.HINTS -> GameIcons.Hint
        else -> null
    }

@Composable
private fun lessonText(lesson: Lesson): String = when (lesson) {
    // Two sentences, each with its own count, so each language says its plural (lint's rule).
    Lesson.FIRST_GUESS -> pluralStringResource(R.plurals.lesson_attempts, GameSetup.ATTEMPTS, GameSetup.ATTEMPTS) +
        " " + pluralStringResource(R.plurals.lesson_start_letters, GameSetup.LENGTH, GameSetup.LENGTH)

    Lesson.FIVE_LETTERS -> pluralStringResource(R.plurals.lesson_all_letters, GameSetup.LENGTH, GameSetup.LENGTH)

    Lesson.ONLY_WORDS -> stringResource(R.string.lesson_only_words)

    Lesson.RIGHT_PLACE -> stringResource(R.string.lesson_right_place)

    Lesson.ELSEWHERE -> stringResource(R.string.lesson_elsewhere)

    Lesson.ABSENT -> stringResource(R.string.lesson_absent)

    Lesson.KEYBOARD -> stringResource(R.string.lesson_keyboard)

    Lesson.HINTS -> stringResource(R.string.lesson_hints)
}

private val CoachShape = RoundedCornerShape(20.dp)
private val COACH_HEIGHT = 96.dp
private val LESSON_GLYPH = 20.dp
