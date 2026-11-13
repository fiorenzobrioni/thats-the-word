package com.callbackdev.thatstheword.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.thatstheword.core.data.DayTicker
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.InterfaceLanguage
import com.callbackdev.thatstheword.core.data.NotebookRepository
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Provider

/**
 * The start page (PLANNING.md §11 Phase 4): today's word in each language, the game language
 * changed in one touch, the streak, the calendar of the dailies, free play and Hard; and the games
 * they open, started or taken up; and the notebook, once it holds a word.
 *
 * The change of day is seen live by [DayTicker], collected only while the page is on screen
 * (`WhileSubscribed`): nothing runs while it is not (PLANNING.md §9). The calendar's month is kept
 * in [SavedStateHandle], so a page the system took away comes back on it.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val games: GameRepository,
    private val interfaceLanguage: InterfaceLanguage,
    private val clock: Provider<Clock>,
    private val savedState: SavedStateHandle,
    ticker: DayTicker,
    notebook: NotebookRepository,
) : ViewModel() {
    private val month = savedState.getStateFlow<String?>(MONTH, null)

    val state: StateFlow<HomeUiState?> =
        combine(
            settings.settings,
            games.records(),
            ticker.dates,
            month,
            notebook.size(),
        ) { settings, records, _, month, notebookSize ->
            HomeUiState.of(
                settings = settings,
                records = records,
                calendar = DailyCalendar(clock.get()),
                month = month?.let(YearMonth::parse),
                scheduleLength = games.scheduleLength(settings.gameLanguage),
                notebookSize = notebookSize,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), null)

    private var opening: Job? = null

    fun selectLanguage(language: Language) {
        viewModelScope.launch { settings.setGameLanguage(language) }
    }

    fun setHard(hard: Boolean) {
        viewModelScope.launch { settings.setHard(hard) }
    }

    fun setLevel(level: Level) {
        viewModelScope.launch { settings.setLevel(settings.current().gameLanguage, level) }
    }

    fun showMonth(month: YearMonth) {
        savedState[MONTH] = month.toString()
    }

    /** The game language's daily of [date], started or taken up: today's, or one from the archive. */
    fun openDaily(date: LocalDate, onOpened: (Long) -> Unit) = open(onOpened) {
        val current = settings.current()
        games.dailyGame(current.gameLanguage, date, interfaceLanguage.current(), current.hard)
    }

    /** The game language's free game under way, or a new word at its level. */
    fun openFree(onOpened: (Long) -> Unit) = open(onOpened) {
        val current = settings.current()
        games.freeGame(
            current.gameLanguage,
            current.levelOf(current.gameLanguage),
            interfaceLanguage.current(),
            current.hard,
        )
    }

    /** One game opened at a time: a second touch while the first is opening opens nothing more. */
    private fun open(onOpened: (Long) -> Unit, game: suspend () -> Long) {
        if (opening?.isActive == true) return
        opening = viewModelScope.launch { onOpened(game()) }
    }

    private companion object {
        const val MONTH = "month"

        /** A rotation keeps the state; a page left for longer lets its flows go. */
        const val STOP_AFTER_MILLIS = 5_000L
    }
}
