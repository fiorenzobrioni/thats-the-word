package com.callbackdev.thatstheword.shell

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.thatstheword.R
import com.callbackdev.thatstheword.core.designsystem.components.LetterTile
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.feature.game.GameRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Where the shell can go: the start page, and a game by its id. Saved with the activity's state. */
@Serializable
data object StartKey : NavKey

@Serializable
data class GameKey(val gameId: Long) : NavKey

/**
 * The app's pages (Navigation 3): the start page, and the game page above it. Each page's view
 * models live as long as its entry on the back stack; predictive back pops it.
 */
@Composable
fun ThatsTheWordRoot() {
    val backStack = rememberNavBackStack(StartKey)
    val reduced = reducedMotion()
    Surface(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { pageTransition(reduced, forward = true) },
            popTransitionSpec = { pageTransition(reduced, forward = false) },
            predictivePopTransitionSpec = { pageTransition(reduced, forward = false) },
            entryProvider = entryProvider {
                entry<StartKey> { StartRoute(onOpenGame = { backStack.add(GameKey(it)) }) }
                entry<GameKey> { key -> GameRoute(gameId = key.gameId, onBack = { backStack.removeLastOrNull() }) }
            },
        )
    }
}

/** A page arriving slides a little and fades, on the family's spring; a fade alone under reduced motion. */
private fun pageTransition(reduced: Boolean, forward: Boolean): ContentTransform {
    if (reduced) return fadeIn(ThatsTheWordMotion.fade()) togetherWith fadeOut(ThatsTheWordMotion.fade())
    val sign = if (forward) 1 else -1
    return (
        fadeIn(ThatsTheWordMotion.spatial()) + slideInHorizontally(ThatsTheWordMotion.spatial()) {
            sign * it / 6
        }
        ) togetherWith
        (fadeOut(ThatsTheWordMotion.spatial()) + slideOutHorizontally(ThatsTheWordMotion.spatial()) { -sign * it / 6 })
}

/** The start page with today's daily, from its view model. */
@Composable
private fun StartRoute(onOpenGame: (Long) -> Unit) {
    val viewModel = hiltViewModel<StartViewModel>()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    StartPage(today = today, onPlay = { scope.launch { onOpenGame(viewModel.openToday()) } })
}

/**
 * The start page (VISION §1, §2): the name, the line under it, a word composed tile by tile, and,
 * once the last tile has landed, the line the name stands for: the game's moment of victory;
 * then today's daily in the game language, to play, take up or see again (provisional, until the
 * start page proper of Phase 4). The
 * capitals are drawn, not written, so TalkBack reads the lines as words and the strings stay in
 * sentence case for the translators. Under reduced motion every piece fades in, at the same
 * moment it would have arrived.
 */
@Composable
internal fun StartPage(today: TodayUi?, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val name = stringResource(R.string.app_name)
    val tagline = stringResource(R.string.start_tagline)
    val word = stringResource(R.string.start_sample_word)
    Column(
        modifier
            .fillMaxSize()
            .testTag(ShellTags.PAGE)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
            .padding(pageGutter())
            .padding(horizontal = ScreenMargin, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = name.uppercase(locale),
            style = ThatsTheWordTheme.type.wordmark,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = name
                heading()
            },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = tagline.uppercase(locale),
            style = ThatsTheWordTheme.type.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { contentDescription = tagline },
        )
        Spacer(Modifier.height(64.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = word },
        ) {
            word.uppercase(locale).forEachIndexed { index, letter ->
                LetterTile(
                    letter = letter,
                    appearDelayMillis = FIRST_TILE_MILLIS + index * ThatsTheWordMotion.TILE_STAGGER_MILLIS,
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        VictoryLine(
            text = stringResource(R.string.start_victory),
            delayMillis = FIRST_TILE_MILLIS + word.length * ThatsTheWordMotion.TILE_STAGGER_MILLIS + SETTLE_MILLIS,
        )
        if (today != null) {
            Spacer(Modifier.height(48.dp))
            TodayButton(today, onPlay)
        }
    }
}

/** Today's daily: play it, take it up, or see how it went. Under it, its language and number. */
@Composable
private fun TodayButton(today: TodayUi, onPlay: () -> Unit) {
    val label = stringResource(
        when (today.status) {
            DayStatus.NOT_PLAYED -> R.string.start_play
            DayStatus.IN_PROGRESS -> R.string.start_continue
            DayStatus.SOLVED, DayStatus.MISSED -> R.string.start_see
        },
    )
    Button(onClick = onPlay, modifier = Modifier.heightIn(min = 56.dp).testTag(ShellTags.PLAY)) {
        Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.start_today_caption, stringResource(languageName(today.language)), today.number),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** The game language's name, in the interface's words. */
private fun languageName(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.start_language_it
    Language.ENGLISH -> R.string.start_language_en
    Language.SPANISH -> R.string.start_language_es
}

/**
 * "That's the word.": it rises a little and settles once the word is whole, as it will greet
 * every word found. Under reduced motion it fades in at the same moment.
 */
@Composable
private fun VictoryLine(text: String, delayMillis: Long) {
    val reduced = reducedMotion()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(reduced) {
        delay(delayMillis)
        progress.animateTo(1f, ThatsTheWordMotion.spatial(reduced))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.graphicsLayer {
            alpha = progress.value.coerceIn(0f, 1f)
            if (!reduced) translationY = (1f - progress.value) * 12.dp.toPx()
        },
    )
}

/** The first tile waits for the page to settle, so the wave is seen. */
private const val FIRST_TILE_MILLIS = 250L

/** After the last tile starts, the time it takes to land before the line arrives. */
private const val SETTLE_MILLIS = 350L

/** What the shell's tests find the page by. */
internal object ShellTags {
    const val PAGE = "start_page"
    const val PLAY = "start_play"
}
