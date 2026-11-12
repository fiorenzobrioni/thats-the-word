package com.callbackdev.thatstheword.feature.home

import androidx.compose.runtime.Immutable
import com.callbackdev.thatstheword.core.domain.daily.ArchiveMonth
import com.callbackdev.thatstheword.core.domain.daily.Dailies
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.progress.Streaks
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.UserSettings
import java.time.LocalDate
import java.time.YearMonth

/**
 * Today's daily in one language, as the start page says it: waiting, under way, found in n, or
 * missed (PLANNING.md §11 Phase 4).
 *
 * @property attemptsUsed the attempts the day's counted game has used, a hint being one.
 */
@Immutable
data class LanguageToday(val language: Language, val status: DayStatus, val attemptsUsed: Int, val attempts: Int)

/**
 * The start page, all of it, from the settings, the games and the date (PLANNING.md §11 Phase 4):
 * drawn from this alone, so a test draws any day without a database.
 *
 * @property languages today's daily in each of the three languages, in the languages' order.
 * @property streak the game language's streak running today (PLANNING.md §4.8).
 * @property month the calendar's month shown, in the game language.
 * @property level free play's level in the game language.
 * @property freeUnderWay the level of the game language's free game left unfinished, if one is.
 * @property hard the Hard option, for the next game (PLANNING.md §4.4).
 */
@Immutable
data class HomeUiState(
    val gameLanguage: Language,
    val today: LocalDate,
    val number: Int,
    val languages: List<LanguageToday>,
    val streak: Int,
    val month: ArchiveMonth,
    val level: Level,
    val freeUnderWay: Level?,
    val hard: Boolean,
) {
    val gameLanguageToday: LanguageToday get() = languages.first { it.language == gameLanguage }

    companion object {
        /**
         * The page for [settings] and [records] on the [calendar]'s today; [month] is the
         * calendar's month asked for (null: today's), [scheduleLength] how many dailies the game
         * language's schedule holds.
         */
        fun of(
            settings: UserSettings,
            records: List<GameRecord>,
            calendar: DailyCalendar,
            month: YearMonth?,
            scheduleLength: Int,
        ): HomeUiState {
            val today = calendar.today()
            val language = settings.gameLanguage
            return HomeUiState(
                gameLanguage = language,
                today = today,
                number = calendar.number(today),
                languages = Language.entries.map { languageToday(records, it, today) },
                streak = Streaks.current(records, language, today),
                month = ArchiveMonth.of(month ?: YearMonth.from(today), records, language, calendar, scheduleLength),
                level = settings.levelOf(language),
                freeUnderWay = records
                    .lastOrNull { it.language == language && it.mode is GameMode.Free && it.state == GameState.PLAYING }
                    ?.let { (it.mode as GameMode.Free).level },
                hard = settings.hard,
            )
        }

        private fun languageToday(records: List<GameRecord>, language: Language, today: LocalDate): LanguageToday {
            // The day's counted game is unique: its first game that is not practice.
            val counted = records.firstOrNull { it.language == language && !it.practice && it.daily?.date == today }
            return LanguageToday(
                language = language,
                status = Dailies.status(records, language, today),
                attemptsUsed = counted?.attemptsUsed ?: 0,
                attempts = counted?.attempts ?: 6,
            )
        }
    }
}
