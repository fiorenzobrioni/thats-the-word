package com.callbackdev.thatstheword.core.words

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The committed files, as the app reads them (PLANNING.md §6.6): the decoder of :core:domain on the
 * bytes `tools/words/` wrote. What the domain's tests check on small files is checked here on the
 * real ones, with no word of a daily written in the test.
 */
@RunWith(AndroidJUnit4::class)
class AssetWordFilesTest {
    private val files = AssetWordFiles(ApplicationProvider.getApplicationContext<android.content.Context>().assets)

    private fun lexicon(language: Language) = Lexicon(files, language)

    private fun key(language: Language, key: String) = requireNotNull(Alphabet.of(language).parse(key))

    @Test
    fun `every language's files open, with the counts of the words' report`() {
        assertThat(lexicon(Language.ITALIAN).accepted.size).isEqualTo(8_258)
        assertThat(lexicon(Language.ENGLISH).accepted.size).isEqualTo(22_528)
        assertThat(lexicon(Language.SPANISH).accepted.size).isEqualTo(13_083)
        assertThat(lexicon(Language.ITALIAN).solutions.size).isEqualTo(687)
        assertThat(lexicon(Language.ENGLISH).solutions.size).isEqualTo(1_295)
        assertThat(lexicon(Language.SPANISH).solutions.size).isEqualTo(1_010)
    }

    @Test
    fun `every length from 3 to 8 ships, each solution accepted and explained in the three languages`() {
        // PLANNING.md §6.6 (§15, 14 Nov 2026): 1.1's and 1.2's lengths are in the app already, read
        // by the same decoder; one glossary per interface language describes them all.
        for (language in Language.entries) {
            for (length in 3..8) {
                val lexicon = Lexicon(files, language, length)
                val words = lexicon.solutions.at(Level.COMPLETE)
                assertThat(words).isNotEmpty()
                assertThat(words.size).isEqualTo(lexicon.solutions.size)
                for (word in words) {
                    assertThat(word.length).isEqualTo(length)
                    assertThat(word in lexicon.accepted).isTrue()
                    for (speaks in Language.entries) {
                        assertThat(lexicon.gloss(word, speaks)?.meaning).isNotNull()
                    }
                }
            }
        }
    }

    @Test
    fun `common words are accepted, and nonsense is not`() {
        val it = lexicon(Language.ITALIAN).accepted
        assertThat(key(Language.ITALIAN, "cuore") in it).isTrue()
        assertThat(key(Language.ITALIAN, "taxi") in it).isFalse() // four letters
        assertThat(key(Language.ITALIAN, "qqqqq") in it).isFalse()
        val es = lexicon(Language.SPANISH).accepted
        assertThat(key(Language.SPANISH, "niños") in es).isTrue()
        assertThat(key(Language.SPANISH, "arbol") in es).isTrue()
    }

    @Test
    fun `Italian città typed as CITTA - the end card says città`() {
        val it = lexicon(Language.ITALIAN)
        val città = key(Language.ITALIAN, "citta")
        assertThat(it.solutions.levelOf(città)).isNotNull()
        assertThat(it.forms.solutionDisplay(città)).isEqualTo("città")
        assertThat(it.gloss(città, Language.ENGLISH)?.translations).contains("city")
        assertThat(lexicon(Language.SPANISH).forms.solutionDisplay(key(Language.SPANISH, "arbol"))).isEqualTo("árbol")
    }

    @Test
    fun `a key form with two display forms - the guess shows both, the solution its own`() {
        val gatto = key(Language.ITALIAN, "gatto")
        val forms = lexicon(Language.ITALIAN).forms
        assertThat(forms.of(gatto).map { it.display }).containsExactly("gatto", "gattò").inOrder()
        assertThat(forms.solutionDisplay(gatto)).isEqualTo("gatto")
    }

    @Test
    fun `every daily is a solution of the daily's levels, so the mask is read as it was written`() {
        for (language in Language.entries) {
            val lexicon = lexicon(language)
            for (n in 1..lexicon.daily.size) {
                val word = lexicon.daily.word(n)
                val level = lexicon.solutions.levelOf(word)
                assertThat(level).isNotNull()
                assertThat(Level.DAILY.includes(requireNotNull(level))).isTrue()
                assertThat(word in lexicon.accepted).isTrue()
            }
        }
    }

    @Test
    fun `the schedule outlasts the build by three years at least`() {
        // PLANNING.md §4.6: a test fails when fewer than three years are left from the build's date.
        val calendar = DailyCalendar(java.time.Clock.systemDefaultZone())
        for (language in Language.entries) {
            val last = calendar.dateOf(lexicon(language).daily.size)
            assertThat(ChronoUnit.DAYS.between(LocalDate.now(), last)).isAtLeast(3 * 365L + 1)
        }
    }

    @Test
    fun `a foreign word offers its hints, a word in the interface's language none`() {
        val it = lexicon(Language.ITALIAN)
        val città = key(Language.ITALIAN, "citta")
        assertThat(
            it.hintKinds(città, Language.ENGLISH),
        ).containsExactly(HintKind.TRANSLATION, HintKind.DEFINITION, HintKind.LETTER)
        assertThat(it.hintKinds(città, Language.ITALIAN)).isEmpty()
    }

    @Test
    fun `a word is found among the English 23,000 in well under a millisecond`() {
        // PLANNING.md §11 Phase 2: under a millisecond on a mid-range phone. On the JVM a lookup is
        // a few microseconds; the bound is the phone's, so a slow machine does not fail the build.
        val accepted = lexicon(Language.ENGLISH).accepted
        val words = lexicon(Language.ENGLISH).solutions.at(Level.COMPLETE)
        repeat(3) { words.forEach { w -> w in accepted } } // warm up
        val rounds = 10
        val start = System.nanoTime()
        repeat(rounds) { words.forEach { w -> check(w in accepted) } }
        val perLookup = (System.nanoTime() - start) / (rounds * words.size)
        assertThat(perLookup).isLessThan(1_000_000L)
    }
}
