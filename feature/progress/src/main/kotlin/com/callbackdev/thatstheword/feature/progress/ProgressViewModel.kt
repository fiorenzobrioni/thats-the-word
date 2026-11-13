package com.callbackdev.thatstheword.feature.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.NotebookRepository
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.progress.ProgressMode
import com.callbackdev.thatstheword.core.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider

/**
 * The progress page (VISION §11, PLANNING.md §11 Phase 6): every number computed from the games
 * as they are now, never kept (PLANNING.md §4.8), off the main thread. The language and the mode
 * looked at live in [SavedStateHandle], so a page the system took away comes back as it was.
 * Collected while the page is seen (`WhileSubscribed`), nothing runs after.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    games: GameRepository,
    notebook: NotebookRepository,
    settings: SettingsRepository,
    private val clock: Provider<Clock>,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val language = savedState.getStateFlow<String?>(LANGUAGE, null)
    private val mode = savedState.getStateFlow<String?>(MODE, null)
    private val gameLanguage = settings.settings.map { it.gameLanguage }.distinctUntilChanged()

    val state: StateFlow<ProgressUiState?> = combine(
        games.records(),
        notebook.size(),
        notebook.milestones(),
        gameLanguage,
        combine(language, mode, ::Pair),
    ) { records, size, unlocked, gameLanguage, (language, mode) ->
        val now = clock.get()
        ProgressUiState.of(
            records = records,
            notebookSize = size,
            unlocked = unlocked.mapValues { (_, at) -> LocalDate.ofInstant(at, now.zone) },
            today = LocalDate.now(now),
            gameLanguage = gameLanguage,
            wanted = language?.let(Language::ofTag),
            wantedMode = mode?.let { name -> ProgressMode.entries.firstOrNull { it.name == name } },
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), null)

    fun showLanguage(language: Language) {
        savedState[LANGUAGE] = language.tag
    }

    fun showMode(mode: ProgressMode) {
        savedState[MODE] = mode.name
    }

    private companion object {
        const val LANGUAGE = "language"
        const val MODE = "mode"

        /** A rotation keeps the state; a page left for longer lets its flows go. */
        const val STOP_AFTER_MILLIS = 5_000L
    }
}
