package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import java.time.LocalDate

/**
 * The modes the progress counts apart (PLANNING.md §4.8, VISION §11): the daily, the same word for
 * everyone, and free play, a word at the player's level. The guided first game is a lesson, not a
 * result: it is counted in neither.
 */
enum class ProgressMode {
    DAILY,
    FREE,
    ;

    fun holds(mode: GameMode): Boolean = when (this) {
        DAILY -> mode is GameMode.Daily
        FREE -> mode is GameMode.Free
    }
}

/**
 * One mode of one language: its [statistics], and how its latest counted game ended, so the bar
 * it added to can be lit ([latestWon] and [latestAttempts]; null when nothing was played).
 */
data class ModeProgress(val statistics: Statistics, val latestWon: Boolean?, val latestAttempts: Int?) {
    companion object {
        fun of(records: List<GameRecord>): ModeProgress {
            val latest = records.lastOrNull { it.counted }
            return ModeProgress(
                statistics = Statistics.of(records),
                latestWon = latest?.let { it.state == GameState.WON },
                latestAttempts = latest?.attemptsUsed,
            )
        }
    }
}

/**
 * A language's progress (PLANNING.md §4.8), all computed from the games, nothing kept: the
 * statistics of each mode, the streak running [today] and the longest one.
 */
data class LanguageProgress(
    val language: Language,
    val modes: Map<ProgressMode, ModeProgress>,
    val streak: Int,
    val longestStreak: Int,
) {
    fun of(mode: ProgressMode): ModeProgress = modes.getValue(mode)

    /** Every counted game of the language, in every mode. */
    val played: Int get() = modes.values.sumOf { it.statistics.played }

    companion object {
        fun of(records: List<GameRecord>, language: Language, today: LocalDate): LanguageProgress {
            val own = records.filter { it.language == language }
            return LanguageProgress(
                language = language,
                modes = ProgressMode.entries.associateWith { mode ->
                    ModeProgress.of(own.filter { mode.holds(it.mode) })
                },
                streak = Streaks.current(records, language, today),
                longestStreak = Streaks.longest(records, language),
            )
        }

        /** The languages with a counted game, in the languages' order: those the page has a story for. */
        fun played(records: List<GameRecord>): List<Language> =
            Language.entries.filter { language -> records.any { it.language == language && it.counted } }
    }
}
