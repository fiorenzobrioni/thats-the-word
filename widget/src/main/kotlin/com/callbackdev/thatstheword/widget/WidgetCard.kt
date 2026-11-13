package com.callbackdev.thatstheword.widget

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.text.TextPaint
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
import androidx.annotation.StyleRes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callbackdev.thatstheword.core.designsystem.theme.CARD_TILE_RIM_ALPHA
import com.callbackdev.thatstheword.core.designsystem.theme.WidgetDress
import com.callbackdev.thatstheword.core.designsystem.theme.widgetCardContainer
import com.callbackdev.thatstheword.core.designsystem.theme.widgetDress
import com.callbackdev.thatstheword.core.domain.widget.WidgetType
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.WidgetGround
import com.callbackdev.thatstheword.core.model.WidgetLook
import java.util.Locale

/*
 * The card's side of the design system (`docs/adr/0004-home-screen-card.md`): the family's card,
 * its launcher's corner, insets, grounds, inks and type scale; the system face, because a launcher
 * draws `RemoteViews` in it whatever the game's own setting is.
 */

/** The dress a card writes in: the player's, or the wallpaper's schemes when they asked. */
fun widgetDressFor(context: Context, palette: AppPalette, wallpaperColors: Boolean): WidgetDress =
    if (wallpaperColors) {
        widgetDress(palette, dynamicLightColorScheme(context), dynamicDarkColorScheme(context))
    } else {
        widgetDress(palette)
    }

/**
 * The inks and marks a card draws with, resolved once for the ground it really has.
 *
 * @property primary a language's name, the eyebrow's words; [secondary] the quieter lines.
 * @property frame an empty tile's outline: the ink, thinned, so it reads on any ground.
 * @property sunk a word missed's tiles: the ink, faint, sunk into whatever ground is under it.
 * @property rim the light edge round a word found's tiles where the ground is dark: the right
 *   place's ink blue is as dark as the cards' colours; null on a light ground.
 */
data class CardPalette(
    val primary: Color,
    val secondary: Color,
    val frame: Color,
    val sunk: Color,
    val rim: Color?,
    val darkGround: Boolean,
) {
    val primaryInk: ColorProvider get() = ColorProvider(primary)
    val secondaryInk: ColorProvider get() = ColorProvider(secondary)
}

/** [widgetInk]'s answer, dressed in the colours it names. */
fun cardPalette(context: Context, look: WidgetLook, dress: WidgetDress): CardPalette {
    val night = isNight(context)
    val delegates = look.ground == WidgetGround.SYSTEM || look.ground == WidgetGround.COLOR
    val bright = look.opacityPct < INK_TRUST_FLOOR_PCT && delegates && wallpaperWantsDarkInk(context)
    return cardPaletteFor(widgetInk(look.ground, look.opacityPct, night, bright), dress)
}

/** The palette for an [ink] already decided: pure, so a test pins every ground's inks. */
fun cardPaletteFor(ink: WidgetInk, dress: WidgetDress): CardPalette {
    val (primary, secondary) = when (ink) {
        WidgetInk.OVER_COLOR -> Color.White to Color.White.copy(alpha = QUIET_ALPHA)

        WidgetInk.ON_LIGHT, WidgetInk.ON_DARK -> dress.scheme(ink.darkGround).let {
            it.onSurface to it.onSurfaceVariant
        }
    }
    return CardPalette(
        primary = primary,
        secondary = secondary,
        frame = primary.copy(alpha = FRAME_ALPHA),
        sunk = primary.copy(alpha = SUNK_ALPHA),
        rim = if (ink.darkGround) Color.White.copy(alpha = CARD_TILE_RIM_ALPHA) else null,
        darkGround = ink.darkGround,
    )
}

/** The card's ground at full strength: what [widgetCardFill] thins by the player's opacity. */
fun cardGround(look: WidgetLook, dress: WidgetDress, night: Boolean): Color = when (look.ground) {
    WidgetGround.LIGHT -> dress.lightScheme.surface
    WidgetGround.DARK -> dress.darkScheme.surface
    WidgetGround.SYSTEM -> dress.scheme(night).surface
    WidgetGround.COLOR -> widgetCardContainer(look.cardColor)
}

/** The card's fill at the player's opacity: the ground thins, the ink never does. */
fun widgetCardFill(look: WidgetLook, dress: WidgetDress, night: Boolean): Color =
    cardGround(look, dress, night).copy(alpha = look.opacityPct.coerceIn(0, 100) / 100f)

/**
 * The phone's night mode at render time. Glance's day/night providers are resolved by the
 * launcher, and a host that flips the card without the words leaves dark ink on a dark card:
 * every colour here is resolved against one answer.
 */
fun isNight(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * Whether the wallpaper behind a see-through card can carry dark text, by the system's own
 * account ([WallpaperColors.HINT_SUPPORTS_DARK_TEXT]). Read as the affirmative signal it is:
 * dark ink only when the system says the ground is bright, light ink whenever it says nothing.
 */
fun wallpaperWantsDarkInk(context: Context): Boolean {
    val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull() ?: return false
    val colors = runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        ?: runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_LOCK) }.getOrNull()
        ?: return false
    return colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
}

