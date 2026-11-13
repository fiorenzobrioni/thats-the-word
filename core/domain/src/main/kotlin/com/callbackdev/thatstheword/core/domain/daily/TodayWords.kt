package com.callbackdev.thatstheword.core.domain.daily

import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.progress.Streaks
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.Language
import java.time.LocalDate

/**
 * How a language's daily of a date stands: waiting, under way, found in n, or missed (PLANNING.md
 * §11 Phase 4), as the start page's switch and the home-screen card say it.
 *
 * @property attemptsUsed the attempts the day's counted game has used, a hint being one; 0 while
 *   nothing was played.
 */
data class DayStanding(val language: Language, val status: DayStatus, val attemptsUsed: Int, val attempts: Int) {
    companion object {
        fun of(records: List<GameRecord>, language: Language, date: LocalDate): DayStanding {
            // The day's counted game is unique: its first game that is not practice.
            val counted = records.firstOrNull { it.language == language && !it.practice && it.daily?.date == date }
            return DayStanding(
                language = language,
                status = Dailies.status(records, language, date),
                attemptsUsed = counted?.attemptsUsed ?: 0,
                attempts = counted?.attempts ?: GameSetup.ATTEMPTS,
            )
        }
    }
}

/**
 * Today's word in every language and the game language's streak (PLANNING.md §11 Phase 7b): what
 * the home-screen card draws, read from the games and the date alone.
 *
 * @property days the three languages in their order.
 * @property streak the game language's streak running today (PLANNING.md §4.8).
 */
data class TodayWords(
    val date: LocalDate,
    val number: Int,
    val gameLanguage: Language,
    val days: List<DayStanding>,
    val streak: Int,
) {
    fun of(language: Language): DayStanding = days.first { it.language == language }

    companion object {
        fun of(records: List<GameRecord>, gameLanguage: Language, calendar: DailyCalendar): TodayWords {
            val today = calendar.today()
            return TodayWords(
                date = today,
                number = calendar.number(today),
                gameLanguage = gameLanguage,
                days = Language.entries.map { DayStanding.of(records, it, today) },
                streak = Streaks.current(records, gameLanguage, today),
            )
        }
    }
}
