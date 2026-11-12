package com.callbackdev.thatstheword.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.callbackdev.thatstheword.R
import com.callbackdev.thatstheword.core.designsystem.components.LetterTile
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import kotlinx.coroutines.delay

/** The app's pages. One for now: the start page, before any game exists (PLANNING.md §11 Phase 0). */
@Composable
fun ThatsTheWordRoot() {
    Surface(Modifier.fillMaxSize()) {
        StartPage()
    }
}

/**
 * The start page (VISION §1, §2): the name, the line under it, a word composed tile by tile, and,
 * once the last tile has landed, the line the name stands for: the game's moment of victory,
 * shown before any game exists. No button yet: one is drawn when it starts a game (Phase 3). The
 * capitals are drawn, not written, so TalkBack reads the lines as words and the strings stay in
 * sentence case for the translators. Under reduced motion every piece fades in, at the same
 * moment it would have arrived.
 */
@Composable
internal fun StartPage(modifier: Modifier = Modifier) {
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
    }
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
}
