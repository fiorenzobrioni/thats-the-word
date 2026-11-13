package com.callbackdev.thatstheword.feature.home

import androidx.compose.runtime.Immutable
import com.callbackdev.thatstheword.core.domain.daily.ArchiveMonth
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.daily.DayStanding
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
 * The game language's free word left unfinished (PLANNING.md §4.7): its level, the attempts it has
 * used, and whether it is played in Hard.
 */
@Immutable
data class FreeUnderWay(val level: Level, val attemptsUsed: Int, val attempts: Int, val hard: Boolean)

/**
 * Where Hard stands for the game language's two games the page opens (PLANNING.md §4.4): the switch
 * applies to every word not yet begun, and a word begun keeps the rule it started with.
 *
 * @property today today's word is played in Hard, or will be; a day finished says how it was.
 * @property free free play's word is played in Hard: the one under way, or the next.
 * @property todayKept today's word is begun with the rule the switch no longer says.
 * @property freeKept the free word under way is begun with the rule the switch no longer says.
 */
@Immutable
data class HardStanding(
    val today: Boolean,
    val free: Boolean,
    val todayKept: Boolean = false,
    val freeKept: Boolean = false,
) {
    companion object {
        /** A game begun keeps its own Hard; one not yet tried takes the switch's when it is taken up. */
        internal fun of(setting: Boolean, today: GameRecord?, free: GameRecord?): HardStanding {
            fun begun(record: GameRecord?) = record != null && record.attemptsUsed > 0
            val todayBegun = begun(today) || today?.state?.finished == true
            return HardStanding(
                today = if (todayBegun && today != null) today.hard else setting,
                free = if (begun(free) && free != null) free.hard else setting,
                todayKept = today != null && today.state == GameState.PLAYING && begun(today) && today.hard != setting,
                freeKept = free != null && begun(free) && free.hard != setting,
            )
        }
    }
}

/**
 * The start page, all of it, from the settings, the games and the date (PLANNING.md §11 Phase 4):
 * drawn from this alone, so a test draws any day without a database.
 *
 * @property languages today's daily in each of the three languages, in the languages' order.
 * @property streak the game language's streak running today (PLANNING.md §4.8).
 * @property month the calendar's month shown, in the game language.
 * @property level free play's level in the game language.
 * @property freeUnderWay the game language's free game left unfinished, if one is.
 * @property hard the Hard option, for every word not yet begun (PLANNING.md §4.4).
 * @property hardStanding which of the page's games Hard holds for.
 * @property notebookSize how many words the notebook holds, in every language: its card is drawn
 *   once it holds one.
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
    val freeUnderWay: FreeUnderWay?,
    val hard: Boolean,
    val notebookSize: Int = 0,
    val hardStanding: HardStanding = HardStanding(today = hard, free = hard),
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
            notebookSize: Int = 0,
        ): HomeUiState {
            val today = calendar.today()
            val language = settings.gameLanguage
            val free = records.lastOrNull {
                it.language == language && it.mode is GameMode.Free && it.state == GameState.PLAYING
            }
            val daily = records.firstOrNull {
                it.language == language && !it.practice && it.daily?.date == today
            }
            return HomeUiState(
                gameLanguage = language,
                today = today,
                number = calendar.number(today),
                languages = Language.entries.map { languageToday(records, it, today) },
                streak = Streaks.current(records, language, today),
                month = ArchiveMonth.of(month ?: YearMonth.from(today), records, language, calendar, scheduleLength),
                level = settings.levelOf(language),
                freeUnderWay = free?.let {
                    FreeUnderWay(
                        level = (it.mode as GameMode.Free).level,
                        attemptsUsed = it.attemptsUsed,
                        attempts = it.attempts,
                        hard = it.hard,
                    )
                },
                hard = settings.hard,
                notebookSize = notebookSize,
                hardStanding = HardStanding.of(settings.hard, daily, free),
            )
        }

        // The card on the home screen reads the same standing (TodayWords): the two never disagree.
        private fun languageToday(records: List<GameRecord>, language: Language, today: LocalDate): LanguageToday =
            DayStanding.of(records, language, today).let {
                LanguageToday(it.language, it.status, it.attemptsUsed, it.attempts)
            }
    }
}
