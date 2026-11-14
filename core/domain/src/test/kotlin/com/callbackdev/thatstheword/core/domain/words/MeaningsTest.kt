package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.domain.TestFiles
import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Language.ENGLISH
import com.callbackdev.thatstheword.core.model.Language.ITALIAN
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MeaningsTest {
    private fun glossary(interfaceLanguage: Language, records: Map<String, List<List<String>>>) =
        TestFiles.table('G', ITALIAN, 0, records, extra = interfaceLanguage.code)

    private val files = mapOf(
        "it/forms-5.bin" to TestFiles.table(
            'F',
            ITALIAN,
            5,
            mapOf(
                "citta" to listOf(listOf("città"), listOf("citta")),
                "canti" to listOf(listOf("canti", "canto", "cantare")),
                "pesca" to listOf(listOf("pesca"), listOf("pésca")),
                "zuppa" to listOf(listOf("zuppa")),
                "torta" to listOf(listOf("torta")),
            ),
        ),
        "it/gloss-en.bin" to glossary(
            ENGLISH,
            mapOf(
                "città" to listOf(listOf("city", "town"), listOf("A large town."), listOf()),
                "canto" to listOf(listOf(), listOf(), listOf()),
                "cantare" to listOf(listOf("to sing"), listOf("To produce musical sounds."), listOf()),
                "pésca" to listOf(listOf("fishing"), listOf(), listOf()),
                "zuppa" to listOf(listOf(), listOf("A soup of bread."), listOf("soup"), listOf("noun")),
                "torta" to listOf(listOf("cake", "pie"), listOf("Pie, cake."), listOf()),
            ),
        ),
        "it/gloss-it.bin" to glossary(
            ITALIAN,
            mapOf("città" to listOf(listOf(), listOf("Centro abitato."), listOf())),
        ),
    )
    private val lexicon = Lexicon({ files.getValue(it).duplicate() }, ITALIAN)

    @Test
    fun `a solution is read under its own written form, the accents kept`() {
        val città = lexicon.meaning(word("citta", ITALIAN), ENGLISH)
        assertThat(
            città,
        ).isEqualTo(WordMeaning(listOf("città", "citta"), null, listOf("city", "town"), "A large town."))
        assertThat(città.known).isTrue()
    }

    @Test
    fun `the part of speech comes with its meaning, and a file without it reads as before`() {
        // PLANNING.md §6.4: the glossary's fourth field, written in the interface language.
        assertThat(lexicon.meaning(word("zuppa", ITALIAN), ENGLISH).partOfSpeech).isEqualTo("noun")
        assertThat(lexicon.meaning(word("citta", ITALIAN), ENGLISH).partOfSpeech).isNull()
    }

    @Test
    fun `in the game's own language, the definition alone`() {
        val città = lexicon.meaning(word("citta", ITALIAN), ITALIAN)
        assertThat(città.translations).isEmpty()
        assertThat(città.meaning).isEqualTo("Centro abitato.")
    }

    @Test
    fun `a form is read under the first of its lemmas that says something, and names it`() {
        val canti = lexicon.meaning(word("canti", ITALIAN), ENGLISH)
        assertThat(canti.written).containsExactly("canti")
        assertThat(canti.lemma).isEqualTo("cantare")
        assertThat(canti.translations).containsExactly("to sing")
    }

    @Test
    fun `a key with two written forms shows both, and the one that has a meaning gives it`() {
        val pesca = lexicon.meaning(word("pesca", ITALIAN), ENGLISH)
        assertThat(pesca.written).containsExactly("pesca", "pésca").inOrder()
        assertThat(pesca.lemma).isNull()
        assertThat(pesca.translations).containsExactly("fishing")
    }

    @Test
    fun `a meaning without a translation is shown alone, and a word unknown says nothing`() {
        val zuppa = lexicon.meaning(word("zuppa", ITALIAN), ENGLISH)
        assertThat(zuppa.translations).isEmpty()
        assertThat(zuppa.meaning).isEqualTo("A soup of bread.")
        val unknown = lexicon.meaning(word("zuppa", ITALIAN), ITALIAN)
        assertThat(unknown).isEqualTo(WordMeaning(listOf("zuppa"), null, emptyList(), null))
        assertThat(unknown.known).isFalse()
    }

    @Test
    fun `a meaning that only lists the translations again is not said twice`() {
        val torta = lexicon.meaning(word("torta", ITALIAN), ENGLISH)
        assertThat(torta.translations).containsExactly("cake", "pie").inOrder()
        assertThat(torta.meaning).isNull()
    }
}
