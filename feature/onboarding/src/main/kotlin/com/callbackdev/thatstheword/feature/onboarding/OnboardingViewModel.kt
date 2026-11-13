package com.callbackdev.thatstheword.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.InterfaceLanguage
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The first run's model (PLANNING.md §11 Phase 7): the interface's language, which the run offers
 * first, and the end of the run, which writes the player's choices, marks the first run done and
 * starts the guided game in the chosen language, unless the player knows the game already.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val games: GameRepository,
    private val interfaceLanguage: InterfaceLanguage,
) : ViewModel() {
    /** The language the interface speaks: the phone's, or the one picked for the app. */
    val speaks: Language get() = interfaceLanguage.current()

    private val finishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = finishing.asStateFlow()

    /**
     * The run's end: [language] becomes the game language, [level] its level (a foreign language's
     * only: the player's own starts at Base, as every language does), the first run is done; then
     * [onDone] hears the guided game's id, or null when the player chose to skip it ([guided]).
     */
    fun finish(language: Language, level: Level?, guided: Boolean, onDone: (Long?) -> Unit) {
        if (finishing.value) return
        finishing.value = true
        viewModelScope.launch {
            settings.setGameLanguage(language)
            if (level != null) settings.setLevel(language, level)
            val id = if (guided) games.guidedGame(language, interfaceLanguage.current()) else null
            settings.setFirstRunDone()
            onDone(id)
        }
    }
}
