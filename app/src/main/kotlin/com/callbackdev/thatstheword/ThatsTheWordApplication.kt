package com.callbackdev.thatstheword

import android.app.Application
import com.callbackdev.thatstheword.core.reminder.DailyReminder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The application: Hilt's graph root. Nothing ever runs on a timer: the game has no service and
 * no background work of its own (PLANNING.md §9). The one thing it starts is the daily reminder's
 * watch over its own setting, which arms one inexact alarm a day while the player wants it, and
 * none otherwise (PLANNING.md §8).
 */
@HiltAndroidApp
class ThatsTheWordApplication : Application() {
    @Inject lateinit var reminder: DailyReminder

    override fun onCreate() {
        super.onCreate()
        reminder.start()
    }
}
