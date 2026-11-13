package com.callbackdev.thatstheword.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.collection.intSetOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.HomeScreenCards
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.daily.TodayWords
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * The game's side of the refresh (PLANNING.md §7.8, "after every game"): a change of what the card
 * draws (a word found, missed or begun in any language, the streak, the game language, the dress,
 * the wallpaper's colours), once its write is committed, redraws the cards at once. Whatever wrote
 * it: the game, the archive, a restore. Only with a card on a home screen, and only for a change the
 * card shows: a guess that leaves the day as it stood redraws nothing. Nothing runs on a timer, and
 * the listener costs nothing until something is written.
 */
@Singleton
class WidgetUpdater
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val clock: Provider<Clock>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** What the card draws: a change in anything else redraws nothing. */
    private data class Drawn(val today: TodayWords, val look: Any)

    /** From the Application: listens for as long as the process lives. */
    fun start() {
        scope.launch {
            combine(games.records(), settings.settings) { records, current ->
                Drawn(
                    TodayWords.of(records, current.gameLanguage, DailyCalendar(clock.get())),
                    current.palette to current.wallpaperColors,
                )
            }
                .distinctUntilChanged()
                .drop(1)
                .conflate()
                .collect { redraw() }
        }
    }

    /** The interface's language changed under a running game (the per-app picker): new words. */
    fun languageChanged() {
        scope.launch { redraw() }
    }

    private suspend fun redraw() {
        if (HomeCards.hasCards(context)) runCatching { HomeCards.updateAll(context) }
    }
}

/** The pin request (PLANNING.md §8), for Settings. */
@Singleton
class PinnedCards @Inject constructor(@ApplicationContext private val context: Context) : HomeScreenCards {
    private val manager: AppWidgetManager? get() = runCatching { AppWidgetManager.getInstance(context) }.getOrNull()

    override fun canPin(): Boolean = manager?.isRequestPinAppWidgetSupported == true

    override fun pin(): Boolean = runCatching {
        manager?.requestPinAppWidget(HomeCards.provider(context), null, null) == true
    }.getOrDefault(false)

    override fun placed(): Boolean = HomeCards.hasCards(context)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetModule {
    @Binds
    abstract fun homeScreenCards(pins: PinnedCards): HomeScreenCards
}

/**
 * The picker's generated preview (Android 15+): the real card, drawn from [WidgetSamples] by
 * `providePreview`, published once per app version. The platform rate-limits the call, and a
 * preview only changes when the app does, so the version it was published for is remembered; a
 * refused call is tried again at the next start. Below Android 15 the static `previewLayout` is
 * what the picker shows.
 */
object WidgetPreviews {
    suspend fun publishIfNeeded(context: Context, appVersion: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val store = context.widgetLookDataStore
        if (store.data.first()[PublishedFor] == appVersion) return
        val categories = intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
        // Bounded: this runs at every app start until it succeeds, and nothing is worth a
        // coroutine left hanging on the launcher's side of a binder call.
        val result = runCatching {
            withTimeoutOrNull(PUBLISH_TIMEOUT_MILLIS) {
                GlanceAppWidgetManager(context).setWidgetPreviews(TodayCardReceiver::class, categories)
            }
        }
            .onFailure { logWidgetFailure("Publishing the picker preview failed", it) }
            .getOrNull()
        if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
            store.edit { it[PublishedFor] = appVersion }
        }
    }

    private val PublishedFor = longPreferencesKey("previews_published_for")
    private const val PUBLISH_TIMEOUT_MILLIS = 30_000L
}
