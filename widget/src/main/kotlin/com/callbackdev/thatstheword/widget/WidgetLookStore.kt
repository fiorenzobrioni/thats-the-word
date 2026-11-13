package com.callbackdev.thatstheword.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.thatstheword.core.model.WidgetCardColor
import com.callbackdev.thatstheword.core.model.WidgetGround
import com.callbackdev.thatstheword.core.model.WidgetLook
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The file the cards' looks live in (PLANNING.md §5, `widgets`): outside the backup's allowlist
 * on purpose, because a restore renews the cards' ids and a look kept under an old id would dress
 * nothing, or the wrong card.
 */
internal val Context.widgetLookDataStore by preferencesDataStore(name = WidgetLookStore.FILE_NAME)

/**
 * The looks, keyed by appWidgetId. Presentation only, so it lives with the card and not in
 * `:core:data`; a removed card takes its look with it ([forget]).
 */
@Singleton
class WidgetLookStore internal constructor(private val dataStore: DataStore<Preferences>) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.widgetLookDataStore)

    suspend fun lookFor(appWidgetId: Int): WidgetLook {
        val prefs = dataStore.data.first()
        val default = WidgetLook()
        return WidgetLook(
            ground = prefs[groundKey(appWidgetId)]
                ?.let { name -> WidgetGround.entries.firstOrNull { it.name == name } }
                ?: default.ground,
            opacityPct = (prefs[opacityKey(appWidgetId)] ?: default.opacityPct).coerceIn(0, 100),
            cardColor = prefs[colorKey(appWidgetId)]
                ?.let { name -> WidgetCardColor.entries.firstOrNull { it.name == name } }
                ?: default.cardColor,
        )
    }

    suspend fun set(appWidgetId: Int, look: WidgetLook) {
        dataStore.edit { prefs ->
            prefs[groundKey(appWidgetId)] = look.ground.name
            prefs[opacityKey(appWidgetId)] = look.opacityPct.coerceIn(0, 100)
            prefs[colorKey(appWidgetId)] = look.cardColor.name
        }
    }

    /** From the receiver's `onDeleted`: a removed card leaves nothing behind. */
    suspend fun forget(appWidgetIds: IntArray) {
        dataStore.edit { prefs ->
            appWidgetIds.forEach { id ->
                prefs.remove(groundKey(id))
                prefs.remove(opacityKey(id))
                prefs.remove(colorKey(id))
            }
        }
    }

    private fun groundKey(id: Int) = stringPreferencesKey("ground_$id")
    private fun opacityKey(id: Int) = intPreferencesKey("opacity_$id")
    private fun colorKey(id: Int) = stringPreferencesKey("card_color_$id")

    companion object {
        const val FILE_NAME: String = "widgets"
    }
}
