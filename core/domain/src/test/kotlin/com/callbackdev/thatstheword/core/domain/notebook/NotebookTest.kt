package com.callbackdev.thatstheword.core.domain.notebook

import com.callbackdev.thatstheword.core.domain.DAY
import com.callbackdev.thatstheword.core.domain.daily
import com.callbackdev.thatstheword.core.domain.free
import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotebookTest {
    private val cuore = word("cuore", Language.ITALIAN)

    @Test
    fun `a solution met for the first time is written down, found or missed`() {
        val found = Notebook.afterGame(null, daily(DAY), DAY)
        assertThat(found?.word).isEqualTo(cuore)
        assertThat(found?.language).isEqualTo(Language.ITALIAN)
        assertThat(found?.firstMet).isEqualTo(DAY)
        assertThat(found?.timesMet).isEqualTo(1)
        assertThat(found?.solved).isEqualTo(1)
        assertThat(found?.missed).isEqualTo(0)
        assertThat(found?.savedByHand).isFalse()
        val missed = Notebook.afterGame(null, daily(DAY, GameState.LOST), DAY)
        assertThat(missed?.solved).isEqualTo(0)
        assertThat(missed?.missed).isEqualTo(1)
    }

    @Test
    fun `a word met again keeps its first date and counts the game, hints apart`() {
        val first = Notebook.afterGame(null, daily(DAY), DAY)
        val later = DAY.plusDays(30)
        val again = Notebook.afterGame(first, free(GameState.LOST), later)
        val hinted = Notebook.afterGame(again, daily(later, hints = 2), later)
        assertThat(hinted?.firstMet).isEqualTo(DAY)
        assertThat(hinted?.lastMet).isEqualTo(later)
        assertThat(hinted?.timesMet).isEqualTo(3)
        assertThat(hinted?.solved).isEqualTo(2)
        assertThat(hinted?.missed).isEqualTo(1)
        assertThat(hinted?.withHints).isEqualTo(1)
    }

    @Test
    fun `practice and a game not finished leave the notebook as it was`() {
        assertThat(Notebook.afterGame(null, daily(DAY, practice = true), DAY)).isNull()
        assertThat(Notebook.afterGame(null, daily(DAY, GameState.PLAYING), DAY)).isNull()
    }

    @Test
    fun `the guided first game is a lesson, and keeps nothing`() {
        val lesson = daily(DAY).copy(mode = GameMode.Guided, interfaceLanguage = Language.ENGLISH)
        assertThat(lesson.counted).isFalse()
        assertThat(Notebook.afterGame(null, lesson, DAY)).isNull()
    }

    @Test
    fun `a word tried is added by hand, and a touch undone takes it away`() {
        val added = Notebook.savedByHand(null, Language.ITALIAN, cuore, DAY)
        assertThat(added.savedByHand).isTrue()
        assertThat(added.timesMet).isEqualTo(1)
        assertThat(added.solved + added.missed).isEqualTo(0)
        assertThat(Notebook.unsaved(added)).isNull()
    }

    @Test
    fun `a solution added by hand keeps its games, and keeps them when the touch is undone`() {
        val met = Notebook.afterGame(null, daily(DAY), DAY)!!
        val kept = Notebook.savedByHand(met, Language.ITALIAN, cuore, DAY.plusDays(1))
        assertThat(kept).isEqualTo(met.copy(savedByHand = true))
        assertThat(Notebook.unsaved(kept)).isEqualTo(met)
    }

    @Test
    fun `the search forgives accents and case, and an empty query finds everything`() {
        assertThat(Notebook.matches("citta", listOf("città"))).isTrue()
        assertThat(Notebook.matches("ARBOL", listOf("árbol"))).isTrue()
        assertThat(Notebook.matches("nino", listOf("niño"))).isTrue()
        assertThat(Notebook.matches("heart", listOf("cuore", "heart, core"))).isTrue()
        assertThat(Notebook.matches("cane", listOf("cuore", "heart"))).isFalse()
        assertThat(Notebook.matches("  ", listOf("cuore"))).isTrue()
    }
}
