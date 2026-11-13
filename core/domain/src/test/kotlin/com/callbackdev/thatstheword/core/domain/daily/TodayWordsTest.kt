package com.callbackdev.thatstheword.core.domain.daily

import com.callbackdev.thatstheword.core.domain.DAY
import com.callbackdev.thatstheword.core.domain.daily
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Clock
import java.time.ZoneId

/** What the home-screen card reads (PLANNING.md §11 Phase 7b): today in each language, and the streak. */
class TodayWordsTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val calendar = DailyCalendar(Clock.fixed(DAY.atTime(9, 30).atZone(zone).toInstant(), zone))

    @Test
    fun `each language's word of today, in the languages' order`() {
        val records = listOf(
            daily(DAY, GameState.WON, Language.ITALIAN, attemptsUsed = 4),
            daily(DAY, GameState.PLAYING, Language.ENGLISH, attemptsUsed = 2),
        )
        val today = TodayWords.of(records, Language.ITALIAN, calendar)
        assertThat(today.date).isEqualTo(DAY)
        assertThat(today.number).isEqualTo(calendar.number(DAY))
        assertThat(
            today.days.map {
                it.language
            },
        ).containsExactly(Language.ITALIAN, Language.ENGLISH, Language.SPANISH).inOrder()
        assertThat(today.of(Language.ITALIAN)).isEqualTo(DayStanding(Language.ITALIAN, DayStatus.SOLVED, 4, 6))
        assertThat(today.of(Language.ENGLISH)).isEqualTo(DayStanding(Language.ENGLISH, DayStatus.IN_PROGRESS, 2, 6))
        assertThat(today.of(Language.SPANISH)).isEqualTo(DayStanding(Language.SPANISH, DayStatus.NOT_PLAYED, 0, 6))
    }

    @Test
    fun `a word missed, then replayed as practice, stays missed`() {
        val records = listOf(
            daily(DAY, GameState.LOST, Language.SPANISH),
            daily(DAY, GameState.WON, Language.SPANISH, practice = true, attemptsUsed = 2),
        )
        val spanish = TodayWords.of(records, Language.ITALIAN, calendar).of(Language.SPANISH)
        assertThat(spanish.status).isEqualTo(DayStatus.MISSED)
        assertThat(spanish.attemptsUsed).isEqualTo(6)
    }

    @Test
    fun `yesterday's word belongs to yesterday`() {
        val records = listOf(daily(DAY.minusDays(1), GameState.WON, Language.ITALIAN))
        val today = TodayWords.of(records, Language.ITALIAN, calendar)
        assertThat(today.of(Language.ITALIAN).status).isEqualTo(DayStatus.NOT_PLAYED)
        // Still running: today's word is not played yet.
        assertThat(today.streak).isEqualTo(1)
    }

    @Test
    fun `the streak is the game language's`() {
        val records = (0L..2L).map { daily(DAY.minusDays(it), GameState.WON, Language.ENGLISH) } +
            daily(DAY.minusDays(1), GameState.WON, Language.ITALIAN)
        assertThat(TodayWords.of(records, Language.ENGLISH, calendar).streak).isEqualTo(3)
        assertThat(TodayWords.of(records, Language.ITALIAN, calendar).streak).isEqualTo(1)
        assertThat(TodayWords.of(records, Language.SPANISH, calendar).streak).isEqualTo(0)
    }
}
