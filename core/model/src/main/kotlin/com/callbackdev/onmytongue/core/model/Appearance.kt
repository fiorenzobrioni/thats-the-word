package com.callbackdev.onmytongue.core.model

/** Light, dark, or whatever the phone says: the player's choice, the phone's by default. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Which of the family's two dresses the app wears when it is not wearing the wallpaper's:
 * [PAPER], warm white and amber; [VIVID], cool white and azure, the default. The feedback's
 * colours are the game's own and never change with the dress (PLANNING.md §7).
 */
enum class AppPalette {
    PAPER,
    VIVID,
}

/**
 * The typeface: two bundled faces, the same drawing on every phone, and the phone's own sans as
 * the third answer. The tiles' letters follow it too.
 */
enum class AppFont {
    GOOGLE_SANS,
    INTER,
    SYSTEM,
}
