package com.callbackdev.thatstheword.feature.notebook

import androidx.compose.runtime.Immutable
import com.callbackdev.thatstheword.core.domain.notebook.Notebook
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.domain.words.meaning
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.NotebookEntry
import java.time.LocalDate

/** How a word met stands, drawn as the game draws a letter: found, missed, or kept by hand. */
enum class WordStanding {
    FOUND,
    MISSED,
    FOUND_AND_MISSED,
    KEPT,
}

/**
 * A word of the notebook as its page shows it: as it is written, what it means in the interface's
 * language (read from the words' files, never copied: PLANNING.md §5), and what happened to it.
 *
 * @property id the word's language and key form: one entry a word and language.
 */
@Immutable
data class NotebookWordUi(
    val id: String,
    val language: Language,
    val written: String,
    val lemma: String?,
    val translations: String?,
    val meaning: String?,
    val standing: WordStanding,
    val withHints: Boolean,
    val timesMet: Int,
    val firstMet: LocalDate,
    val lastMet: LocalDate,
) {
    /** What the search reads: the written forms and the translations. */
    internal val searchable: List<String> = listOfNotNull(written, translations)

    companion object {
        fun of(entry: NotebookEntry, lexicon: Lexicon, interfaceLanguage: Language): NotebookWordUi {
            val meaning = lexicon.meaning(entry.word, interfaceLanguage)
            val games = entry.solved + entry.missed
            return NotebookWordUi(
                id = "${entry.language.tag}:${Alphabet.of(entry.language).keyForm(entry.word)}",
                language = entry.language,
                // A game's solution is its own written form (*città*); a word tried, every form its
                // letters can be (*pero, però*).
                written = if (games > 0) meaning.written.first() else meaning.written.joinToString(", "),
                lemma = meaning.lemma,
                translations = meaning.translations.takeIf { it.isNotEmpty() }?.joinToString(", "),
                meaning = meaning.meaning,
                standing = when {
                    games == 0 -> WordStanding.KEPT
                    entry.missed == 0 -> WordStanding.FOUND
                    entry.solved == 0 -> WordStanding.MISSED
                    else -> WordStanding.FOUND_AND_MISSED
                },
                withHints = entry.withHints > 0,
                timesMet = entry.timesMet,
                firstMet = entry.firstMet,
                lastMet = entry.lastMet,
            )
        }
    }
}

/**
 * The notebook's page (VISION §9), all of it, from the words met, the search and the language
 * chosen: drawn from this alone, so a test draws any notebook without a database.
 *
 * @property total how many words the notebook holds, in every language.
 * @property languages the languages it holds words of, in the languages' order: the filter offers
 *   them when there are two or more.
 * @property filter the language shown, or null for every one.
 * @property words the words that match, the latest met first.
 */
@Immutable
data class NotebookUiState(
    val total: Int,
    val languages: List<Language>,
    val filter: Language?,
    val query: String,
    val words: List<NotebookWordUi>,
) {
    companion object {
        fun of(all: List<NotebookWordUi>, query: String, filter: Language?): NotebookUiState {
            val languages = Language.entries.filter { language -> all.any { it.language == language } }
            val shown = filter?.takeIf { it in languages }
            return NotebookUiState(
                total = all.size,
                languages = languages,
                filter = shown,
                query = query,
                words = all.filter {
                    (shown == null || it.language == shown) && Notebook.matches(query, it.searchable)
                },
            )
        }
    }
}
