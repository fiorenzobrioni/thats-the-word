package com.callbackdev.thatstheword.feature.game

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.callbackdev.thatstheword.core.designsystem.components.GameTile
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Letter
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The game page (PLANNING.md §7.3, §7.4), drawn from its state alone: the grid, the line that says
 * why a word was refused, the keyboard; once the game is over, the line the name stands for and
 * the end card.
 *
 * The motion is played for what happens while the page is open (a row revealed, a word refused, a
 * word found); a game taken up is drawn at rest. No animation gates information: a row's marks
 * are in its sentence for TalkBack the moment it is accepted.
 */
@Composable
fun GameScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onLetter: (Letter) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: Boolean = true,
    onHardwareChar: (Char) -> Boolean = { false },
) {
    val reduced = reducedMotion()
    val view = LocalView.current
    val haptic: (Int) -> Unit = { if (haptics) view.performHapticFeedback(it) }

    // What the page saw when it opened: the rows below this were revealed here, and are played.
    val rowsAtStart = remember { state.rows.size }
    val endedHere = remember { !state.finished }.let { it && state.finished }
    val revealMillis = (state.length - 1) * ThatsTheWordMotion.TILE_STAGGER_MILLIS + REVEAL_SETTLE_MILLIS
    val endMillis = if (endedHere) revealMillis + if (state.state == GameState.WON) WAVE_MILLIS else 0L else 0L

    // The keyboard leaves when the end card arrives, not before: the last row is revealed above it.
    var ending by remember { mutableStateOf(state.finished) }
    LaunchedEffect(state.finished) {
        if (state.finished && !ending) {
            delay(endMillis)
            ending = true
        }
    }

    // TalkBack: the last row, once it is revealed (a polite live region); every refusal.
    var announcement by remember { mutableStateOf("") }
    val lastRowSentence = state.rows.lastOrNull()?.let { rowSentence(state.rows.size, it) }
    LaunchedEffect(state.rows.size) {
        if (state.rows.size > rowsAtStart && lastRowSentence != null) {
            repeat(state.length) {
                delay(ThatsTheWordMotion.TILE_STAGGER_MILLIS)
                haptic(HapticFeedbackConstants.SEGMENT_TICK)
            }
            delay(revealMillis - state.length * ThatsTheWordMotion.TILE_STAGGER_MILLIS)
            announcement = lastRowSentence
            if (state.state == GameState.WON) haptic(HapticFeedbackConstants.CONFIRM)
        }
    }
    LaunchedEffect(state.refusal?.id) {
        if (state.refusal != null) haptic(HapticFeedbackConstants.REJECT)
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val currentState by rememberUpdatedState(state)

    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .focusRequester(focus)
                .focusable()
                .onKeyEvent { event ->
                    // A hardware keyboard types too (PLANNING.md §7.3).
                    if (event.type != KeyEventType.KeyDown || currentState.finished) return@onKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> onEnter().let { true }
                        Key.Backspace, Key.Delete -> onDelete().let { true }
                        else -> event.utf16CodePoint.takeIf { it > 0 }?.let { onHardwareChar(it.toChar()) } ?: false
                    }
                },
        ) {
            TopBar(state, onBack)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // The tiles as large as the column allows (56dp, never under 44): the keyboard's
                // height is counted whether it is there or not, so the grid keeps its size when
                // the keyboard leaves.
                val keyboard = if (ending) KEYBOARD_HEIGHT else 0.dp
                val tile = tileSize(maxWidth, maxHeight - keyboard - REFUSAL_LINE, state.length, state.attempts)
                // While the game is played the grid stands in the middle of the room above the
                // keyboard; once it is over, the end card follows it down the page.
                val viewport = maxHeight
                Column(
                    Modifier
                        .fillMaxSize()
                        .testTag(GameTags.PAGE)
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = if (ending) 0.dp else viewport)
                        .padding(pageGutter(sideInsets = false))
                        .padding(horizontal = ScreenMargin, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (ending) Arrangement.Top else Arrangement.Center,
                ) {
                    Grid(state, tile, rowsAtStart, endedHere, revealMillis, reduced)
                    RefusalLine(state)
                    Box(
                        Modifier
                            .size(1.dp)
                            .semantics {
                                contentDescription = announcement
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                    AnimatedVisibility(
                        visible = ending,
                        enter = if (reduced) {
                            fadeIn(ThatsTheWordMotion.fade())
                        } else {
                            fadeIn(ThatsTheWordMotion.spatial()) +
                                slideInVertically(ThatsTheWordMotion.playful()) { it / 3 }
                        },
                    ) {
                        EndCard(state, onShare, Modifier.padding(top = 8.dp, bottom = 24.dp))
                    }
                }
            }
            AnimatedVisibility(
                visible = !ending,
                enter = fadeIn(ThatsTheWordMotion.effects(reduced)),
                exit = if (reduced) {
                    fadeOut(ThatsTheWordMotion.fade())
                } else {
                    fadeOut(ThatsTheWordMotion.spatial()) +
                        slideOutVertically(ThatsTheWordMotion.spatial()) { it }
                },
            ) {
                Keyboard(
                    state = state,
                    onLetter = {
                        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
                        onLetter(it)
                    },
                    onDelete = {
                        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDelete()
                    },
                    onEnter = {
                        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
                        onEnter()
                    },
                    modifier = Modifier
                        .padding(pageGutter(sideInsets = false))
                        .padding(horizontal = KEYBOARD_MARGIN)
                        .padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
    }
}

/** The page's top: back, and what this game is (its language and number, its date). */
@Composable
private fun TopBar(state: GameUiState, onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val language = stringResource(languageName(state.language))
    val (eyebrow, title) = when (val mode = state.mode) {
        is GameMode.Daily -> stringResource(R.string.game_daily_eyebrow, language, mode.number) to
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(mode.date)

        is GameMode.Free -> language to stringResource(R.string.game_free)

        GameMode.Guided -> language to stringResource(R.string.game_guided)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(pageGutter(sideInsets = false))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(GameIcons.Back, contentDescription = stringResource(R.string.game_back))
        }
        Column(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = eyebrow.uppercase(locale),
                style = ThatsTheWordTheme.type.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { contentDescription = eyebrow },
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        // The back button's width again, so the title stands in the middle of the page.
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun Grid(
    state: GameUiState,
    tile: Dp,
    rowsAtStart: Int,
    endedHere: Boolean,
    revealMillis: Long,
    reduced: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TILE_GAP), modifier = Modifier.padding(vertical = 8.dp)) {
        for (row in 0 until state.attempts) {
            val revealed = state.rows.getOrNull(row)
            val spent = row >= state.attempts - state.spentOnHints
            when {
                revealed != null -> {
                    val sentence = rowSentence(row + 1, revealed)
                    val animate = row >= rowsAtStart
                    val winning = endedHere && state.state == GameState.WON && row == state.rows.lastIndex
                    val stagger = ThatsTheWordMotion.TILE_STAGGER_MILLIS
                    androidx.compose.runtime.key(row, "revealed") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
                            modifier = Modifier.clearAndSetSemantics { contentDescription = sentence },
                        ) {
                            revealed.letters.forEachIndexed { i, letter ->
                                GameTile(
                                    letter = letter,
                                    mark = revealed.marks[i],
                                    size = tile,
                                    revealDelayMillis = if (animate) i * stagger else null,
                                    celebrateDelayMillis = if (winning) revealMillis + i * stagger else null,
                                )
                            }
                        }
                    }
                }

                row == state.rows.size && !state.finished && !spent -> TypingRow(state, row, tile, reduced)

                else -> {
                    val description = if (spent) stringResource(R.string.row_spent, row + 1) else ""
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
                        modifier = Modifier
                            .graphicsLayer { alpha = if (spent) SPENT_ALPHA else 1f }
                            .clearAndSetSemantics { if (spent) contentDescription = description },
                    ) {
                        repeat(state.length) { GameTile(letter = null, mark = null, size = tile) }
                    }
                }
            }
        }
    }
}

