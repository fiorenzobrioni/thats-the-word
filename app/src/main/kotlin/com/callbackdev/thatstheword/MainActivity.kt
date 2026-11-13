package com.callbackdev.thatstheword

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.callbackdev.thatstheword.core.model.UserSettings
import com.callbackdev.thatstheword.core.reminder.DailyReminder
import com.callbackdev.thatstheword.shell.ThatsTheWordRoot
import com.callbackdev.thatstheword.shell.TodayShortcuts
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The one activity, edge to edge, in the player's appearance (theme, dress, typeface, the
 * wallpaper's colours, motion: PLANNING.md §11 Phase 7), read from the settings as they change.
 * Until they are read (a frame or two at a cold start) nothing is drawn, so the first run and the
 * start page never flash one for the other.
 *
 * It keeps the launcher's shortcuts (`TodayShortcuts`) in the game language's order while it is on
 * screen, and opens the daily one of them, or the daily reminder, asks for, at a cold start and
 * when the game is already open (`singleTask`). Coming to the front takes the reminder back from
 * the shade: the player is here.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var reminder: DailyReminder

    private val dailyRequest = MutableStateFlow<Language?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A page restored after the system took the process away is not asked again.
        if (savedInstanceState == null) takeShortcut(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.settings.map { it.gameLanguage }.distinctUntilChanged().collect {
                    TodayShortcuts.publish(this@MainActivity, it)
                }
            }
        }
        setContent {
            val request by dailyRequest.collectAsStateWithLifecycle()
            val chosen by settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val current: UserSettings = chosen ?: return@setContent
            ThatsTheWordTheme(
                darkTheme = when (current.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
                dynamicColor = current.wallpaperColors,
                palette = current.palette,
                font = current.font,
                reducedMotion = current.reducedMotion,
            ) {
                ThatsTheWordRoot(
                    firstRunDone = current.firstRunDone,
                    dailyRequest = request,
                    onDailyRequestHandled = { dailyRequest.value = null },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        reminder.appOpened()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeShortcut(intent)
    }

    /** The daily a shortcut or the reminder asked for, once per touch. */
    private fun takeShortcut(intent: Intent?) {
        val language = TodayShortcuts.languageOf(intent) ?: return
        intent?.removeExtra(TodayShortcuts.EXTRA_DAILY_LANGUAGE)
        TodayShortcuts.reportUsed(this, language)
        dailyRequest.value = language
    }
}
