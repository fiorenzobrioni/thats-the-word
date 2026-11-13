package com.callbackdev.thatstheword.feature.game

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.callbackdev.thatstheword.core.designsystem.components.GameTile
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.sound.GameSound
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.markGround
import com.callbackdev.thatstheword.core.designsystem.theme.markStyle
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Mark
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
    onNext: () -> Unit = {},
    haptics: Boolean = true,
    onHardwareChar: (Char) -> Boolean = { false },
    onTakeHint: (HintKind) -> Unit = {},
    onToggleNotebook: (Int) -> Unit = {},
    hintsOpenAtStart: Boolean = false,
    onSound: (GameSound) -> Unit = {},
) {
    val reduced = reducedMotion()
    val view = LocalView.current
    val haptic: (Int) -> Unit = { if (haptics) view.performHapticFeedback(it) }
    // The key's touch: felt and heard together (PLANNING.md §7.5).
    val tap: () -> Unit = {
        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        onSound(GameSound.KEY)
    }

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

    // TalkBack: the last row, once it is revealed (a polite live region); every refusal; a hint
    // taken, what it shows.
    var announcement by remember { mutableStateOf("") }
    val lastRowSentence = state.rows.lastOrNull()?.let { rowSentence(state.rows.size, it) }
    val hintAnnouncement = state.takenHints.lastOrNull()?.let { hint ->
        hintSentence(state.attempts - state.takenHints.size + 1, hint)
    } ?: ""
    LaunchedEffect(state.rows.size) {
        if (state.rows.size > rowsAtStart && lastRowSentence != null) {
            // Each tile's note as its colour arrives: a row plays its result as a short phrase.
            val marks = state.rows.last().marks
            repeat(state.length) { i ->
                delay(ThatsTheWordMotion.TILE_STAGGER_MILLIS)
                haptic(HapticFeedbackConstants.SEGMENT_TICK)
                onSound(GameSound.of(marks[i]))
            }
            delay(revealMillis - state.length * ThatsTheWordMotion.TILE_STAGGER_MILLIS)
            announcement = lastRowSentence
            if (state.state == GameState.WON) {
                haptic(HapticFeedbackConstants.CONFIRM)
                onSound(GameSound.WIN)
            }
        }
    }
    LaunchedEffect(state.refusal?.id) {
        if (state.refusal != null) {
            haptic(HapticFeedbackConstants.REJECT)
            onSound(GameSound.REFUSAL)
        }
    }

    // The hints stand in the keyboard's place while they are open: two touches to take one.
    var hintsOpen by rememberSaveable { mutableStateOf(hintsOpenAtStart) }
    if (state.finished || !state.hintsAvailable) hintsOpen = false
    val hintsAtStart = remember { state.takenHints.size }
    LaunchedEffect(state.takenHints.size) {
        if (state.takenHints.size > hintsAtStart) announcement = hintAnnouncement
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
            TopBar(state, onBack, hintsOpen = hintsOpen, onHints = { hintsOpen = !hintsOpen })
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // The tiles as large as the column allows (56dp, never under 44): the keyboard's
                // height is counted whether it is there or not, so the grid keeps its size when
                // the keyboard leaves.
                val keyboard = if (ending) KEYBOARD_HEIGHT else 0.dp
                // The guided game keeps a lesson's room under the grid, so the grid does not move
                // when one is said.
                val under = if (state.guided) COACH else MARKS_KEY
                val tile =
                    tileSize(maxWidth, maxHeight - keyboard - REFUSAL_LINE - under, state.length, state.attempts)
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
                    Grid(state, tile, rowsAtStart, hintsAtStart, endedHere, revealMillis, reduced)
                    if (state.guided) {
                        Coach(state.lessons, visible = !ending)
                    } else {
                        MarksKey(visible = !ending)
                    }
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
                        EndCard(
                            state = state,
                            onShare = onShare,
                            onNext = onNext,
                            onToggleNotebook = onToggleNotebook,
                            onSound = onSound,
                            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                        )
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
                val bottom = Modifier
                    .padding(pageGutter(sideInsets = false))
                    .padding(horizontal = KEYBOARD_MARGIN)
                    .padding(top = 4.dp, bottom = 12.dp)
                AnimatedContent(
                    targetState = hintsOpen,
                    transitionSpec = {
                        fadeIn(ThatsTheWordMotion.effects(reduced)) togetherWith
                            fadeOut(ThatsTheWordMotion.effects(reduced))
                    },
                    label = "keyboard",
                ) { open ->
                    if (open) {
                        HintsPanel(
                            state = state,
                            onTake = {
                                haptic(HapticFeedbackConstants.CONFIRM)
                                onSound(GameSound.RIGHT_PLACE)
                                hintsOpen = false
                                onTakeHint(it)
                            },
                            onClose = { hintsOpen = false },
                            modifier = bottom,
                        )
                    } else {
                        Keyboard(
                            state = state,
                            onLetter = {
                                tap()
                                onLetter(it)
                            },
                            onDelete = {
                                tap()
                                onDelete()
                            },
                            onEnter = {
                                tap()
                                onEnter()
                            },
                            modifier = bottom,
                        )
                    }
                }
            }
        }
    }
}

