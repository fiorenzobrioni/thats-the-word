package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.domain.key
import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.model.Language.ENGLISH
import com.callbackdev.thatstheword.core.model.Language.ITALIAN
import com.callbackdev.thatstheword.core.model.Language.SPANISH
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AlphabetTest {
    private val it = Alphabet.of(ITALIAN)
    private val en = Alphabet.of(ENGLISH)
    private val es = Alphabet.of(SPANISH)

    @Test
    fun `the keys are A to Z, and Spanish has its Ñ as a letter of its own`() {
        assertThat(it.size).isEqualTo(26)
        assertThat(en.size).isEqualTo(26)
        assertThat(es.size).isEqualTo(27)
        assertThat(es.letterOf('Ñ')?.index).isEqualTo(26)
        assertThat(it.letterOf('ñ')).isNull()
    }

    @Test
    fun `Italian città is typed CITTA`() {
        assertThat(it.fold("città")?.key(ITALIAN)).isEqualTo("citta")
        assertThat(it.fold("Città")?.key(ITALIAN)).isEqualTo("citta")
        assertThat(it.fold("perché")?.key(ITALIAN)).isEqualTo("perche")
    }

    @Test
    fun `the Italian keyboard types the loanwords' letters`() {
        assertThat(it.fold("taxi")?.key(ITALIAN)).isEqualTo("taxi")
        assertThat(it.fold("jeans")?.key(ITALIAN)).isEqualTo("jeans")
    }

    @Test
    fun `Spanish keeps the ñ and drops every other mark`() {
        assertThat(es.fold("niño")?.key(SPANISH)).isEqualTo("niño")
        assertThat(es.fold("NIÑO")?.key(SPANISH)).isEqualTo("niño")
        assertThat(es.fold("pingüino")?.key(SPANISH)).isEqualTo("pinguino")
        assertThat(es.fold("árbol")?.key(SPANISH)).isEqualTo("arbol")
    }

    @Test
    fun `Spanish ñ against n are two different letters`() {
        val años = word("años", SPANISH)
        val anos = word("anos", SPANISH)
        assertThat(años).isNotEqualTo(anos)
        assertThat(es.pack(años)).isNotEqualTo(es.pack(anos))
    }

    @Test
    fun `outside Spanish the tilde goes like any mark`() {
        assertThat(en.fold("niño")?.key()).isEqualTo("nino")
    }

    @Test
    fun `anything left outside the keys makes no word`() {
        assertThat(it.fold("po'")).isNull()
        assertThat(en.fold("re-do")).isNull()
        assertThat(en.fold("a b")).isNull()
        assertThat(en.fold("abc1")).isNull()
        assertThat(en.fold("")).isNull()
        assertThat(en.fold("straße")).isNull()
    }

    @Test
    fun `packing puts five bits a letter, the first highest, so packed words sort as words`() {
        assertThat(en.pack(word("ab"))).isEqualTo(1L)
        assertThat(en.pack(word("ba"))).isEqualTo(32L)
        val words = listOf("crane", "apple", "zebra", "crank", "abbey").map { word(it) }
        assertThat(words.sortedBy { en.pack(it) }.map { it.key() })
            .containsExactly("abbey", "apple", "crane", "crank", "zebra").inOrder()
    }

    @Test
    fun `a word comes back from its packed form, twelve letters too`() {
        val long = word("abcdefghijkl")
        assertThat(en.unpack(en.pack(long), 12)).isEqualTo(long)
        val ñ = word("ñandu", SPANISH)
        assertThat(es.unpack(es.pack(ñ), 5)).isEqualTo(ñ)
    }

    @Test
    fun `a hardware keyboard's key in either case is the letter`() {
        assertThat(en.letterOf('Q')).isEqualTo(en.letterOf('q'))
        assertThat(en.letterOf('1')).isNull()
    }
}
