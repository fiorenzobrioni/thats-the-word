package com.callbackdev.thatstheword

import android.app.Application
import android.content.res.Configuration
import com.callbackdev.thatstheword.core.reminder.DailyReminder
import com.callbackdev.thatstheword.widget.WidgetPreviews
import com.callbackdev.thatstheword.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The application: Hilt's graph root. Nothing ever runs on a timer: the game has no service and
 * no background work of its own (PLANNING.md §9). It starts two listeners, idle until something
 * is written: the daily reminder's watch over its own setting, which arms one inexact alarm a day
 * while the player wants it (PLANNING.md §8); and the home-screen card's, which redraws a placed
 * card when a game changes what it shows (PLANNING.md §7.8). The card's picker preview is
 * published once per version.
 */
@HiltAndroidApp
class ThatsTheWordApplication : Application() {
    @Inject lateinit var reminder: DailyReminder

    @Inject lateinit var cards: WidgetUpdater

    private var locales: String? = null

    override fun onCreate() {
        super.onCreate()
        reminder.start()
        cards.start()
        locales = resources.configuration.locales.toLanguageTags()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching {
                val version = packageManager.getPackageInfo(packageName, 0).longVersionCode
                WidgetPreviews.publishIfNeeded(this@ThatsTheWordApplication, version)
            }
        }
    }

    /**
     * The interface's language changed while the game runs (the per-app picker): the card speaks
     * it at once, not at its next game.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val now = newConfig.locales.toLanguageTags()
        if (now != locales) {
            locales = now
            cards.languageChanged()
        }
    }
}
