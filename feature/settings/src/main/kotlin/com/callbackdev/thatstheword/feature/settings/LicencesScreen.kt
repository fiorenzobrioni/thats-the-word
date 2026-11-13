package com.callbackdev.thatstheword.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.PageTopBar
import com.callbackdev.thatstheword.core.designsystem.components.arrive
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter

/** One thing that ships and is not ours, or is ours under a licence: its name, what it gives, its licence. */
private data class Credit(
    @param:StringRes val name: Int,
    @param:StringRes val gives: Int,
    @param:StringRes val licence: Int,
)

private val GAME =
    listOf(Credit(R.string.licences_game_name, R.string.licences_game_gives, R.string.licences_game_licence))

private val TYPEFACES = listOf(
    Credit(R.string.licences_google_sans, R.string.licences_google_sans_gives, R.string.licences_ofl),
    Credit(R.string.licences_inter, R.string.licences_inter_gives, R.string.licences_ofl),
)

private val WORDS = listOf(
    Credit(R.string.licences_hunspell_it, R.string.licences_hunspell_it_gives, R.string.licences_gpl3),
    Credit(R.string.licences_hunspell_es, R.string.licences_hunspell_es_gives, R.string.licences_hunspell_es_licence),
    Credit(R.string.licences_scowl, R.string.licences_scowl_gives, R.string.licences_scowl_licence),
    Credit(R.string.licences_wordfreq, R.string.licences_wordfreq_gives, R.string.licences_cc_by_sa),
    Credit(R.string.licences_wiktionary, R.string.licences_wiktionary_gives, R.string.licences_cc_by_sa),
)

/**
 * Licences and sources (VISION §10, PLANNING.md §6.7): the game's own licence, the typefaces, and
 * every source of the words with what it gives and under which licence, as each asks to be
 * credited where the player can read it, not only in a file of the repository. The sounds are the
 * game's own code, and say so.
 */
@Composable
fun LicencesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val arrived = rememberSaveable { mutableStateOf(false) }
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
        ) {
            PageTopBar(
                title = stringResource(R.string.licences_title),
                backLabel = stringResource(R.string.settings_back),
                onBack = onBack,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .testTag(SettingsTags.LICENCES_PAGE)
                    .verticalScroll(rememberScrollState())
                    .padding(pageGutter())
                    .padding(horizontal = ScreenMargin)
                    .padding(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Text(
                    text = stringResource(R.string.licences_intro),
                    style = MaterialTheme.typography.bodyLarge.balanced(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .arrive(0, arrived),
                )
                Credits(R.string.licences_section_game, GAME, Modifier.arrive(1, arrived))
                Credits(R.string.licences_section_typefaces, TYPEFACES, Modifier.arrive(2, arrived))
                Credits(
                    R.string.licences_section_words,
                    WORDS,
                    Modifier.arrive(3, arrived),
                    R.string.licences_meanings_note,
                )
                SettingsSection(stringResource(R.string.licences_section_sounds), Modifier.arrive(4, arrived)) {
                    Text(
                        text = stringResource(R.string.licences_sounds),
                        style = MaterialTheme.typography.bodyMedium.balanced(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Credits(@StringRes title: Int, credits: List<Credit>, modifier: Modifier, @StringRes note: Int? = null) {
    SettingsSection(stringResource(title), modifier) {
        credits.forEachIndexed { i, credit ->
            if (i > 0) RowDivider()
            Column(
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(stringResource(credit.name), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(credit.gives),
                    style = MaterialTheme.typography.bodyMedium.balanced(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(credit.licence),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (note != null) {
            NoteCard(stringResource(note), Modifier.padding(bottom = 12.dp))
        }
    }
}