/** The page's top: back, and what this game is (its language and number, its date). */
@Composable
private fun TopBar(state: GameUiState, onBack: () -> Unit, hintsOpen: Boolean, onHints: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val language = stringResource(languageName(state.language))
    val (eyebrowOfMode, title) = when (val mode = state.mode) {
        is GameMode.Daily -> stringResource(
            if (state.practice) R.string.game_practice_eyebrow else R.string.game_daily_eyebrow,
            language,
            mode.number,
        ) to
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(mode.date)

        is GameMode.Free -> language to stringResource(R.string.game_free)

        GameMode.Guided -> language to stringResource(R.string.game_guided)
    }
    // A game in Hard says so above its title: the rule it keeps is the player's to remember.
    val eyebrow = if (state.hard) stringResource(R.string.game_eyebrow_hard, eyebrowOfMode) else eyebrowOfMode
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
        if (state.hintsAvailable && !state.finished) {
            // The hints, in a game whose language is not the interface's (VISION §5).
            val label = stringResource(R.string.hints_open)
            val shown = stringResource(if (hintsOpen) R.string.hints_shown else R.string.hints_hidden)
            IconButton(
                onClick = onHints,
                modifier = Modifier
                    .testTag(GameTags.HINTS)
                    .semantics { stateDescription = shown },
            ) {
                Icon(
                    GameIcons.Hint,
                    contentDescription = label,
                    tint = if (hintsOpen) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        } else {
            // The back button's width again, so the title stands in the middle of the page.
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun Grid(
    state: GameUiState,
    tile: Dp,
    rowsAtStart: Int,
    hintsAtStart: Int,
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

                // A row spent on a hint: the first hint the last row, the next the row above, so
                // a hint stays where it was drawn (PLANNING.md §7.3).
                spent && state.takenHints.getOrNull(state.attempts - 1 - row) != null -> {
                    val index = state.attempts - 1 - row
                    androidx.compose.runtime.key(row, "hint") {
                        HintRow(
                            hint = state.takenHints[index],
                            number = row + 1,
                            length = state.length,
                            tile = tile,
                            animate = index >= hintsAtStart,
                            reduced = reduced,
                        )
                    }
                }

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
    // TalkBack: where the player stands ("Row 3 of 6"), and each letter typed or deleted heard as
    // the row reads back, as a text field reads back what is typed in it (a polite live region).
    val sentence = if (state.typed.isEmpty()) {
        stringResource(R.string.row_waiting, row + 1, state.attempts)
    } else {
        stringResource(R.string.row_typing, row + 1, state.attempts, state.typed.joinToString(" "))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(TILE_GAP),
        modifier = Modifier
            .graphicsLayer { translationX = shake.value }
            .clearAndSetSemantics {
                contentDescription = sentence
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        repeat(state.length) { i ->
            GameTile(letter = state.typed.getOrNull(i), mark = null, size = tile, active = i == state.typed.size)
        }
    }
}

/**
 * The three marks in a line under the grid, while the game is played: the tile of each, in small,
 * with its words, so a player who has not met the game yet, or comes back to it, reads the grid
 * without a page of rules (VISION §12). Quiet: the dress's quiet ink, the size of a note. TalkBack
 * reads each row's marks in words already, so the key is a line of text to it.
 */
@Composable
private fun MarksKey(visible: Boolean) {
    val reduced = reducedMotion()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(ThatsTheWordMotion.effects(reduced)),
        exit = fadeOut(ThatsTheWordMotion.effects(reduced)),
    ) {
        // A line on a phone; at a larger text size it wraps rather than cutting a word.
        FlowRow(
            Modifier
                .fillMaxWidth()
                .heightIn(min = MARKS_KEY)
                .padding(top = 4.dp)
                .testTag(GameTags.MARKS_KEY),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            itemVerticalAlignment = Alignment.CenterVertically,
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
}

/** A mark's tile in small: its ground and its shape, nothing to read. */
@Composable
internal fun MarkSample(mark: Mark, size: Dp = MARK_SAMPLE) {
    val style = markStyle(mark)
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = style.scale
                scaleY = style.scale
            }
            .markGround(style.ground, RoundedCornerShape(CornerSize(style.cornerPercent.toInt())), style.rim, 1.dp)
            .clearAndSetSemantics {},
    )
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
    const val NEXT = "game_next"
    const val MARKS_KEY = "game_marks_key"
    const val MEANING = "game_meaning"
    const val MILESTONE = "game_milestone"
    const val HINTS = "game_hints"
    const val HINTS_PANEL = "game_hints_panel"
    const val COACH = "game_coach"
    const val LESSON_DONE = "game_lesson_done"

    fun hint(kind: HintKind) = "game_hint_${kind.name.lowercase()}"

    fun hintRow(kind: HintKind) = "game_hint_row_${kind.name.lowercase()}"

    fun notebook(index: Int) = "game_notebook_$index"
}

private val MAX_TILE = 56.dp
private val MIN_TILE = 44.dp
internal val TILE_GAP = 6.dp
private val REFUSAL_LINE = 40.dp
private val MARKS_KEY = 32.dp
private val COACH = 104.dp
private val MARK_SAMPLE = 12.dp
private val SHAKE_VELOCITY = 900.dp
internal const val SPENT_ALPHA = 0.4f

/** After the last tile of a row starts, the time its spring takes to settle. */
private const val REVEAL_SETTLE_MILLIS = 420L

/** A word found: the wave across its row, before the end card rises. */
private const val WAVE_MILLIS = 650L
