package com.callbackdev.thatstheword.core.domain.daily

import com.callbackdev.thatstheword.core.domain.DAY
import com.callbackdev.thatstheword.core.domain.daily
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DailiesTest {
    private val it = Language.ITALIAN

    @Test
    fun `a day finished, then replayed and won - the first result stands`() {
        val missed = daily(DAY, GameState.LOST)
        val replay = daily(DAY, GameState.WON, practice = true)
        val records = listOf(missed, replay)
        assertThat(Dailies.result(records, it, DAY)).isEqualTo(missed)
        assertThat(Dailies.status(records, it, DAY)).isEqualTo(DayStatus.MISSED)
    }

    @Test
    fun `a day with its result makes the next game practice`() {
        assertThat(Dailies.nextIsPractice(emptyList(), it, DAY)).isFalse()
        assertThat(Dailies.nextIsPractice(listOf(daily(DAY, GameState.PLAYING)), it, DAY)).isFalse()
        assertThat(Dailies.nextIsPractice(listOf(daily(DAY, GameState.WON)), it, DAY)).isTrue()
    }

    @Test
    fun `a day's status - not played, in progress, solved, missed`() {
        assertThat(Dailies.status(emptyList(), it, DAY)).isEqualTo(DayStatus.NOT_PLAYED)
        assertThat(Dailies.status(listOf(daily(DAY, GameState.PLAYING)), it, DAY)).isEqualTo(DayStatus.IN_PROGRESS)
        assertThat(Dailies.status(listOf(daily(DAY, GameState.WON)), it, DAY)).isEqualTo(DayStatus.SOLVED)
        assertThat(Dailies.status(listOf(daily(DAY, GameState.LOST)), it, DAY)).isEqualTo(DayStatus.MISSED)
    }

    @Test
    fun `each language has its own day`() {
        val records = listOf(daily(DAY, GameState.WON, language = Language.SPANISH))
        assertThat(Dailies.status(records, it, DAY)).isEqualTo(DayStatus.NOT_PLAYED)
        assertThat(Dailies.status(records, Language.SPANISH, DAY)).isEqualTo(DayStatus.SOLVED)
    }

    @Test
    fun `an archive win for a missed day - the calendar says solved`() {
        val records = listOf(daily(DAY.minusDays(3), GameState.WON, startedOn = DAY))
        assertThat(Dailies.status(records, it, DAY.minusDays(3))).isEqualTo(DayStatus.SOLVED)
    }
}
