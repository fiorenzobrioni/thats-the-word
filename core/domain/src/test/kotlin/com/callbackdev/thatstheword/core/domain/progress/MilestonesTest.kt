package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.domain.DAY
import com.callbackdev.thatstheword.core.domain.daily
import com.callbackdev.thatstheword.core.domain.free
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Milestone
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MilestonesTest {
    private fun reached(games: List<GameRecord>, notebook: Int = 0) =
        Milestones.reached(Milestones.Facts(games, notebook))

    @Test
    fun `nothing yet`() {
        assertThat(reached(emptyList())).isEmpty()
    }

    @Test
    fun `seven dailies in a row`() {
        val six = (1L..6L).map { daily(DAY.minusDays(it)) }
        assertThat(reached(six)).doesNotContain(Milestone.SEVEN_DAILIES_IN_A_ROW)
        assertThat(reached(six + daily(DAY))).contains(Milestone.SEVEN_DAILIES_IN_A_ROW)
    }

    @Test
    fun `a first win in a foreign language`() {
        assertThat(reached(listOf(free()))).doesNotContain(Milestone.FIRST_FOREIGN_WIN)
        assertThat(reached(listOf(free(GameState.LOST, interfaceLanguage = Language.ENGLISH))))
            .doesNotContain(Milestone.FIRST_FOREIGN_WIN)
        assertThat(
            reached(listOf(free(interfaceLanguage = Language.ENGLISH))),
        ).containsExactly(Milestone.FIRST_FOREIGN_WIN)
    }

    @Test
    fun `a hundred words in the notebook`() {
        assertThat(reached(emptyList(), notebook = 99)).isEmpty()
        assertThat(reached(emptyList(), notebook = 100)).containsExactly(Milestone.HUNDRED_WORDS)
    }

    @Test
    fun `a day's daily won in all three languages`() {
        val two = listOf(daily(DAY), daily(DAY, language = Language.ENGLISH))
        assertThat(reached(two)).doesNotContain(Milestone.ONE_DAY_THREE_LANGUAGES)
        val three = two + daily(DAY, language = Language.SPANISH)
        assertThat(reached(three)).contains(Milestone.ONE_DAY_THREE_LANGUAGES)
        val otherDay = two + daily(DAY.minusDays(1), language = Language.SPANISH)
        assertThat(reached(otherDay)).doesNotContain(Milestone.ONE_DAY_THREE_LANGUAGES)
    }

    @Test
    fun `a milestone is unlocked once and never taken back`() {
        val facts = Milestones.Facts(emptyList(), 100)
        assertThat(Milestones.newlyReached(facts, emptySet())).containsExactly(Milestone.HUNDRED_WORDS)
        assertThat(Milestones.newlyReached(facts, setOf(Milestone.HUNDRED_WORDS))).isEmpty()
        // A notebook that shrinks below a hundred takes nothing away: nothing new, nothing lost.
        assertThat(Milestones.newlyReached(Milestones.Facts(emptyList(), 50), setOf(Milestone.HUNDRED_WORDS))).isEmpty()
    }
}
