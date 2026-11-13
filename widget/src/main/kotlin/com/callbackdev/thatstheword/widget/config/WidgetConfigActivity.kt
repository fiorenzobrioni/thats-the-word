package com.callbackdev.thatstheword.widget.config

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.callbackdev.thatstheword.core.model.WidgetLook
import com.callbackdev.thatstheword.widget.HomeCards
import com.callbackdev.thatstheword.widget.WidgetLookStore
import com.callbackdev.thatstheword.widget.WidgetModel
import com.callbackdev.thatstheword.widget.WidgetModelLoader
import com.callbackdev.thatstheword.widget.WidgetSamples
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The launcher's door into one card's settings (`android:configure`, reconfigurable and optional:
 * a card is placed at once with the defaults, and a long press opens this). It wears the game's
 * own appearance: it is part of the game.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var looks: WidgetLookStore

    @Inject lateinit var loader: WidgetModelLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appWidgetId = configTarget(applicationContext, intent)
        if (appWidgetId == null) {
            finish()
            return
        }
        // The host expects this result whether or not anything changes.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        val placed = placedWidgetSize(applicationContext, appWidgetId)

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            var look by remember { mutableStateOf<WidgetLook?>(null) }
            var model by remember { mutableStateOf<WidgetModel?>(null) }
            LaunchedEffect(appWidgetId) {
                look = looks.lookFor(appWidgetId)
                // The games could not be read: the sample day still shows what a choice does.
                model = runCatching { loader.load(appWidgetId) }.getOrNull() ?: WidgetSamples.model()
            }

            fun save(next: WidgetLook) {
                look = next
                scope.launch {
                    looks.set(appWidgetId, next)
                    runCatching { HomeCards.updateOne(applicationContext, appWidgetId) }
                }
            }

            val current = settings ?: return@setContent
            ThatsTheWordTheme(
                darkTheme = when (current.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                },
                dynamicColor = current.wallpaperColors,
                palette = current.palette,
                font = current.font,
                reducedMotion = current.reducedMotion,
            ) {
                // Until the stored look is read, nothing: a page of defaults about to change would
                // show the player their choices in the wrong place.
                val chosen = look ?: return@ThatsTheWordTheme
                WidgetConfigScreen(
                    look = chosen,
                    preview = model,
                    placed = placed,
                    onLook = ::save,
                    onOpacityDrag = { look = chosen.copy(opacityPct = it) },
                    onOpacityDone = { look?.let(::save) },
                    onDone = ::finish,
                    palette = current.palette,
                    wallpaperColors = current.wallpaperColors,
                )
            }
        }
    }
}

/**
 * The card a configure request names, when it is one of the game's placed cards. The page is
 * exported, because the launcher opens it, so any app could open it with any id: one that is not
 * the game's card on a home screen gets nothing, rather than a page drawn from the player's games
 * that would write a look for a card that does not exist.
 */
internal fun configTarget(context: Context, intent: Intent?): Int? {
    val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        ?: AppWidgetManager.INVALID_APPWIDGET_ID
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return null
    return appWidgetId.takeIf { HomeCards.isCard(context, it) }
}
