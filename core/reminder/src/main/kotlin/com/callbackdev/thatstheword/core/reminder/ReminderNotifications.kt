package com.callbackdev.thatstheword.core.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.callbackdev.thatstheword.core.model.Language
import java.time.Duration

/**
 * The reminder's channel and its one notification (PLANNING.md §8): `reminder`, at the default
 * importance, since it is something the player asked for in Settings. One sentence about today's
 * word; a touch opens it, and it goes by itself at midnight, when another word is today's.
 */
internal class ReminderNotifications(private val context: Context) {
    /** Made, or renamed into the app's language, at every start of the process. */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** "Today's word is waiting", in [language], for [lifetime]: the rest of the day. */
    fun post(language: Language, lifetime: Duration) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_word)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text, context.getString(languageName(language))))
            .setContentIntent(openToday(language))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setTimeoutAfter(lifetime.toMillis())
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Takes the reminder back from the shade, if it is there. */
    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /**
     * The app's own front door, asked for today's word in [language], as a launcher's shortcut
     * asks: this module does not know the app's activity, and should not.
     */
    private fun openToday(language: Language): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(it).setAction(Intent.ACTION_VIEW).putExtra(DailyReminder.EXTRA_DAILY_LANGUAGE, language.tag),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

    @StringRes
    private fun languageName(language: Language): Int = when (language) {
        Language.ITALIAN -> R.string.reminder_language_it
        Language.ENGLISH -> R.string.reminder_language_en
        Language.SPANISH -> R.string.reminder_language_es
    }

    companion object {
        const val CHANNEL_ID = "reminder"
        const val NOTIFICATION_ID = 1
    }
}

/** What stands between the reminder and the player, if anything. */
enum class ReminderBlock {
    /** It can show. */
    NONE,

    /** The game may not notify at all: the permission not given yet, refused, or its notifications off. */
    APP,

    /** The game may notify, but the player turned the reminder's channel off in the system's settings. */
    CHANNEL,
}

/**
 * Whether the reminder the player turned on can reach them: a switch that is on while Android drops
 * what it sends would be the screen lying, so Settings says so and opens the page that fixes it,
 * and no alarm wakes the phone for a notification that cannot show (PLANNING.md §8).
 */
object ReminderAccess {
    fun block(context: Context): ReminderBlock {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return ReminderBlock.APP
        val channel = manager.getNotificationChannel(ReminderNotifications.CHANNEL_ID)
        return if (channel?.importance ==
            NotificationManager.IMPORTANCE_NONE
        ) {
            ReminderBlock.CHANNEL
        } else {
            ReminderBlock.NONE
        }
    }

    /** The system's page that lifts [block]: the reminder's channel, or the app's notifications. */
    fun settingsIntent(context: Context, block: ReminderBlock): Intent = if (block == ReminderBlock.CHANNEL) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderNotifications.CHANNEL_ID)
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
}
