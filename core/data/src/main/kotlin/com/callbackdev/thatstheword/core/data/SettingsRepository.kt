package com.callbackdev.thatstheword.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.thatstheword.core.domain.reminder.ReminderSchedule
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.callbackdev.thatstheword.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject

/**
 * What the player chose (PLANNING.md §5), in the `settings` file. What was never chosen reads as
 * its default, and the defaults are written here only:
 *
 * - the game language: the interface's, until the player chooses another (the first run, Phase 7);
 * - every level Base (PLANNING.md §4.7); Hard off; motion as the system says;
 * - sound and haptics on, the phone's silent mode and touch-feedback setting still followed (the
 *   owner, PLANNING §15, Open 13);
 * - the look the theme has worn since Phase 0: the phone's light or dark, the Vivid dress, Google
 *   Sans, the dress's own colours (Settings offers the family's choices: the owner, Open 14);
 * - the daily reminder off, at eight in the evening once it is on (PLANNING.md §8).
 */
class SettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
    private val interfaceLanguage: InterfaceLanguage,
) {
    val settings: Flow<UserSettings> = store.data.map(::settingsOf).distinctUntilChanged()

    suspend fun current(): UserSettings = settings.first()

    // The setters return nothing: the file's own types stay inside this module.

    private fun settingsOf(p: Preferences) = UserSettings(
        gameLanguage = p[GAME_LANGUAGE]?.let(Language::ofTag) ?: interfaceLanguage.current(),
        levels = Language.entries.mapNotNull { language ->
            p[levelKey(language)]?.let { code -> Level.entries.firstOrNull { it.code == code } }?.let { language to it }
        }.toMap(),
        hard = p[HARD] ?: false,
        sound = p[SOUND] ?: true,
        haptics = p[HAPTICS] ?: true,
        reducedMotion = p[REDUCED_MOTION],
        themeMode = p[THEME].enumOr(ThemeMode.SYSTEM),
        palette = p[PALETTE].enumOr(AppPalette.VIVID),
        font = p[FONT].enumOr(AppFont.GOOGLE_SANS),
        wallpaperColors = p[WALLPAPER_COLORS] ?: false,
        firstRunDone = p[FIRST_RUN_DONE] ?: false,
        tipsSeen = p[TIPS_SEEN] ?: false,
        reminder = p[REMINDER] ?: false,
        reminderTime = p[REMINDER_MINUTE]?.let { LocalTime.ofSecondOfDay(it.coerceIn(0, MINUTES_A_DAY - 1) * 60L) }
            ?: ReminderSchedule.DEFAULT_TIME,
        askedNotifications = p[ASKED_NOTIFICATIONS] ?: false,
    )

    suspend fun setGameLanguage(language: Language) {
        store.edit { it[GAME_LANGUAGE] = language.tag }
    }

    suspend fun setLevel(language: Language, level: Level) {
        store.edit { it[levelKey(language)] = level.code }
    }

    suspend fun setHard(hard: Boolean) {
        store.edit { it[HARD] = hard }
    }

    suspend fun setSound(on: Boolean) {
        store.edit { it[SOUND] = on }
    }

    suspend fun setHaptics(on: Boolean) {
        store.edit { it[HAPTICS] = on }
    }

    /** Null follows the system's setting again. */
    suspend fun setReducedMotion(reduced: Boolean?) {
        store.edit { if (reduced == null) it.remove(REDUCED_MOTION) else it[REDUCED_MOTION] = reduced }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    suspend fun setPalette(palette: AppPalette) {
        store.edit { it[PALETTE] = palette.name }
    }

    suspend fun setFont(font: AppFont) {
        store.edit { it[FONT] = font.name }
    }

    suspend fun setWallpaperColors(on: Boolean) {
        store.edit { it[WALLPAPER_COLORS] = on }
    }

    suspend fun setFirstRunDone() {
        store.edit { it[FIRST_RUN_DONE] = true }
    }

    suspend fun setTipsSeen() {
        store.edit { it[TIPS_SEEN] = true }
    }

    suspend fun setReminder(on: Boolean) {
        store.edit { it[REMINDER] = on }
    }

    /** The reminder's hour, to the minute: an alarm is no finer, and the picker offers no seconds. */
    suspend fun setReminderTime(time: LocalTime) {
        store.edit { it[REMINDER_MINUTE] = time.hour * 60 + time.minute }
    }

    suspend fun setAskedNotifications() {
        store.edit { it[ASKED_NOTIFICATIONS] = true }
    }

    /**
     * The player's choices from a backup (PLANNING.md §5), in one edit: every choice the file holds
     * replaces this phone's, a level the file has none for goes back to Base, and the first run reads
     * as done. Whether the first-day tips were seen, and whether the phone was asked for
     * notifications, are the phone's, not the player's, and stay.
     */
    suspend fun restore(settings: UserSettings) {
        store.edit { p ->
            p[GAME_LANGUAGE] = settings.gameLanguage.tag
            Language.entries.forEach { language ->
                val level = settings.levels[language]
                if (level == null) p.remove(levelKey(language)) else p[levelKey(language)] = level.code
            }
            p[HARD] = settings.hard
            p[SOUND] = settings.sound
            p[HAPTICS] = settings.haptics
            val reduced = settings.reducedMotion
            if (reduced == null) p.remove(REDUCED_MOTION) else p[REDUCED_MOTION] = reduced
            p[THEME] = settings.themeMode.name
            p[PALETTE] = settings.palette.name
            p[FONT] = settings.font.name
            p[WALLPAPER_COLORS] = settings.wallpaperColors
            p[REMINDER] = settings.reminder
            p[REMINDER_MINUTE] = settings.reminderTime.hour * 60 + settings.reminderTime.minute
            p[FIRST_RUN_DONE] = true
        }
    }

    private companion object {
        // The keys are the file's format, read back after an update and from a backup: never renamed.
        val GAME_LANGUAGE = stringPreferencesKey("game_language")
        val HARD = booleanPreferencesKey("hard")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val THEME = stringPreferencesKey("theme")
        val PALETTE = stringPreferencesKey("palette")
        val FONT = stringPreferencesKey("font")
        val WALLPAPER_COLORS = booleanPreferencesKey("wallpaper_colors")
        val FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
        val TIPS_SEEN = booleanPreferencesKey("tips_seen")
        val REMINDER = booleanPreferencesKey("reminder")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val ASKED_NOTIFICATIONS = booleanPreferencesKey("asked_notifications")

        const val MINUTES_A_DAY = 24 * 60

        fun levelKey(language: Language) = intPreferencesKey("level_${language.tag}")

        inline fun <reified E : Enum<E>> String?.enumOr(default: E): E =
            this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
    }
}
