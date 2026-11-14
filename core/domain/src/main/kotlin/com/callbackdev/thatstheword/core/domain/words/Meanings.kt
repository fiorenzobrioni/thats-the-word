package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Word

/**
 * What a word means to a player of an interface language (VISION §9, PLANNING.md §6.4): what the
 * end card shows for the solution and for each word tried, and the notebook for each word met.
 *
 * @property written every written form of the word, with its accents (*città*; *pero*, *però*), the
 *   solution's own first.
 * @property lemma the lemma whose meaning this is, when the word is one of its forms (*canti*, a form
 *   of *cantare*); null when the word is the lemma itself.
 * @property translations into the interface language, best first: none in the game's own language,
 *   and none where they were unreliable (the meaning is shown alone then).
 * @property meaning a short definition in the interface language, or null.
 * @property partOfSpeech the word's part of speech in the interface language, beside its meaning
 *   (*sostantivo*, *noun*, *sustantivo*), or null.
 */
data class WordMeaning(
    val written: List<String>,
    val lemma: String?,
    val translations: List<String>,
    val meaning: String?,
    val partOfSpeech: String? = null,
) {
    /** Whether there is anything to say beyond the written form. */
    val known: Boolean get() = translations.isNotEmpty() || meaning != null
}

/**
 * [word]'s meaning in [interfaceLanguage]. A written form that is a lemma is read under its own
 * entry first (so a solution under its own written form, as [Lexicon.gloss] reads it); then the
 * lemmas the forms belong to, in their order. The first entry that says something is the word's.
 */
fun Lexicon.meaning(word: Word, interfaceLanguage: Language): WordMeaning {
    val written = forms.of(word)
    val glossary = glossary(interfaceLanguage)
    val ownEntries = written.filter { it.lemmas.isEmpty() }.map { it.display to null }
    val lemmaEntries = written.flatMap { form -> form.lemmas.map { it to it } }
    val (entry, lemma) = (ownEntries + lemmaEntries)
        .asSequence()
        .mapNotNull { (key, lemma) -> glossary.entry(key)?.let { it to lemma } }
        .firstOrNull { (entry, _) -> entry.translations.isNotEmpty() || entry.meaning != null }
        ?: (null to null)
    val translations = if (interfaceLanguage == language) emptyList() else entry?.translations.orEmpty()
    val meaning = entry?.meaning?.takeUnless { repeats(it, translations) }
    return WordMeaning(
        written = written.map { it.display }.distinct(),
        // A lemma of a form is named only when it is not one of the word's own written forms.
        lemma = lemma?.takeIf { it !in written.map { form -> form.display } },
        translations = translations,
        meaning = meaning,
        // Said with the meaning it belongs to, never alone.
        partOfSpeech = entry?.partOfSpeech?.takeIf { meaning != null },
    )
}

/**
 * Whether a meaning only lists the translations again: a gloss is often its translations
 * ("town, city" under "city, town"), and saying them twice says nothing more.
 */
private fun repeats(meaning: String, translations: List<String>): Boolean {
    if (translations.isEmpty()) return false
    val known = translations.mapTo(HashSet()) { it.trim().lowercase() }
    val items = meaning.trimEnd('.').split(',', ';').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    return items.isNotEmpty() && items.all { it in known }
}
