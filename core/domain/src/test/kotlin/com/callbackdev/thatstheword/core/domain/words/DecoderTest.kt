package com.callbackdev.thatstheword.core.domain.words

import com.callbackdev.thatstheword.core.domain.TestFiles
import com.callbackdev.thatstheword.core.domain.key
import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Language.ENGLISH
import com.callbackdev.thatstheword.core.model.Language.ITALIAN
import com.callbackdev.thatstheword.core.model.Language.SPANISH
import com.callbackdev.thatstheword.core.model.Level
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer

class DecoderTest {
    private fun file(buffer: ByteBuffer) = WordsFile(buffer)

    @Test
    fun `the header says the kind, the language, the length and the count`() {
        val f = file(TestFiles.accepted(SPANISH, listOf("niños", "arbol")))
        assertThat(f.kind).isEqualTo(WordsFile.Kind.ACCEPTED)
        assertThat(f.language).isEqualTo(SPANISH)
        assertThat(f.length).isEqualTo(5)
        assertThat(f.count).isEqualTo(2)
        assertThat(f.width).isEqualTo(4)
    }

    @Test
    fun `a file that is not a words file, or of a newer version, is refused`() {
        assertThrows(IllegalArgumentException::class.java) { file(ByteBuffer.wrap(ByteArray(16))) }
        val newer = TestFiles.accepted(ENGLISH, listOf("crane")).also { it.put(4, 2.toByte()) }
        assertThrows(IllegalArgumentException::class.java) { file(newer) }
    }

    @Test
    fun `four bytes a word up to six letters, eight from seven to twelve`() {
        assertThat(WordsFile.wordWidth(5)).isEqualTo(4)
        assertThat(WordsFile.wordWidth(6)).isEqualTo(4)
        assertThat(WordsFile.wordWidth(7)).isEqualTo(8)
        assertThat(WordsFile.wordWidth(12)).isEqualTo(8)
        assertThrows(IllegalArgumentException::class.java) { WordsFile.wordWidth(13) }
    }

    @Test
    fun `an accepted word is found, any other is not`() {
        val accepted =
            AcceptedWords(file(TestFiles.accepted(ENGLISH, listOf("crane", "zebra", "apple", "nacho", "abbey"))))
        listOf("abbey", "apple", "crane", "nacho", "zebra").forEach { assertThat(word(it) in accepted).isTrue() }
        listOf("aaaaa", "crank", "zzzzz", "zebrb").forEach { assertThat(word(it) in accepted).isFalse() }
        assertThat(word("cran") in accepted).isFalse()
    }

    @Test
    fun `Spanish ñ against n - añejo is a word, anejo is not`() {
        val accepted = AcceptedWords(file(TestFiles.accepted(SPANISH, listOf("niños", "arbol", "añejo"))))
        assertThat(word("añejo", SPANISH) in accepted).isTrue()
        assertThat(word("anejo", SPANISH) in accepted).isFalse()
    }

    @Test
    fun `seven-letter words are read from eight-byte slots`() {
        val keys = listOf("example", "zymurgy", "abalone")
        val accepted = AcceptedWords(file(TestFiles.accepted(ENGLISH, keys)))
        keys.forEach { assertThat(word(it) in accepted).isTrue() }
        assertThat(word("exampla") in accepted).isFalse()
    }

    @Test
    fun `the solutions keep their levels, and a level draws every easier one's words`() {
        val solutions = Solutions(
            file(
                TestFiles.solutions(
                    ENGLISH,
                    mapOf(
                        "crane" to 0,
                        "nacho" to 1,
                        "zebra" to 2,
                        "abbey" to 3,
                    ),
                ),
            ),
        )
        assertThat(solutions.levelOf(word("crane"))).isEqualTo(Level.BASE)
        assertThat(solutions.levelOf(word("abbey"))).isEqualTo(Level.COMPLETE)
        assertThat(solutions.levelOf(word("apple"))).isNull()
        assertThat(solutions.at(Level.BASE).map { it.key() }).containsExactly("crane")
        assertThat(solutions.at(Level.INTERMEDIATE).map { it.key() }).containsExactly("crane", "nacho")
        assertThat(solutions.at(Level.COMPLETE)).hasSize(4)
    }

    @Test
    fun `the daily schedule is read entry by entry, unmasked`() {
        val keys = listOf("cuore", "notte", "sedia", "cuore")
        val schedule = DailySchedule(file(TestFiles.daily(ITALIAN, keys)))
        assertThat(schedule.size).isEqualTo(4)
        assertThat((1..4).map { schedule.word(it).key(ITALIAN) }).isEqualTo(keys)
        assertThrows(IllegalArgumentException::class.java) { schedule.word(0) }
        assertThrows(IllegalArgumentException::class.java) { schedule.word(5) }
    }

