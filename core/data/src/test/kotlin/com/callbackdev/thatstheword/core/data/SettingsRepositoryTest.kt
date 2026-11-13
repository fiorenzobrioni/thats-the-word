package com.callbackdev.thatstheword.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.ThemeMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalTime

/** The settings file (PLANNING.md §5): what was never chosen reads as its default. */
class SettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private fun TestScope.repository(interfaceLanguage: Language = Language.SPANISH) = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.root.resolve("settings.preferences_pb")
        },
    ) { interfaceLanguage }

    @Test
    fun `the defaults`() = runTest {
        val settings = repository().current()
        assertThat(settings.gameLanguage).isEqualTo(Language.SPANISH)
        assertThat(settings.levelOf(Language.ITALIAN)).isEqualTo(Level.BASE)
        assertThat(settings.hard).isFalse()
        // The owner, PLANNING §15, Open 13.
        assertThat(settings.sound).isTrue()
        assertThat(settings.haptics).isTrue()
        assertThat(settings.reducedMotion).isNull()
        assertThat(settings.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(settings.palette).isEqualTo(AppPalette.VIVID)
        assertThat(settings.firstRunDone).isFalse()
        // The reminder, off until the player turns it on (PLANNING.md §8).
        assertThat(settings.reminder).isFalse()
        assertThat(settings.reminderTime).isEqualTo(LocalTime.of(20, 0))
        assertThat(settings.askedNotifications).isFalse()
    }

    @Test
    fun `what is chosen is read back`() = runTest {
        val repository = repository()
        repository.setGameLanguage(Language.ITALIAN)
        repository.setLevel(Language.ITALIAN, Level.ADVANCED)
        repository.setHard(true)
        repository.setHaptics(false)
        repository.setReducedMotion(true)
        repository.setPalette(AppPalette.PAPER)
        val settings = repository.current()
        assertThat(settings.gameLanguage).isEqualTo(Language.ITALIAN)
        assertThat(settings.levelOf(Language.ITALIAN)).isEqualTo(Level.ADVANCED)
        assertThat(settings.levelOf(Language.ENGLISH)).isEqualTo(Level.BASE)
        assertThat(settings.hard).isTrue()
        assertThat(settings.haptics).isFalse()
        assertThat(settings.reducedMotion).isTrue()
        assertThat(settings.palette).isEqualTo(AppPalette.PAPER)

        repository.setReducedMotion(null)
        assertThat(repository.current().reducedMotion).isNull()
    }

    @Test
    fun `the reminder, its hour to the minute, and the question asked once`() = runTest {
        val repository = repository()
        repository.setReminder(true)
        repository.setReminderTime(LocalTime.of(7, 45, 30))
        repository.setAskedNotifications()
        val settings = repository.current()
        assertThat(settings.reminder).isTrue()
        assertThat(settings.reminderTime).isEqualTo(LocalTime.of(7, 45))
        assertThat(settings.askedNotifications).isTrue()
    }

    @Test
    fun `a restore brings the reminder and keeps the phone's own question`() = runTest {
        val repository = repository()
        repository.setAskedNotifications()
        val chosen = repository.current().copy(
            reminder = true,
            reminderTime = LocalTime.of(18, 30),
            askedNotifications = false,
        )
        repository.restore(chosen)
        val settings = repository.current()
        assertThat(settings.reminder).isTrue()
        assertThat(settings.reminderTime).isEqualTo(LocalTime.of(18, 30))
        assertThat(settings.askedNotifications).isTrue()
    }
}
