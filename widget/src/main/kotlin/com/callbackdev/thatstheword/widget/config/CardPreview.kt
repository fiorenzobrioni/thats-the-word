package com.callbackdev.thatstheword.widget.config

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.widget.R
import com.callbackdev.thatstheword.widget.TodayCardContent
import com.callbackdev.thatstheword.widget.WidgetCornerRes
import com.callbackdev.thatstheword.widget.WidgetModel

/**
 * A launcher grant, named by its cells: the family's reference sizes, the ones the layout tests
 * measure against, so what a chip shows is what the tests hold.
 */
internal enum class PreviewSize(val label: String, val size: DpSize) {
    TWO_BY_ONE("2×1", DpSize(159.dp, 85.dp)),
    FOUR_BY_ONE("4×1", DpSize(340.dp, 85.dp)),
    TWO_BY_TWO("2×2", DpSize(159.dp, 189.dp)),
    FOUR_BY_TWO("4×2", DpSize(340.dp, 189.dp)),
    FOUR_BY_THREE("4×3", DpSize(340.dp, 293.dp)),
    ;

    companion object {
        /** Where a card lands when first placed: its provider's `targetCell*`. */
        val Default = FOUR_BY_TWO
    }
}

/**
 * The size this card really has on the home screen in portrait (the launcher's
 * `minWidth × maxHeight`), or null before its first layout.
 */
internal fun placedWidgetSize(context: Context, appWidgetId: Int): DpSize? {
    val options = runCatching { AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId) }.getOrNull()
        ?: return null
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
}

/**
 * The card as it will look, on a ground that stands in for a wallpaper, with the sizes it can be
 * given underneath. It opens on the size the card really has when the launcher has said it: the
 * player's own card first.
 */
@Composable
internal fun PreviewSection(model: WidgetModel?, placed: DpSize?) {
    var size by remember { mutableStateOf(placed ?: PreviewSize.Default.size) }
    Column(Modifier.fillMaxWidth()) {
        PreviewStage { CardPreview(model, size) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp)
                .testTag(WidgetConfigTags.SIZES),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            placed?.let { here ->
                FilterChip(
                    selected = size == here,
                    onClick = { size = here },
                    label = { Text(stringResource(R.string.widget_config_size_placed)) },
                )
            }
            PreviewSize.entries.forEach { option ->
                FilterChip(
                    selected = size == option.size && size != placed,
                    onClick = { size = option.size },
                    label = { Text(option.label) },
                )
            }
        }
        Text(
            text = stringResource(R.string.widget_config_resize_hint),
            style = MaterialTheme.typography.bodySmall.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
        )
    }
}

/** The ground a preview stands on: the theme's containers, standing in for a wallpaper. */
@Composable
internal fun PreviewStage(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)))
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        content()
    }
}

/**
 * The card itself, not a lookalike: Glance composes the same content function the receiver runs
 * into `RemoteViews` for the chosen size, and `RemoteViews.apply` inflates them as the launcher
 * does. The host swallows every touch, so the card's own doors never open from a preview, and it
 * is one picture to a screen reader.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
internal fun CardPreview(model: WidgetModel?, size: DpSize, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(model, size) {
        if (model == null) return@LaunchedEffect
        views = runCatching {
            GlanceRemoteViews().compose(context, size) { TodayCardContent(model) }.remoteViews
        }.getOrNull()
    }
    val description = stringResource(R.string.widget_config_preview_desc)
    BoxWithConstraints(modifier = modifier.fillMaxWidth().semantics { contentDescription = description }) {
        val scale = minOf(1f, maxWidth / size.width)
        Box(modifier = Modifier.fillMaxWidth().height(size.height * scale), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .requiredSize(size.width, size.height)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(dimensionResource(WidgetCornerRes))),
            ) {
                views?.let { remote ->
                    AndroidView(
                        factory = { TouchlessFrame(it) },
                        update = { frame ->
                            frame.removeAllViews()
                            runCatching { remote.apply(frame.context, frame) }.getOrNull()?.let { frame.addView(it) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@SuppressLint("ViewConstructor")
private class TouchlessFrame(context: Context) : FrameLayout(context) {
    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean = true
}
