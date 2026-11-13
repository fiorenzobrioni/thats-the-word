package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.domain.DAY
import com.callbackdev.thatstheword.core.domain.daily
import com.callbackdev.thatstheword.core.domain.free
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A language's progress (PLANNING.md §4.8): the modes counted apart, the streaks, the latest game. */
class ProgressTest {
    private val records = listOf(
        daily(DAY.minusDays(3), attemptsUsed = 4),
        daily(DAY.minusDays(2), attemptsUsed = 3, hints = 1),
        daily(DAY.minusDays(1), state = GameState.LOST),
        daily(DAY, attemptsUsed = 5, hard = true),
        // A replay as practice counts nowhere.
        daily(DAY.minusDays(1), attemptsUsed = 2, practice = true),
        free(attemptsUsed = 2),
        free(GameState.LOST),
        daily(DAY, language = Language.ENGLISH),
    )

    @Test
    fun `the daily and free play are counted apart`() {
        val italian = LanguageProgress.of(records, Language.ITALIAN, DAY)
        val daily = italian.of(ProgressMode.DAILY).statistics
        assertThat(daily.played).isEqualTo(4)
        assertThat(daily.won).isEqualTo(3)
        assertThat(daily.lost).isEqualTo(1)
        assertThat(daily.distribution).containsExactly(3, 1, 4, 1, 5, 1)
        assertThat(daily.withHints).isEqualTo(1)
        assertThat(daily.withHard).isEqualTo(1)
        val free = italian.of(ProgressMode.FREE).statistics
        assertThat(free.played).isEqualTo(2)
        assertThat(free.distribution).containsExactly(2, 1)
        assertThat(italian.played).isEqualTo(6)
    }

    @Test
    fun `the latest counted game lights its bar`() {
        val italian = LanguageProgress.of(records, Language.ITALIAN, DAY)
        val daily = italian.of(ProgressMode.DAILY)
        // The practice replay started after today's game is not the latest: it does not count.
        assertThat(daily.latestWon).isTrue()
        assertThat(daily.latestAttempts).isEqualTo(5)
        val free = italian.of(ProgressMode.FREE)
        assertThat(free.latestWon).isFalse()
    }

    @Test
    fun `the streak runs today and the longest is kept`() {
        val italian = LanguageProgress.of(records, Language.ITALIAN, DAY)
        assertThat(italian.streak).isEqualTo(1)
        assertThat(italian.longestStreak).isEqualTo(2)
    }

    @Test
    fun `a language never played has nothing to say`() {
        val spanish = LanguageProgress.of(records, Language.SPANISH, DAY)
        assertThat(spanish.played).isEqualTo(0)
        assertThat(spanish.of(ProgressMode.DAILY).latestWon).isNull()
        assertThat(spanish.streak).isEqualTo(0)
        assertThat(LanguageProgress.played(records)).containsExactly(Language.ITALIAN, Language.ENGLISH).inOrder()
    }

    @Test
    fun `the guided game is a lesson, not a result`() {
        assertThat(
            ProgressMode.entries.none {
                it.holds(com.callbackdev.thatstheword.core.model.GameMode.Guided)
            },
        ).isTrue()
    }
}
