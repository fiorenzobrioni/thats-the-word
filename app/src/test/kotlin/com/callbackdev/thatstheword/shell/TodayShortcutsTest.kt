package com.callbackdev.thatstheword.shell

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** The launcher's "Today's word" (PLANNING.md §8): one a language, the game language first, in the app's words. */
@RunWith(AndroidJUnit4::class)
class TodayShortcutsTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun published() = ShortcutManagerCompat.getDynamicShortcuts(app).sortedBy { it.rank }

    @Test
    fun `one shortcut a language, the game language first`() {
        TodayShortcuts.publish(app, Language.SPANISH)
        assertThat(published().map { it.longLabel.toString() }).containsExactly(
            "Today’s word in Spanish",
            "Today’s word in Italian",
            "Today’s word in English",
        ).inOrder()
        val spanish = published().first().intent
        assertThat(TodayShortcuts.languageOf(spanish)).isEqualTo(Language.SPANISH)
    }

    @Test
    fun `a change of the game language, or of the app's, reaches the launcher`() {
        TodayShortcuts.publish(app, Language.ITALIAN)
        TodayShortcuts.publish(app, Language.ENGLISH)
        assertThat(published().first().shortLabel.toString()).isEqualTo("In English")
        TodayShortcuts.publish(app.inLanguage(Locale.ITALIAN), Language.ENGLISH)
        assertThat(published().map { it.shortLabel.toString() })
            .containsExactly("In inglese", "In italiano", "In spagnolo").inOrder()
    }

    @Test
    fun `an intent that is no shortcut's asks for nothing`() {
        assertThat(TodayShortcuts.languageOf(null)).isNull()
        assertThat(TodayShortcuts.languageOf(android.content.Intent())).isNull()
    }

    private fun Context.inLanguage(locale: Locale): Context =
        createConfigurationContext(Configuration(resources.configuration).apply { setLocale(locale) })
}
