package com.callbackdev.thatstheword.widget

import android.util.Log
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.daily.TodayWords
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.WidgetLook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.time.Clock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * What a card knows when it draws: today's word in each language and the streak, read at render
 * time from the games and the date (PLANNING.md §7.8: a card never shows a day that has ended), and
 * the dress it is drawn in.
 *
 * @property today null when the games could not be read (an error, or a read that did not finish
 *   in time): said, not waited on.
 */
data class WidgetModel(
    val look: WidgetLook,
    val palette: AppPalette,
    val wallpaperColors: Boolean,
    val today: TodayWords?,
) {
    companion object {
        /** A card that says it could not read the games, in the default dress. */
        fun unavailable(): WidgetModel = WidgetModel(WidgetLook(), AppPalette.VIVID, false, today = null)
    }
}

/** Reads a [WidgetModel]: the games, the settings, the card's look and today's date. */
@Singleton
class WidgetModelLoader
@Inject
constructor(
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val looks: WidgetLookStore,
    private val clock: Provider<Clock>,
) {
    suspend fun load(appWidgetId: Int): WidgetModel {
        val current = settings.current()
        return WidgetModel(
            look = looks.lookFor(appWidgetId),
            palette = current.palette,
            wallpaperColors = current.wallpaperColors,
            today = TodayWords.of(games.records().first(), current.gameLanguage, DailyCalendar(clock.get())),
        )
    }

    /**
     * [load] for a card that is waiting to be drawn: never longer than [LOAD_TIMEOUT_MILLIS] and
     * never an exception, because Glance shows its loading spinner until `provideContent` is
     * reached and a card stuck on it says nothing to anybody. A failure is logged under [TAG] and
     * drawn as a card that says the games could not be read; the next redraw tries again.
     */
    suspend fun loadForCard(appWidgetId: Int): WidgetModel =
        guardedLoad(LOAD_TIMEOUT_MILLIS, "card $appWidgetId") { load(appWidgetId) }

    companion object {
        /** The log tag of everything the cards report: `adb logcat -s ThatsTheWordCard`. */
        const val TAG: String = "ThatsTheWordCard"

        /** A read is a few queries on the phone's own storage; ten seconds is something wrong. */
        const val LOAD_TIMEOUT_MILLIS: Long = 10_000L
    }
}

/**
 * [load], bounded by [timeoutMillis] and never throwing but for cancellation: a failure is logged
 * as [what] and becomes [WidgetModel.unavailable]. Split off the loader so a test can pin it.
 */
internal suspend fun guardedLoad(timeoutMillis: Long, what: String, load: suspend () -> WidgetModel): WidgetModel =
    try {
        withTimeout(timeoutMillis) { load() }
    } catch (e: TimeoutCancellationException) {
        logWidgetFailure("Reading the games for $what took over $timeoutMillis ms", e)
        WidgetModel.unavailable()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logWidgetFailure("Reading the games for $what failed", e)
        WidgetModel.unavailable()
    }

/** A failure, in the log; quiet on a JVM without Android's log, where the tests run. */
internal fun logWidgetFailure(message: String, error: Throwable) {
    runCatching { Log.w(WidgetModelLoader.TAG, message, error) }
}

/** A line of what the cards did, for `adb logcat -s ThatsTheWordCard`. */
internal fun logWidget(message: String) {
    runCatching { Log.i(WidgetModelLoader.TAG, message) }
}
