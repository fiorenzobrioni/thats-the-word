package com.callbackdev.thatstheword.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.thatstheword.core.data.HomeScreenCards
import com.callbackdev.thatstheword.core.data.InterfaceLanguage
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.callbackdev.thatstheword.core.model.UserSettings
import com.callbackdev.thatstheword.core.reminder.DailyReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * Settings, as the page draws it: what the player chose, and the language the interface speaks
 * now (the system's per-app choice, which the page names and opens, never keeps).
 */
@Immutable
data class SettingsUiState(val settings: UserSettings, val interfaceLanguage: Language) {
    val level: Level get() = settings.levelOf(settings.gameLanguage)
}

/**
 * The settings page's model (PLANNING.md §11 Phase 7): every choice written at once to the
 * settings file, which the whole app reads; the reminder's alarm follows its own setting by
 * itself (`DailyReminder.start`), and is told here only when the notifications may have changed.
 *
 * Its state is collected only while the page is (`WhileSubscribed`): nothing runs for a page that
 * is not seen (PLANNING.md §9).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val interfaceLanguage: InterfaceLanguage,
    private val reminder: DailyReminder,
    private val cards: HomeScreenCards,
) : ViewModel() {
    val state: StateFlow<SettingsUiState?> = settings.settings
        .map { SettingsUiState(it, interfaceLanguage.current()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), null)

    private fun write(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settings.block() }
    }

    fun setGameLanguage(language: Language) = write { setGameLanguage(language) }

    /** The level of the game language: each language keeps its own (PLANNING.md §4.7). */
    fun setLevel(level: Level) = write { setLevel(current().gameLanguage, level) }

    fun setHard(hard: Boolean) = write { setHard(hard) }

    fun setSound(on: Boolean) = write { setSound(on) }

    fun setHaptics(on: Boolean) = write { setHaptics(on) }

    fun setReducedMotion(reduced: Boolean?) = write { setReducedMotion(reduced) }

    fun setTheme(mode: ThemeMode) = write { setThemeMode(mode) }

    fun setPalette(palette: AppPalette) = write { setPalette(palette) }

    fun setFont(font: AppFont) = write { setFont(font) }

    fun setWallpaperColors(on: Boolean) = write { setWallpaperColors(on) }

    fun setReminder(on: Boolean) = write { setReminder(on) }

    fun setReminderTime(time: LocalTime) = write { setReminderTime(time) }

    fun setAskedNotifications() = write { setAskedNotifications() }

    /** Where the home-screen card stands now: read again whenever the page comes back. */
    fun homeCard(): HomeCard = when {
        cards.placed() -> HomeCard.PLACED
        cards.canPin() -> HomeCard.ADD
        else -> HomeCard.BY_HAND
    }

    /** The launcher's own question, "Add to the home screen?": it places the card on a yes. */
    fun addCard() {
        cards.pin()
    }

    /** The permission answered, or the system's page left: the alarm follows what can show now. */
    fun accessMayHaveChanged() {
        viewModelScope.launch { reminder.accessMayHaveChanged() }
    }

    private companion object {
        /** A turn of the phone keeps the state; a page left for good lets it go. */
        const val STOP_AFTER_MILLIS = 5_000L
    }
}
