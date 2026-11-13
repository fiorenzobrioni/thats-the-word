package com.callbackdev.thatstheword.shell

import androidx.lifecycle.ViewModel
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.InterfaceLanguage
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import javax.inject.Provider

/**
 * What the shell opens by itself, outside any page: today's daily of a language, asked for by a
 * launcher shortcut or the daily reminder (PLANNING.md §8), the language asked for becoming the
 * game language, so Back lands on a start page that speaks of it; and the guided first game,
 * played again from Settings (PLANNING.md §11 Phase 7).
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val games: GameRepository,
    private val clock: Provider<Clock>,
    private val interfaceLanguage: InterfaceLanguage,
) : ViewModel() {
    /** The id of [language]'s daily of today, started now if it was not. */
    suspend fun openToday(language: Language): Long {
        settings.setGameLanguage(language)
        return games.dailyGame(
            language,
            DailyCalendar(clock.get()).today(),
            interfaceLanguage.current(),
            settings.current().hard,
        )
    }

    /** The id of the guided first game in the game language, taken up or started afresh. */
    suspend fun openGuided(): Long = games.guidedGame(settings.current().gameLanguage, interfaceLanguage.current())
}
