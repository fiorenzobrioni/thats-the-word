package com.callbackdev.thatstheword.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.InterfaceLanguage
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.daily.Dailies
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider

/** Today's daily in the game language, as the start page offers it. */
data class TodayUi(val language: Language, val date: LocalDate, val number: Int, val status: DayStatus)

/**
 * The provisional start page's one game (PLANNING.md §11 Phase 3): today's daily in the game
 * language, started or taken up. Phase 4 brings the start page proper: every language, the
 * calendar, free play, the change of day seen live.
 */
@HiltViewModel
class StartViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val games: GameRepository,
    private val clock: Provider<Clock>,
    private val interfaceLanguage: InterfaceLanguage,
) : ViewModel() {
    val today: StateFlow<TodayUi?> = combine(settings.settings, games.records()) { settings, records ->
        val calendar = DailyCalendar(clock.get())
        val date = calendar.today()
        TodayUi(
            settings.gameLanguage,
            date,
            calendar.number(date),
            Dailies.status(records, settings.gameLanguage, date),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The id of today's daily in the game language, started now if it was not. */
    suspend fun openToday(): Long {
        val current = settings.current()
        return games.dailyGame(
            current.gameLanguage,
            DailyCalendar(clock.get()).today(),
            interfaceLanguage.current(),
            current.hard,
        )
    }
}
