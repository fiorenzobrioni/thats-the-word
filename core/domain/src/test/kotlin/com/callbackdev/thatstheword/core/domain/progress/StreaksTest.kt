package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.domain.DAY
import com.callbackdev.thatstheword.core.domain.daily
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreaksTest {
    private val it = Language.ITALIAN
    private val today = DAY

    private fun wonOn(vararg daysAgo: Long) = daysAgo.map { daily(today.minusDays(it)) }

    @Test
    fun `yesterday's daily won and today's not yet played - still running`() {
        assertThat(Streaks.current(wonOn(1, 2, 3), it, today)).isEqualTo(3)
    }

    @Test
    fun `today's won adds to it`() {
        assertThat(Streaks.current(wonOn(0, 1, 2, 3), it, today)).isEqualTo(4)
    }

    @Test
    fun `yesterday missed - ended, and today's win starts at 1`() {
        val records = wonOn(2, 3, 4) + daily(today.minusDays(1), GameState.LOST)
        assertThat(Streaks.current(records, it, today)).isEqualTo(0)
        assertThat(Streaks.current(records + daily(today), it, today)).isEqualTo(1)
    }

    @Test
    fun `today's word missed - ended at once, not tomorrow`() {
        val records = wonOn(1, 2, 3) + daily(today, GameState.LOST)
        assertThat(Streaks.current(records, it, today)).isEqualTo(0)
        // A replay won as practice does not mend it, and the longest keeps the run it was.
        assertThat(Streaks.current(records + daily(today, GameState.WON, practice = true), it, today)).isEqualTo(0)
        assertThat(Streaks.longest(records, it)).isEqualTo(3)
    }

    @Test
    fun `today's word under way - still running`() {
        assertThat(Streaks.current(wonOn(1, 2) + daily(today, GameState.PLAYING), it, today)).isEqualTo(2)
    }

    @Test
    fun `a day not played ends it too`() {
        assertThat(Streaks.current(wonOn(2, 3, 4), it, today)).isEqualTo(0)
    }

    @Test
    fun `an archive win for a missed day - the streak untouched`() {
        // Three days ago missed, then won from the archive today: the run of yesterday and the day
        // before stays two, and does not join the days before the gap.
        val records = wonOn(1, 2, 4, 5) + daily(today.minusDays(3), GameState.WON, startedOn = today)
        assertThat(Streaks.current(records, it, today)).isEqualTo(2)
        assertThat(Streaks.longest(records, it)).isEqualTo(2)
    }

    @Test
    fun `hints do not break a streak`() {
        val records = listOf(daily(today, hints = 2), daily(today.minusDays(1), hints = 1))
        assertThat(Streaks.current(records, it, today)).isEqualTo(2)
    }

    @Test
    fun `a practice win is no day's result`() {
        val records = listOf(daily(today.minusDays(1), GameState.LOST), daily(today.minusDays(1), practice = true))
        assertThat(Streaks.current(records, it, today)).isEqualTo(0)
    }

    @Test
    fun `each language has its own streak`() {
        val records = wonOn(0, 1) + daily(today, language = Language.SPANISH)
        assertThat(Streaks.current(records, it, today)).isEqualTo(2)
        assertThat(Streaks.current(records, Language.SPANISH, today)).isEqualTo(1)
        assertThat(Streaks.current(records, Language.ENGLISH, today)).isEqualTo(0)
    }

    @Test
    fun `the longest streak is the longest run ever`() {
        val records = wonOn(0, 1, 5, 6, 7, 8, 20)
        assertThat(Streaks.longest(records, it)).isEqualTo(4)
        assertThat(Streaks.longest(emptyList(), it)).isEqualTo(0)
    }
}
