package com.callbackdev.thatstheword.core.model

/**
 * What the player chose (PLANNING.md §5, kept in DataStore from Phase 3). The interface language is
 * not here: it is the system's per-app language.
 *
 * [levels] holds a level per game language; a language with none is at [Level.BASE] (PLANNING.md
 * §4.7). [reducedMotion] null follows the system's setting. No default is written here: sound's,
 * haptics' and the appearance's are PLANNING §15's Open 13 and 14, for the phases that build them.
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
) {
    fun levelOf(language: Language): Level = levels[language] ?: Level.BASE
}
