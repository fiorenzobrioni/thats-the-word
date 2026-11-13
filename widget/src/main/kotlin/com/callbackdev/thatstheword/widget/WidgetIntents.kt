package com.callbackdev.thatstheword.widget

import android.content.Context
import android.content.Intent
import com.callbackdev.thatstheword.core.data.TodayDoor
import com.callbackdev.thatstheword.core.model.Language

/**
 * The doors a card's touches open (PLANNING.md §7.8): a language's row, its word of today; the rest
 * of the card, the game's start page.
 */
internal object WidgetIntents {
    /** The app, by the launcher's own intent: a touch brings back its task rather than a second one. */
    fun openApp(context: Context): Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)

    fun today(context: Context, language: Language): Intent? = TodayDoor.intent(context, language)
}