/**
 * The card every form lives in: the launcher's corner, the ground at the player's opacity, the
 * insets for what sits against each edge, and one door into the game for the whole card (a
 * language's row, inside it, opens that language's word instead).
 */
@Composable
internal fun WidgetCard(
    model: WidgetModel,
    paddingHorizontal: Dp,
    paddingVertical: Dp,
    content: @Composable (CardPalette) -> Unit,
) {
    val context = LocalContext.current
    val dress = remember(model.palette, model.wallpaperColors) {
        widgetDressFor(context, model.palette, model.wallpaperColors)
    }
    val palette = remember(model.look, dress) { cardPalette(context, model.look, dress) }
    val tap = WidgetIntents.openApp(context)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(WidgetCornerRes)
            .then(if (tap != null) GlanceModifier.clickable(actionStartActivity(tap)) else GlanceModifier),
    ) {
        // The ground in a box of its own, under the content: the corner clips it on every launcher.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(widgetCardFill(model.look, dress, isNight(context)))),
        ) {}
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        ) {
            content(palette)
        }
    }
}

/**
 * The model this card draws, read again inside the composition whenever [WidgetRefresh] ticks.
 * [initial] is what `provideGlance` loaded at revision [loadedAt], so the first frame costs
 * nothing more and only a real change reloads. Each frame reports what it drew, so whatever asked
 * for the redraw can stay until it is drawn ([WidgetRefresh.awaitDrawn]).
 */
@Composable
internal fun rememberWidgetModel(
    appWidgetId: Int,
    initial: WidgetModel,
    loadedAt: Long,
    reload: suspend () -> WidgetModel,
): WidgetModel {
    val revision by WidgetRefresh.revision.collectAsState()
    val model by produceState(initial, revision) {
        val read = if (revision != loadedAt) reload() else initial
        value = read
        WidgetRefresh.drawn(appWidgetId, revision)
    }
    return model
}

/** The card that could not read the games: one message, the whole card opening the game. */
@Composable
internal fun MessageContent(title: String, hint: String?, palette: CardPalette) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = palette.primaryInk,
                fontSize = WidgetType.LABEL.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 3,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = TextStyle(color = palette.secondaryInk, fontSize = WidgetType.CAPTION.sp),
                maxLines = 2,
            )
        }
    }
}

/** The locale a card formats in, off the context Glance composes with. */
internal fun Context.widgetLocale(): Locale = resources.configuration.locales[0]

/**
 * How much larger than its sp the player's text size sets a card's words, for the arithmetic:
 * Android 14 scales text non-linearly (small sizes more than large ones), so the scale is read off
 * the platform's own conversion at each of the card's steps, and the largest is taken: a line
 * budgeted at it is never set wider.
 */
internal fun fontScale(context: Context): Float {
    val metrics = context.resources.displayMetrics
    return WidgetType.steps.maxOf { sp ->
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics) / metrics.density / sp
    }
}

/**
 * The three weights a card sets text in, each with the appearance Glance sets it in
 * (`text_faces.xml`): its weight, in the theme's device-default family.
 */
internal enum class TextWeight(@StyleRes val appearance: Int) {
    REGULAR(R.style.WidgetTextFace_Regular),
    MEDIUM(R.style.WidgetTextFace_Medium),
    BOLD(R.style.WidgetTextFace_Bold),
}

/**
 * A paint in the face the launcher will draw [weight] in, at [sizePx]: the very span Glance puts
 * on a weighted `Text`, applied here. Measured in plain sans-serif instead, a maker's own face can
 * come out a tenth wider than budgeted, and a word is cut.
 */
internal fun widgetPaint(context: Context, weight: TextWeight, sizePx: Float): TextPaint =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        TextAppearanceSpan(context, weight.appearance).updateMeasureState(this)
        textSize = sizePx
    }

/**
 * The width of [text] in ems of the system face at [weight]: independent of the density and of
 * the player's font scale, which the arithmetic applies itself; with the margin a launcher's face
 * may want more of.
 */
internal fun textEm(context: Context, text: String, weight: TextWeight): Float =
    widgetPaint(context, weight, EM_PROBE_PX).measureText(text) * FACE_MARGIN / EM_PROBE_PX

/**
 * How much wider than measured a card budgets every line: a launcher may set a line a little wider
 * (its hinting, a face substituted for a letter), and a word cut by a few dp is a word lost.
 */
internal const val FACE_MARGIN = 1.06f
private const val EM_PROBE_PX = 100f

/**
 * The card's corner: the launcher's own. The platform names the radius the home screen gives its
 * widgets, and every card of the family takes it.
 */
internal val WidgetCornerRes = android.R.dimen.system_app_widget_background_radius

private const val QUIET_ALPHA = 0.75f
private const val FRAME_ALPHA = 0.42f
private const val SUNK_ALPHA = 0.16f
