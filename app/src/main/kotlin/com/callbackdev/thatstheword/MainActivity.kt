package com.callbackdev.thatstheword

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.shell.ThatsTheWordRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * The one activity, edge to edge, in the default dress and the phone's light or dark. The
 * player's own appearance (theme, dress, typeface) arrives with the settings.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThatsTheWordTheme {
                ThatsTheWordRoot()
            }
        }
    }
}
