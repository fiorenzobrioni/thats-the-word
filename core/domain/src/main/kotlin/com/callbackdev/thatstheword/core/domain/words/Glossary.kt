package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Word

/**
 * A written form of a key form (PLANNING.md §4.1): *città* of `citta`, with the lemmas it is a form
 * of (*canti* is a form of *canto* and *cantare*); none for a lemma.
 */
data class WrittenForm(val display: String, val lemmas: List<String> = emptyList())

/**
 * The written forms of a language's accepted words. A key with no record is written as its key.
 * A solution's own written form is the first of its record (`tools/words/`, PLANNING §15, «Phase
 * 2»): several lemmas can share a key (*citta* and *città*), the most frequent being the solution.
 */
class Forms(file: WordsFile) {
    private val table = TextTable(file.also { require(it.kind == WordsFile.Kind.FORMS) { "not a forms file" } })
    private val alphabet = Alphabet.of(file.language)

    val language: Language = file.language

    /** Every written form of [word], the solution's own first when it is one. */
    fun of(word: Word): List<WrittenForm> {
        val key = alphabet.keyForm(word)
        val record = table.find(key) ?: return listOf(WrittenForm(key))
        return record.map { field ->
            val items = TextTable.items(field)
            WrittenForm(items.first(), items.drop(1))
        }
    }

    /** How a solution is written (*città*, *árbol*): the end card's word. */
    fun solutionDisplay(word: Word): String = of(word).first().display
}

/** What a word means in the interface language (PLANNING.md §6.4). */
data class GlossEntry(
    /** Its translations into the interface language, best first; none when unreliable. */
    val translations: List<String>,
    /** A short meaning in the interface language, or null. */
    val meaning: String?,
    /** The translation the meaning was read through, when it was (PLANNING.md §6.4), or null. */
    val meaningVia: String?,
)

/**
 * The meanings of a game language's words in one interface language, by written form (a lemma's
 * display form: *città*, *cantare*).
 */
class Glossary(file: WordsFile) {
    private val table = TextTable(file.also { require(it.kind == WordsFile.Kind.GLOSSARY) { "not a glossary" } })

    val language: Language = file.language
    val interfaceLanguage: Language = Language.ofCode(file.extra)

    fun entry(display: String): GlossEntry? {
        val record = table.find(display) ?: return null
        return GlossEntry(
            translations = TextTable.items(record.getOrElse(0) { "" }),
            meaning = record.getOrNull(1)?.takeIf { it.isNotEmpty() },
            meaningVia = record.getOrNull(2)?.takeIf { it.isNotEmpty() },
        )
    }
}