    @Test
    fun `the schedule is not plain text`() {
        val buffer = TestFiles.daily(ITALIAN, listOf("cuore"))
        val plain = Alphabet.of(ITALIAN).pack(word("cuore", ITALIAN))
        assertThat(WordsFile(buffer).slot(0)).isNotEqualTo(plain)
    }

    private val forms = Forms(
        file(
            TestFiles.table(
                'F',
                ITALIAN,
                5,
                mapOf(
                    "citta" to listOf(listOf("città"), listOf("citta")),
                    "canti" to listOf(listOf("canti", "canto", "cantare")),
                    "pesca" to listOf(listOf("pesca"), listOf("pescà")),
                ),
            ),
        ),
    )

    @Test
    fun `Italian città typed as CITTA - the end card says città`() {
        assertThat(forms.solutionDisplay(word("citta", ITALIAN))).isEqualTo("città")
    }

    @Test
    fun `a key form with two display forms - both shown for a guess, the solution's own for the solution`() {
        assertThat(forms.of(word("pesca", ITALIAN)).map { it.display }).containsExactly("pesca", "pescà").inOrder()
        assertThat(forms.solutionDisplay(word("pesca", ITALIAN))).isEqualTo("pesca")
    }

    @Test
    fun `a form says its lemmas, and a key with no record is written as itself`() {
        assertThat(forms.of(word("canti", ITALIAN))).containsExactly(WrittenForm("canti", listOf("canto", "cantare")))
        assertThat(forms.of(word("notte", ITALIAN))).containsExactly(WrittenForm("notte"))
    }

    @Test
    fun `the table is searched by its keys' bytes, accents after every plain letter`() {
        val table = TextTable(
            file(
                TestFiles.table(
                    'G',
                    ITALIAN,
                    0,
                    mapOf(
                        "zuppa" to listOf(listOf("soup")),
                        "città" to listOf(listOf("city")),
                        "citta" to listOf(listOf("x")),
                    ),
                    extra = 1,
                ),
            ),
        )
        assertThat(table.keys()).containsExactly("citta", "città", "zuppa").inOrder()
        assertThat(table.find("città")).containsExactly("city")
        assertThat(table.find("citt")).isNull()
        assertThat(table.find("cittaa")).isNull()
        assertThat(table.find("zz")).isNull()
    }

    private fun glossary(interfaceLanguage: Language, records: Map<String, List<List<String>>>) =
        TestFiles.table('G', ITALIAN, 0, records, extra = interfaceLanguage.code)

    @Test
    fun `a glossary gives the translations, the meaning and the word it was read through`() {
        val g = Glossary(
            file(
                glossary(
                    ENGLISH,
                    mapOf(
                        "città" to listOf(listOf("city", "town"), listOf("town, city"), listOf()),
                        "guado" to listOf(listOf(), listOf("a ford"), listOf("ford")),
                    ),
                ),
            ),
        )
        assertThat(g.interfaceLanguage).isEqualTo(ENGLISH)
        assertThat(g.entry("città")).isEqualTo(GlossEntry(listOf("city", "town"), "town, city", null))
        assertThat(g.entry("guado")).isEqualTo(GlossEntry(emptyList(), "a ford", "ford"))
        assertThat(g.entry("zuppa")).isNull()
    }

    @Test
    fun `a lexicon opens its files by language and name, and says which hints a word can give`() {
        val files = mapOf(
            "it/forms-5.bin" to
                TestFiles.table('F', ITALIAN, 5, mapOf("citta" to listOf(listOf("città"), listOf("citta")))),
            "it/gloss-en.bin" to
                glossary(ENGLISH, mapOf("città" to listOf(listOf("city"), listOf("town, city"), listOf()))),
            "it/gloss-es.bin" to
                glossary(SPANISH, mapOf("città" to listOf(listOf(), listOf("ciudad grande"), listOf()))),
        )
        val lexicon = Lexicon({ files.getValue(it).duplicate() }, ITALIAN)
        val città = word("citta", ITALIAN)
        assertThat(lexicon.gloss(città, ENGLISH)?.translations).containsExactly("city")
        assertThat(
            lexicon.hintKinds(città, ENGLISH),
        ).containsExactly(HintKind.TRANSLATION, HintKind.DEFINITION, HintKind.LETTER)
        assertThat(lexicon.hintKinds(città, SPANISH)).containsExactly(HintKind.DEFINITION, HintKind.LETTER)
        assertThat(lexicon.hintKinds(città, ITALIAN)).isEmpty()
    }

    @Test
    fun `a file of another language is refused`() {
        val lexicon = Lexicon({ TestFiles.accepted(ENGLISH, listOf("crane")) }, ITALIAN)
        assertThrows(IllegalArgumentException::class.java) { lexicon.accepted }
    }
}
