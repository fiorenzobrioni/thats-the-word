package com.callbackdev.thatstheword.core.model

import java.time.LocalTime

/**
 * What the player chose (PLANNING.md §5, kept in DataStore from Phase 3). The interface language is
 * not here: it is the system's per-app language.
 *
 * [levels] holds a level per game language; a language with none is at [Level.BASE] (PLANNING.md
 * §4.7). [reducedMotion] null follows the system's setting. No default is written here: they are
 * `SettingsRepository`'s.
 *
 * [reminder] and [reminderTime] are the daily reminder (PLANNING.md §8): off until the player turns
 * it on, at the player's local time. [askedNotifications] says whether the game ever asked for the
 * notification permission: Android says "no need to explain first" both before the first question
 * and after a refusal it will not let the app repeat, and only this tells the two apart. A fact
 * about this phone, so a backup does not carry it.
 */
data class UserSettings(
    val gameLanguage: Language,
    val levels: Map<Language, Level>,
    val hard: Boolean,
    val sound: Boolean,
    val haptics: Boolean,
    val reducedMotion: Boolean?,
    val themeMode: ThemeMode,
    val palette: AppPalette,
    val font: AppFont,
    val wallpaperColors: Boolean,
    val firstRunDone: Boolean,
    val tipsSeen: Boolean,
    val reminder: Boolean,
    val reminderTime: LocalTime,
    val askedNotifications: Boolean,
) {
    fun levelOf(language: Language): Level = levels[language] ?: Level.BASE
}
