package com.callbackdev.thatstheword.core.domain.game

import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.model.Word
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class FreePlayTest {
    private val level = listOf("crane", "stale", "nacho", "speed", "trace").map { word(it) }

    private fun played(key: String, solved: Boolean = true) = FreePlay.Played(word(key), solved)

    private fun draws(history: List<FreePlay.Played>): Set<Word> =
        (0 until 200).mapTo(HashSet()) { FreePlay.draw(level, history, Random(it)) }

    @Test
    fun `never one solved in free play before`() {
        val history = listOf(played("crane"), played("stale"))
        assertThat(draws(history)).containsExactly(word("nacho"), word("speed"), word("trace"))
    }

    @Test
    fun `never one of the last 200 free games, even missed`() {
        val history = listOf(played("crane", solved = false))
        assertThat(draws(history)).doesNotContain(word("crane"))
    }

    @Test
    fun `a missed word comes back once it is out of the last 200`() {
        val filler = (0 until FreePlay.RECENT).map { FreePlay.Played(word("zzzzz"), solved = false) }
        val history = listOf(played("crane", solved = false)) + filler
        assertThat(draws(history)).contains(word("crane"))
    }

    @Test
    fun `free play at a level where every word was played - the oldest come back`() {
        val history = listOf(played("speed"), played("crane"), played("stale"), played("nacho"), played("trace"))
        assertThat(draws(history)).containsExactly(word("speed"))
        assertThat(FreePlay.draw(level, history + played("speed"), Random(0))).isEqualTo(word("crane"))
    }

    @Test
    fun `the same random, the same word`() {
        assertThat(FreePlay.draw(level, emptyList(), Random(7))).isEqualTo(FreePlay.draw(level, emptyList(), Random(7)))
    }
}
