package com.callbackdev.thatstheword.widget.config

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.designsystem.components.Eyebrow
import com.callbackdev.thatstheword.core.designsystem.components.PageTopBar
import com.callbackdev.thatstheword.core.designsystem.components.inkButtonColors
import com.callbackdev.thatstheword.core.designsystem.icons.GameIcons
import com.callbackdev.thatstheword.core.designsystem.theme.GroupShape
import com.callbackdev.thatstheword.core.designsystem.theme.ScreenMargin
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.widgetCardContainer
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.WidgetCardColor
import com.callbackdev.thatstheword.core.model.WidgetGround
import com.callbackdev.thatstheword.core.model.WidgetLook
import com.callbackdev.thatstheword.widget.R
import com.callbackdev.thatstheword.widget.WidgetModel
import com.callbackdev.thatstheword.widget.isNight
import com.callbackdev.thatstheword.widget.widgetCardFill
import com.callbackdev.thatstheword.widget.widgetDressFor
import kotlin.math.roundToInt

/** Tags for the tests. */
object WidgetConfigTags {
    const val PAGE = "widget_config_page"
    const val SIZES = "widget_config_sizes"
    const val OPACITY = "widget_config_opacity"
    const val RESET = "widget_config_reset"
    const val DONE = "widget_config_done"

    fun ground(ground: WidgetGround) = "widget_config_ground_${ground.name.lowercase()}"

    fun color(color: WidgetCardColor) = "widget_config_color_${color.name.lowercase()}"
}

/**
 * One card's settings (PLANNING.md §7.8), reached when it is placed and from the launcher's long
 * press: the card at the top, at the sizes it can be given, then what it stands on (light, dark,
 * as the phone, one of six colours) and how solid it is. Every choice is saved as it is made and
 * redraws this one card; «Reset this card» puts it back as it was placed, «Done» closes the page.
 *
 * [preview] is the card's own day, or the sample day before the first game.
 */
