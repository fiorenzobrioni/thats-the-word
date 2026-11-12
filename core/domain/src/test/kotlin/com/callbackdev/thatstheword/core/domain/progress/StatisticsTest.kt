package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.domain.DAY
import com.callbackdev.thatstheword.core.domain.daily
import com.callbackdev.thatstheword.core.model.GameState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StatisticsTest {
    @Test
    fun `games, the share won, the distribution, hints and Hard apart`() {
        val records = listOf(
            daily(DAY, attemptsUsed = 3),
            daily(DAY.minusDays(1), attemptsUsed = 4, hints = 1),
            daily(DAY.minusDays(2), attemptsUsed = 4, hard = true),
            daily(DAY.minusDays(3), GameState.LOST),
        )
        val stats = Statistics.of(records)
        assertThat(stats.played).isEqualTo(4)
        assertThat(stats.won).isEqualTo(3)
        assertThat(stats.winShare).isWithin(1e-9).of(0.75)
        assertThat(stats.distribution).containsExactly(3, 1, 4, 2)
        assertThat(stats.withHints).isEqualTo(1)
        assertThat(stats.withHard).isEqualTo(1)
    }

    @Test
    fun `an archive win counts in the statistics`() {
        val stats = Statistics.of(listOf(daily(DAY.minusDays(5), startedOn = DAY)))
        assertThat(stats.won).isEqualTo(1)
    }

    @Test
    fun `practice and games not finished are left out`() {
        val stats = Statistics.of(listOf(daily(DAY, practice = true), daily(DAY.minusDays(1), GameState.PLAYING)))
        assertThat(stats.played).isEqualTo(0)
        assertThat(stats.winShare).isEqualTo(0.0)
    }
}
