package com.callbackdev.thatstheword.feature.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.thatstheword.core.reminder.ReminderAccess
import com.callbackdev.thatstheword.core.reminder.ReminderBlock

/**
 * Settings on screen, from its view model. The page reads what only the system knows (whether the
 * reminder's notification can show, the app's version) and asks for what only it can ask: the
 * notification permission, the first time the reminder is turned on and while Android still lets
 * the question be asked; after a refusal it will not repeat, the page's note opens the system's
 * own page instead. The app's language is the system's per-app picker, opened here.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenData: () -> Unit,
    onOpenLicences: () -> Unit,
    onPlayGuided: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var block by remember { mutableStateOf(ReminderAccess.block(context)) }
    // Back from the system's pages (the permission, the notifications, the language), read again.
    LifecycleResumeEffect(Unit) {
        block = ReminderAccess.block(context)
        viewModel.accessMayHaveChanged()
        onPauseOrDispose {}
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        block = ReminderAccess.block(context)
        viewModel.accessMayHaveChanged()
    }
    val current = state ?: return
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val actions = remember(viewModel, current.settings.askedNotifications) {
        SettingsActions(
            gameLanguage = viewModel::setGameLanguage,
            level = viewModel::setLevel,
            hard = viewModel::setHard,
            appLanguage = { context.openSystemPage(appLanguageIntent(context)) },
            reminder = { on ->
                viewModel.setReminder(on)
                if (on && !notificationsGranted(context)) {
                    val activity = context.findActivity()
                    val mayAsk = !current.settings.askedNotifications ||
                        activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
                    if (mayAsk) {
                        viewModel.setAskedNotifications()
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
            reminderTime = viewModel::setReminderTime,
            fixReminder = {
                context.openSystemPage(
                    ReminderAccess.settingsIntent(
                        context,
                        block.takeIf { it != ReminderBlock.NONE } ?: ReminderBlock.APP,
                    ),
                )
            },
            sound = viewModel::setSound,
            haptics = viewModel::setHaptics,
            motion = viewModel::setReducedMotion,
            theme = viewModel::setTheme,
            palette = viewModel::setPalette,
            font = viewModel::setFont,
            wallpaperColors = viewModel::setWallpaperColors,
            playGuided = onPlayGuided,
            data = onOpenData,
            licences = onOpenLicences,
        )
    }
    SettingsScreen(
        state = current,
        onBack = onBack,
        actions = actions,
        reminderBlock = block,
        version = version,
        modifier = modifier,
    )
}

private fun notificationsGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** The system's per-app language page for this app (the family's way: PLANNING §15, Open 14). */
private fun appLanguageIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.fromParts("package", context.packageName, null))

/** A system page, if the phone has one to open: a button that does nothing is never drawn as one. */
private fun Context.openSystemPage(intent: Intent) {
    runCatching { startActivity(intent) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