@Composable
fun WidgetConfigScreen(
    look: WidgetLook,
    preview: WidgetModel?,
    placed: DpSize?,
    onLook: (WidgetLook) -> Unit,
    onOpacityDrag: (Int) -> Unit,
    onOpacityDone: () -> Unit,
    onDone: () -> Unit,
    palette: AppPalette = AppPalette.VIVID,
    wallpaperColors: Boolean = false,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))) {
            PageTopBar(
                title = stringResource(R.string.widget_config_title),
                backLabel = stringResource(R.string.widget_config_back),
                onBack = onDone,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(WidgetConfigTags.PAGE)
                    .verticalScroll(rememberScrollState())
                    .padding(pageGutter())
                    .padding(horizontal = ScreenMargin)
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PreviewSection(preview?.copy(look = look), placed)
                Section(stringResource(R.string.widget_config_background)) {
                    GroundChoices(look, palette, wallpaperColors, onLook)
                    HorizontalDivider(
                        Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    OpacityRow(look.opacityPct, onOpacityDrag, onOpacityDone)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onDone,
                        colors = inkButtonColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag(WidgetConfigTags.DONE),
                    ) {
                        Text(stringResource(R.string.widget_config_done), style = MaterialTheme.typography.titleMedium)
                    }
                    // The one control that undoes the others, quiet, at the foot.
                    OutlinedButton(
                        onClick = { onLook(WidgetLook()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag(WidgetConfigTags.RESET),
                    ) {
                        Text(stringResource(R.string.widget_config_reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Eyebrow(
            title,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(start = 20.dp, bottom = 8.dp)
                .semantics { heading() },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GroupShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) { content() }
    }
}

/** What the ground question offers, in order. Every [WidgetGround] has its row. */
internal val WidgetGroundChoices: List<Pair<WidgetGround, Int>> = listOf(
    WidgetGround.LIGHT to R.string.widget_bg_light,
    WidgetGround.DARK to R.string.widget_bg_dark,
    WidgetGround.SYSTEM to R.string.widget_bg_system,
    WidgetGround.COLOR to R.string.widget_bg_color,
)

/** The six, in their order. Every [WidgetCardColor] has its swatch. */
internal val WidgetCardColorChoices: List<Pair<WidgetCardColor, Int>> = listOf(
    WidgetCardColor.BLUE to R.string.widget_color_blue,
    WidgetCardColor.AZURE to R.string.widget_color_azure,
    WidgetCardColor.GREEN to R.string.widget_color_green,
    WidgetCardColor.TEAL to R.string.widget_color_teal,
    WidgetCardColor.PLUM to R.string.widget_color_plum,
    WidgetCardColor.CLAY to R.string.widget_color_clay,
)

/**
 * What kind of card, each row with a swatch of the ground it paints; and, once «A colour» is
 * picked, which colour, as a strip of swatches with the chosen one's name under it (a swatch is
 * never the only label).
 */
@Composable
private fun GroundChoices(
    look: WidgetLook,
    palette: AppPalette,
    wallpaperColors: Boolean,
    onLook: (WidgetLook) -> Unit,
) {
    val context = LocalContext.current
    val dress = widgetDressFor(context, palette, wallpaperColors)
    val night = isNight(context)
    Column(Modifier.selectableGroup().padding(vertical = 6.dp)) {
        WidgetGroundChoices.forEach { (ground, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(selected = look.ground == ground, onClick = {
                        onLook(look.copy(ground = ground))
                    }, role = Role.RadioButton)
                    .testTag(WidgetConfigTags.ground(ground))
                    .padding(start = 8.dp, end = 20.dp),
            ) {
                RadioButton(
                    selected = look.ground == ground,
                    onClick = null,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                val swatch = Modifier
                    .size(width = 44.dp, height = 30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                val full = look.copy(ground = ground, opacityPct = 100)
                if (ground == WidgetGround.SYSTEM) {
                    // The phone's choice, drawn as both of its answers on a diagonal.
                    Canvas(swatch) {
                        drawRect(dress.lightScheme.surface)
                        drawPath(
                            Path().apply {
                                moveTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            },
                            dress.darkScheme.surface,
                        )
                    }
                } else {
                    Box(swatch.background(widgetCardFill(full, dress, night)))
                }
            }
        }
    }
    if (look.ground == WidgetGround.COLOR) {
        ColorSwatches(look.cardColor) { onLook(look.copy(cardColor = it)) }
    }
}

@Composable
private fun ColorSwatches(selected: WidgetCardColor, onPick: (WidgetCardColor) -> Unit) {
    Column(modifier = Modifier.padding(start = 52.dp, end = 12.dp, bottom = 14.dp)) {
        // Each colour is a 48dp target around its 36dp disc, side by side: the gaps between the
        // discs are the targets' own. A narrow phone wraps the row.
        FlowRow(modifier = Modifier.selectableGroup()) {
            WidgetCardColorChoices.forEach { (color, label) ->
                val chosen = color == selected
                val name = stringResource(label)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .selectable(selected = chosen, onClick = { onPick(color) }, role = Role.RadioButton)
                        .testTag(WidgetConfigTags.color(color))
                        .semantics { contentDescription = name },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (chosen) 2.dp else 0.dp,
                                color = if (chosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape,
                            )
                            .padding(if (chosen) 4.dp else 0.dp)
                            .clip(CircleShape)
                            .background(widgetCardContainer(color)),
                    ) {
                        if (chosen) {
                            // Every card colour is a dark ground under white ink.
                            Icon(
                                GameIcons.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(WidgetCardColorChoices.first { it.first == selected }.second),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp),
        )
    }
}

/** How solid the card is: the name and the value on one line, the slider under them, in steps of 5%. */
@Composable
private fun OpacityRow(pct: Int, onChange: (Int) -> Unit, onDone: () -> Unit) {
    val value = when (pct) {
        100 -> stringResource(R.string.widget_opacity_full)
        0 -> stringResource(R.string.widget_opacity_transparent)
        else -> stringResource(R.string.widget_opacity_percent, pct)
    }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.widget_config_opacity),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = pct.toFloat(),
            onValueChange = { raw -> onChange((raw / 5f).roundToInt() * 5) },
            onValueChangeFinished = onDone,
            valueRange = 0f..100f,
            steps = 19,
            modifier = Modifier
                .testTag(WidgetConfigTags.OPACITY)
                .semantics { stateDescription = value },
        )
        Text(
            text = stringResource(R.string.widget_config_opacity_note),
            style = MaterialTheme.typography.bodySmall.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
