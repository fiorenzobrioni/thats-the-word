package com.callbackdev.thatstheword.feature.notebook

import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.markGround
import com.callbackdev.thatstheword.core.designsystem.theme.markStyle
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Mark
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The notebook (VISION §9): the words met, the latest first, each with what it means in the
 * interface's language and how it was met, searched like a small dictionary and filtered by
 * language. A word is touched to read the rest: its meaning in full, the lemma it is a form of,
 * how many times it was met. Drawn from its state alone.
 */
@Composable
fun NotebookScreen(
    state: NotebookUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onLanguage: (Language?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The words opened, by their ids: kept when the page comes back, closed again by a touch.
    val opened = rememberSaveable { mutableStateOf(emptyList<String>()) }
    val gutter = pageGutter()
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
        ) {
            TopBar(state.total, onBack)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(NotebookTags.LIST),
                contentPadding = PaddingValues(
                    start = gutter.start + ScreenMargin,
                    end = gutter.end + ScreenMargin,
                    top = 8.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.total == 0) {
                    item { Note(stringResource(R.string.notebook_empty)) }
                    return@LazyColumn
                }
                item {
                    Text(
                        text = stringResource(R.string.notebook_intro),
                        style = MaterialTheme.typography.bodyLarge.balanced(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    )
                }
                item { SearchField(state.query, onQuery) }
                if (state.languages.size > 1) {
                    item { LanguageFilter(state, onLanguage) }
                }
                if (state.words.isEmpty()) {
                    item { Note(stringResource(R.string.notebook_no_match)) }
                }
                items(state.words, key = { it.id }) { word ->
                    WordRow(
                        word = word,
                        showLanguage = state.filter == null && state.languages.size > 1,
                        open = word.id in opened.value,
                        onToggle = {
                            opened.value =
                                if (word.id in opened.value) opened.value - word.id else opened.value + word.id
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** Back, and the page's name over how many words it holds. */
@Composable
private fun TopBar(total: Int, onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val count = pluralStringResource(R.plurals.notebook_count, total, total)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(pageGutter())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(GameIcons.Back, contentDescription = stringResource(R.string.notebook_back))
        }
        Column(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (total > 0) {
                Text(
                    text = count.uppercase(locale),
                    style = ThatsTheWordTheme.type.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = count },
                )
            }
            Text(
                text = stringResource(R.string.notebook_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.notebook_search)) },
        leadingIcon = { Icon(GameIcons.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQuery("") }) {
                    Icon(GameIcons.Clear, contentDescription = stringResource(R.string.notebook_clear))
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NotebookTags.SEARCH),
    )
}

/** Every language, or one: only when the notebook holds words of two or more. */
@Composable
private fun LanguageFilter(state: NotebookUiState, onLanguage: (Language?) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Chip(stringResource(R.string.notebook_all), selected = state.filter == null) { onLanguage(null) }
        state.languages.forEach { language ->
            Chip(stringResource(languageName(language)), selected = state.filter == language) { onLanguage(language) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        // In the dress's ink, as the app's buttons: its primary would stand beside the marks' colours.
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            selectedLabelColor = MaterialTheme.colorScheme.inverseOnSurface,
        ),
        modifier = Modifier.heightIn(min = 48.dp),
    )
}

/**
 * A word met: its mark (found, missed, kept by hand), as it is written, what it means in a line,
 * how and when it was met. Opened by a touch, it says the rest.
 */
@Composable
private fun WordRow(
    word: NotebookWordUi,
    showLanguage: Boolean,
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val first = word.translations ?: word.meaning
    val shownState = stringResource(if (open) R.string.notebook_opened else R.string.notebook_closed)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(colors.surfaceContainer)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) { stateDescription = shownState }
            .testTag(NotebookTags.word(word.id))
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 6.dp)) { StandingGlyph(word.standing) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word.written, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (showLanguage) {
                        val name = stringResource(languageName(word.language))
                        Text(
                            text = name.uppercase(locale),
                            style = ThatsTheWordTheme.type.eyebrow,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = name },
                        )
                    }
                }
                first?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (open) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.notebook_standing_date, standingText(word), shortDate(word.lastMet)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                if (open) {
                    Spacer(Modifier.height(6.dp))
                    word.lemma?.let {
                        Text(
                            text = stringResource(R.string.notebook_form_of, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    if (word.translations != null && word.meaning != null) {
                        Text(word.meaning, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.notebook_times,
                            word.timesMet,
                            word.timesMet,
                            shortDate(word.firstMet),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Found, missed or kept, drawn as the game draws a letter: the right place's ink square, the absent tile sunk, the notebook's mark. */
@Composable
private fun StandingGlyph(standing: WordStanding) {
    if (standing == WordStanding.KEPT) {
        Icon(
            GameIcons.NotebookKept,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(GLYPH),
        )
        return
    }
    val style = markStyle(if (standing == WordStanding.MISSED) Mark.ABSENT else Mark.RIGHT_PLACE)
    Box(
        Modifier
            .size(GLYPH)
            .graphicsLayer {
                scaleX = style.scale
                scaleY = style.scale
            }
            .markGround(style.ground, RoundedCornerShape(CornerSize(style.cornerPercent.toInt())), style.rim, 1.dp),
    )
}

@Composable
private fun standingText(word: NotebookWordUi): String = stringResource(
    when (word.standing) {
        WordStanding.FOUND -> if (word.withHints) R.string.notebook_found_hints else R.string.notebook_found
        WordStanding.MISSED -> R.string.notebook_missed
        WordStanding.FOUND_AND_MISSED -> R.string.notebook_found_and_missed
        WordStanding.KEPT -> R.string.notebook_kept
    },
)

/** "20 November", "20 novembre", "20 de noviembre": the locale's own order. */
@Composable
private fun shortDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale).format(date)
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.balanced(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
    )
}

@StringRes
private fun languageName(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.notebook_language_it
    Language.ENGLISH -> R.string.notebook_language_en
    Language.SPANISH -> R.string.notebook_language_es
}

/** What the tests find the page's parts by. */
object NotebookTags {
    const val LIST = "notebook_list"
    const val SEARCH = "notebook_search"

    fun word(id: String) = "notebook_word_$id"
}

private val RowShape = RoundedCornerShape(20.dp)
private val GLYPH = 12.dp
