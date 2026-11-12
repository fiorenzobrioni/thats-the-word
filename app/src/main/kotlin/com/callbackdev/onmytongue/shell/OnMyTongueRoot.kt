package com.callbackdev.onmytongue.shell

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.onmytongue.R
import com.callbackdev.onmytongue.core.designsystem.components.LetterTile
import com.callbackdev.onmytongue.core.designsystem.theme.OnMyTongueMotion
import com.callbackdev.onmytongue.core.designsystem.theme.OnMyTongueTheme
import com.callbackdev.onmytongue.core.designsystem.theme.ScreenMargin
import com.callbackdev.onmytongue.core.designsystem.theme.balanced
import com.callbackdev.onmytongue.core.designsystem.theme.padding
import com.callbackdev.onmytongue.core.designsystem.theme.pageGutter

/** The app's pages. One for now: the start page, before any game exists (PLANNING.md §11 Phase 0). */
@Composable
fun OnMyTongueRoot() {
    Surface(Modifier.fillMaxSize()) {
        StartPage()
    }
}

/**
 * The start page's words (VISION §2): the name, the line under it, today's promise, and a word
 * on its tiles, each tile arriving a moment after the one before. No button yet: one is drawn
 * when it starts a game (Phase 3). The capitals are drawn, not written, so TalkBack reads the
 * lines as words and the strings stay in sentence case for the translators.
 */
@Composable
internal fun StartPage(modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val name = stringResource(R.string.app_name)
    val tagline = stringResource(R.string.start_tagline)
    val word = stringResource(R.string.start_sample_word)
    val almost = stringResource(R.string.start_almost)
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
            style = OnMyTongueTheme.type.wordmark,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = name
                heading()
            },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = tagline.uppercase(locale),
            style = OnMyTongueTheme.type.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { contentDescription = tagline },
        )
        Spacer(Modifier.height(56.dp))
        Text(
            text = stringResource(R.string.start_today),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.start_rules),
            style = MaterialTheme.typography.bodyLarge.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = word },
        ) {
            word.uppercase(locale).forEachIndexed { index, letter ->
                LetterTile(
                    letter = letter,
                    appearDelayMillis = FIRST_TILE_MILLIS + index * OnMyTongueMotion.TILE_STAGGER_MILLIS,
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = almost.uppercase(locale),
            style = OnMyTongueTheme.type.eyebrow,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { contentDescription = almost },
        )
    }
}

/** The first tile waits for the page to settle, so the wave is seen. */
private const val FIRST_TILE_MILLIS = 250L

/** What the shell's tests find the page by. */
internal object ShellTags {
    const val PAGE = "start_page"
}
