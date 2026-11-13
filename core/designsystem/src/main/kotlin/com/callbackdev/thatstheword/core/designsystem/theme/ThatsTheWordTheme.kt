package com.callbackdev.thatstheword.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.callbackdev.thatstheword.core.model.AppFont
import com.callbackdev.thatstheword.core.model.AppPalette

/**
 * The game's theme: the family's design language (`docs/adr/0002-design-language.md`). The
 * defaults are the vivid dress and Google Sans. [dynamicColor] takes Material's roles from the
 * wallpaper instead. The feedback's colours (`FeedbackColors`, PLANNING.md §7.2) are the
 * game's own and follow neither the dress nor the wallpaper: a letter in the right place looks
 * the same on every phone.
 *
 * [reducedMotion] is the player's in-app choice (VISION §12): true or false overrides the phone's
 * setting, null follows it. The phone's own answer stays readable ([systemReducedMotion]), so
 * Settings can say what "as the phone" means.
 */
@Composable
fun ThatsTheWordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    palette: AppPalette = AppPalette.VIVID,
    font: AppFont = AppFont.GOOGLE_SANS,
    reducedMotion: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        else -> schemeFor(palette, darkTheme)
    }
    val system = rememberReducedMotion(context)
    CompositionLocalProvider(
        LocalThatsTheWordType provides thatsTheWordType(font),
        LocalSystemReducedMotion provides system,
        LocalReducedMotion provides (reducedMotion ?: system),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = thatsTheWordTypography(font),
            shapes = ThatsTheWordShapes,
            content = content,
        )
    }
}

/** The family's two dresses: Paper, warm paper and amber; Vivid, daylight white and azure. */
internal fun schemeFor(palette: AppPalette, dark: Boolean): ColorScheme = when (palette) {
    AppPalette.PAPER -> if (dark) PaperDarkScheme else PaperLightScheme
    AppPalette.VIVID -> if (dark) VividDarkScheme else VividLightScheme
}

/** The dress's colours, for a page that previews them (Settings, Appearance): never for drawing a page. */
fun dressScheme(palette: AppPalette, dark: Boolean): ColorScheme = schemeFor(palette, dark)

/** The design system's own values, next to [MaterialTheme]'s. */
object ThatsTheWordTheme {
    val type: ThatsTheWordType
        @Composable @ReadOnlyComposable
        get() = LocalThatsTheWordType.current
}
