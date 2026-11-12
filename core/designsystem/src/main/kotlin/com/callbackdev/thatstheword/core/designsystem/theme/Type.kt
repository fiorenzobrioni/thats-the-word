package com.callbackdev.thatstheword.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.callbackdev.thatstheword.core.designsystem.R
import com.callbackdev.thatstheword.core.model.AppFont

/*
 * The type (`docs/adr/0002-design-language.md`): two faces and one scale. Both bundled faces are
 * variable fonts under the SIL Open Font License 1.1 (`licenses/`), Google Sans cut down to the
 * scripts the app prints (Italian's accents and Spanish's ñ, ¿ and ¡ among them). Bundled, not
 * fetched from a font provider: that would be a runtime dependency on Play Services, and the app
 * has no network anyway.
 */

// The variationSettings overload is still experimental. Without it Android would synthesise
// the weights by smearing one outline, which is what a variable font is bundled to avoid.
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Google Sans: its weight axis starts at 400, so a Light request lands on Regular. */
private val GoogleSansFamily = FontFamily(
    variable(R.font.google_sans_variable, 400),
    variable(R.font.google_sans_variable, 500),
    variable(R.font.google_sans_variable, 600),
    variable(R.font.google_sans_variable, 700),
)

private val InterFamily = FontFamily(
    variable(R.font.inter_variable, 300),
    variable(R.font.inter_variable, 400),
    variable(R.font.inter_variable, 500),
    variable(R.font.inter_variable, 600),
    variable(R.font.inter_variable, 700),
)

/** The phone's own sans: a different font on every device, with whatever weights it has. */
private val SystemFamily = FontFamily.Default

internal fun familyFor(font: AppFont): FontFamily = when (font) {
    AppFont.GOOGLE_SANS -> GoogleSansFamily
    AppFont.INTER -> InterFamily
    AppFont.SYSTEM -> SystemFamily
}

/** Figures in a column, or updating live, are tabular: proportional digits make a number wobble. */
private const val TABULAR = "tnum"

/**
 * Material's scale in [family], with four adjustments, and the way each role breaks its lines: a
 * heading balanced, so it never leaves one word alone on its last line; a body by the paragraph,
 * the best breaks a long text can have. Every role is named, so no line of the app falls back to
 * the platform face by accident (`TypographyTest` counts them).
 */
internal fun typographyFor(family: FontFamily): Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.heading(family),
        displayMedium = displayMedium.heading(family),
        displaySmall = displaySmall.heading(family).copy(fontSize = 36.sp, lineHeight = 44.sp),
        headlineLarge = headlineLarge.heading(family),
        headlineMedium = headlineMedium.heading(family),
        headlineSmall = headlineSmall.heading(family),
        titleLarge = titleLarge.heading(family).copy(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        ),
        titleMedium = titleMedium.heading(family).copy(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleSmall = titleSmall.heading(family),
        bodyLarge = bodyLarge.copy(fontFamily = family, lineBreak = LineBreak.Paragraph),
        bodyMedium = bodyMedium.copy(fontFamily = family, lineBreak = LineBreak.Paragraph),
        bodySmall = bodySmall.copy(fontFamily = family, lineBreak = LineBreak.Paragraph),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
}

private fun TextStyle.heading(family: FontFamily): TextStyle = copy(fontFamily = family, lineBreak = LineBreak.Heading)

/**
 * The roles Material does not have, in the player's family. A game speaks in capitals where the
 * family's apps never do: the name, the line above a title, the letters on the tiles; the
 * capitals are the caller's (`uppercase(locale)`), the tracking is here.
 *
 * @property wordmark the app's name where a page shows it (the start page): bold, widely tracked
 *   (+0.12em), 30sp on a 36sp line: fifteen capitals with room on a phone's 411dp, balanced on two
 *   lines where they do not fit.
 * @property eyebrow a short line in capitals above or below a title ("A DAILY WORD GAME IN THREE
 *   TONGUES"): medium, tracked +0.16em, 12sp on a 16sp line.
 * @property tileLetter a letter on a tile: bold; its size is the tile's to set (half its side), not
 *   the text size's, so a row of tiles keeps its drawing at any text size.
 * @property heroNumber the one number a page is for (a streak, a score): bold, tracked in, tabular
 *   so the figures do not wobble as they change (64sp on a 68sp line, −0.02em).
 */
@Immutable
data class ThatsTheWordType(
    val wordmark: TextStyle,
    val eyebrow: TextStyle,
    val tileLetter: TextStyle,
    val heroNumber: TextStyle,
)

private fun typeFor(family: FontFamily) = ThatsTheWordType(
    wordmark = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.12.em,
        lineBreak = LineBreak.Heading,
    ),
    eyebrow = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.16.em,
        lineBreak = LineBreak.Heading,
    ),
    tileLetter = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 32.sp,
    ),
    heroNumber = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = TABULAR,
    ),
)

private val typographies = AppFont.entries.associateWith { typographyFor(familyFor(it)) }
private val types = AppFont.entries.associateWith { typeFor(familyFor(it)) }

/** Built once per face and picked: the player changes this about once. */
internal fun thatsTheWordTypography(font: AppFont): Typography = typographies.getValue(font)

internal fun thatsTheWordType(font: AppFont): ThatsTheWordType = types.getValue(font)

val LocalThatsTheWordType = staticCompositionLocalOf { typeFor(GoogleSansFamily) }

/** Any style, with tabular figures. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR)

/**
 * Any style, its lines balanced: a few lines of about one width, never a word alone on the last.
 * For a short text that stands on its own (a centred paragraph, a row's note, a card's words); a
 * long one reads better by the paragraph, as the body roles break already.
 */
fun TextStyle.balanced(): TextStyle = copy(lineBreak = LineBreak.Heading)
