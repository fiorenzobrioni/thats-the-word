package com.callbackdev.thatstheword.shell

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.callbackdev.thatstheword.R
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.reminder.DailyReminder

/**
 * "Today's word" in each game language on the launcher's long press (PLANNING.md §8): each opens
 * its language's daily directly, the game language first.
 *
 * Dynamic, not declared in XML: a static shortcut names its package, and the debug build's is
 * another (`.debug`). The launcher keeps them with the words and the order they were given:
 * published again when those are not the app's any more (the player changed the interface's
 * language, or the game language), and left alone otherwise.
 */
object TodayShortcuts {
    /** The daily reminder's touch asks with the same extra: one door into today's word. */
    const val EXTRA_DAILY_LANGUAGE = DailyReminder.EXTRA_DAILY_LANGUAGE

    fun publish(context: Context, gameLanguage: Language) {
        val order = listOf(gameLanguage) + Language.entries.filter { it != gameLanguage }
        val wanted = order.map { language ->
            Triple(idOf(language), context.getString(shortLabel(language)), context.getString(longLabel(language)))
        }
        val published = ShortcutManagerCompat.getDynamicShortcuts(context)
            .sortedBy { it.rank }
            .map { Triple(it.id, it.shortLabel.toString(), it.longLabel?.toString()) }
        if (published == wanted) return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val shortcuts = order.mapIndexed { rank, language ->
            ShortcutInfoCompat.Builder(context, idOf(language))
                .setShortLabel(context.getString(shortLabel(language)))
                .setLongLabel(context.getString(longLabel(language)))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setRank(rank)
                .setIntent(
                    Intent(launch)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(EXTRA_DAILY_LANGUAGE, language.tag),
                )
                .build()
        }
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: IllegalStateException) {
            // The launcher's rate limit: the next start publishes them.
        }
    }

    /** Tells the launcher a shortcut was used, so it can offer it where it predicts the next app. */
    fun reportUsed(context: Context, language: Language) =
        ShortcutManagerCompat.reportShortcutUsed(context, idOf(language))

    /** The language a shortcut's intent asks for, if it is one. */
    fun languageOf(intent: Intent?): Language? = intent?.getStringExtra(EXTRA_DAILY_LANGUAGE)?.let(Language::ofTag)

    private fun idOf(language: Language) = "today_${language.tag}"

    @StringRes
    private fun shortLabel(language: Language): Int = when (language) {
        Language.ITALIAN -> R.string.shortcut_today_it
        Language.ENGLISH -> R.string.shortcut_today_en
        Language.SPANISH -> R.string.shortcut_today_es
    }

    @StringRes
    private fun longLabel(language: Language): Int = when (language) {
        Language.ITALIAN -> R.string.shortcut_today_it_long
        Language.ENGLISH -> R.string.shortcut_today_en_long
        Language.SPANISH -> R.string.shortcut_today_es_long
    }
}