/** The row being typed: it shakes sideways when its word is refused (PLANNING.md §7.4). */
@Composable
private fun TypingRow(state: GameUiState, row: Int, tile: Dp, reduced: Boolean) {
    val shake = remember { Animatable(0f) }
    val velocity = with(LocalDensity.current) { SHAKE_VELOCITY.toPx() }
    LaunchedEffect(state.refusal?.id) {
        if (state.refusal != null && !reduced) {
            shake.snapTo(0f)
            shake.animateTo(0f, spring(dampingRatio = 0.22f, stiffness = 1400f), initialVelocity = velocity)
        }
    }
    val sentence = if (state.typed.isEmpty()) {
        ""
    } else {
        stringResource(
            R.string.row_typing,
            row + 1,
            state.typed.joinToString(" "),
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
        modifier = Modifier
            .graphicsLayer { translationX = shake.value }
            .clearAndSetSemantics { if (sentence.isNotEmpty()) contentDescription = sentence },
    ) {
        repeat(state.length) { i -> GameTile(letter = state.typed.getOrNull(i), mark = null, size = tile) }
    }
}

/** Why the last word was refused, in one line above the keyboard, never a toast (PLANNING.md §7.3). */
@Composable
private fun RefusalLine(state: GameUiState) {
    val text = state.refusal?.let { refusalText(it) } ?: ""
    Box(Modifier.fillMaxWidth().heightIn(min = REFUSAL_LINE), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.balanced(),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .testTag(GameTags.REFUSAL)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** The tiles' side: as large as the column allows, 56dp at most, 44dp at least (PLANNING.md §7.3). */
internal fun tileSize(width: Dp, height: Dp, length: Int, attempts: Int): Dp {
    val across = (width - ScreenMargin * 2 - TILE_GAP * (length - 1)) / length
    val down = (height - TILE_GAP * (attempts - 1) - 16.dp) / attempts
    return min(MAX_TILE, min(across, down)).coerceAtLeast(MIN_TILE)
}

/** What the tests find the page's parts by. */
object GameTags {
    const val PAGE = "game_page"
    const val REFUSAL = "game_refusal"
    const val END_CARD = "game_end_card"
}

private val MAX_TILE = 56.dp
private val MIN_TILE = 44.dp
internal val TILE_GAP = 6.dp
private val REFUSAL_LINE = 40.dp
private val SHAKE_VELOCITY = 900.dp
private const val SPENT_ALPHA = 0.4f

/** After the last tile of a row starts, the time its spring takes to settle. */
private const val REVEAL_SETTLE_MILLIS = 420L

/** A word found: the wave across its row, before the end card rises. */
private const val WAVE_MILLIS = 650L
