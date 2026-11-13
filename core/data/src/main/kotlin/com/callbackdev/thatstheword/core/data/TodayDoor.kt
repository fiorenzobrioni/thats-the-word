package com.callbackdev.thatstheword.core.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.callbackdev.thatstheword.core.model.Language

/**
 * The one door into a language's word of today (PLANNING.md §8): the launcher's shortcuts, the
 * daily reminder's touch and a row of the home-screen card all open the game by it, and the game
 * reads it back ([languageOf]). Named here, where every one of them can see it.
 */
object TodayDoor {
    /** The language whose daily to open, its tag ("it"). */
    const val EXTRA_DAILY_LANGUAGE: String = "com.callbackdev.thatstheword.extra.DAILY_LANGUAGE"

    /**
     * The game, by the launcher's own intent (a touch brings back its task rather than a second
     * one), asked for [language]'s word of today. Each language's intent carries its own data, so
     * three rows of one card are three pending intents, never one.
     */
    fun intent(context: Context, language: Language): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.setAction(Intent.ACTION_VIEW)
            ?.setData(Uri.Builder().scheme("thatstheword").authority("today").appendPath(language.tag).build())
            ?.putExtra(EXTRA_DAILY_LANGUAGE, language.tag)

    /** The language an intent asks for, if it is one of these. */
    fun languageOf(intent: Intent?): Language? = intent?.getStringExtra(EXTRA_DAILY_LANGUAGE)?.let(Language::ofTag)
}

/**
 * The home-screen card, as Settings reaches it (PLANNING.md §8, the pin request): bound by
 * `:widget`, which the feature modules cannot see.
 */
interface HomeScreenCards {
    /** Whether the launcher takes a pin request. */
    fun canPin(): Boolean

    /** Asks the launcher to place the card; whether it took the request. */
    fun pin(): Boolean

    /** Whether a card is on a home screen now. */
    fun placed(): Boolean
}
