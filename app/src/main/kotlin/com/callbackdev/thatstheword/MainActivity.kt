package com.callbackdev.thatstheword

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.shell.ThatsTheWordRoot
import com.callbackdev.thatstheword.shell.TodayShortcuts
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The one activity, edge to edge, in the default dress and the phone's light or dark. The
 * player's own appearance (theme, dress, typeface) arrives with the settings.
 *
 * It keeps the launcher's shortcuts (`TodayShortcuts`) in the game language's order while it is on
 * screen, and opens the daily one of them asks for, at a cold start and when the game is already
 * open (`singleTask`).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

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
            ThatsTheWordTheme {
                ThatsTheWordRoot(dailyRequest = request, onDailyRequestHandled = { dailyRequest.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeShortcut(intent)
    }

    /** The daily a shortcut asked for, once per touch. */
    private fun takeShortcut(intent: Intent?) {
        val language = TodayShortcuts.languageOf(intent) ?: return
        intent?.removeExtra(TodayShortcuts.EXTRA_DAILY_LANGUAGE)
        TodayShortcuts.reportUsed(this, language)
        dailyRequest.value = language
    }
}
