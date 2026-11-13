package com.callbackdev.thatstheword.core.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The daily reminder (PLANNING.md §8): its one alarm, armed only while it is on and can show, at
 * the player's hour in the zone in effect; and its firing, which speaks only while today's word in
 * the game language is not finished. Unless a test moves it, the clock stands at Friday 20 Nov
 * 2026, 10:00 in Rome.
 */
@RunWith(AndroidJUnit4::class)
class DailyReminderTest {
    @get:Rule val folder = TemporaryFolder()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val rome = ZoneId.of("Europe/Rome")
    private var clock: Clock = at("2026-11-20T10:00")
    private val lexicons = Lexicons(AssetWordFiles(context.assets))
    private lateinit var database: ThatsTheWordDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var settings: SettingsRepository
    private lateinit var games: GameRepository
    private lateinit var reminder: DailyReminder

    private val alarms: ShadowAlarmManager get() = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val shade get() = shadowOf(context.getSystemService(NotificationManager::class.java))

    private fun at(local: String, zone: ZoneId = rome): Clock =
        Clock.fixed(LocalDateTime.parse(local).atZone(zone).toInstant(), zone)

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(context, ThatsTheWordDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "settings.preferences_pb") },
        ) { Language.ENGLISH }
        games = GameRepository(database, lexicons) { clock }
        reminder = DailyReminder(context, settings, games) { clock }
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
    }

    private suspend fun turnOn(time: LocalTime = LocalTime.of(20, 0)) {
        settings.setReminder(true)
        settings.setReminderTime(time)
    }

    private fun millis(local: String, zone: ZoneId = rome): Long =
        LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli()

    private fun dueAt(local: String): Instant = Instant.ofEpochMilli(millis(local))

    private fun theReminder() = shade.getNotification(ReminderNotifications.NOTIFICATION_ID)

    /** Today's word in [language] played to its end. */
    private suspend fun finishToday(language: Language) {
        val id = games.dailyGame(language, LocalDate.now(clock), language, hard = false)
        var game = games.load(id)!!.game
        while (!game.state.finished) {
            game = (game.submit(game.setup.solution, lexicons.of(language).accepted) as Submission.Accepted).game
        }
        games.save(id, game)
    }

    @Test
    fun `off, no alarm is armed`() = runTest {
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }

    @Test
    fun `on, one inexact alarm wakes the phone at the player's hour, let through Doze`() = runTest {
        turnOn()
        reminder.reschedule()
        val alarm = alarms.scheduledAlarms.single()
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(alarm.triggerAtTime).isEqualTo(millis("2026-11-20T20:00"))
        assertThat(alarm.allowWhileIdle).isTrue()
    }

    @Test
    fun `after the hour, tomorrow's, and armed again it replaces the one before`() = runTest {
        turnOn(LocalTime.of(9, 0))
        reminder.reschedule()
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms.single().triggerAtTime).isEqualTo(millis("2026-11-21T09:00"))
    }

    @Test
    fun `a new zone puts the alarm at its own hour`() = runTest {
        turnOn()
        reminder.reschedule()
        val lisbon = ZoneId.of("Europe/Lisbon")
        clock = at("2026-11-20T09:00", lisbon)
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms.single().triggerAtTime).isEqualTo(millis("2026-11-20T20:00", lisbon))
    }

    @Test
    fun `while the game may not notify no alarm wakes the phone, and its return arms one`() = runTest {
        turnOn()
        shade.setNotificationsEnabled(false)
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms).isEmpty()

        shade.setNotificationsEnabled(true)
        reminder.accessMayHaveChanged()
        assertThat(alarms.scheduledAlarms).hasSize(1)
    }

    @Test
    fun `the reminder's channel turned off arms nothing`() = runTest {
        turnOn()
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                ReminderNotifications.CHANNEL_ID,
                "Daily reminder",
                NotificationManager.IMPORTANCE_NONE,
            ),
        )
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(ReminderAccess.block(context)).isEqualTo(ReminderBlock.CHANNEL)
    }

    @Test
    fun `a word not played yet is announced until midnight, and tomorrow's alarm is armed`() = runTest {
        turnOn()
        settings.setGameLanguage(Language.ITALIAN)
        clock = at("2026-11-20T20:01")

        reminder.fire(dueAt("2026-11-20T20:00"))

        val notification = theReminder()
        assertThat(notification).isNotNull()
        assertThat(notification.extras.getString(NotificationCompat.EXTRA_TITLE)).isEqualTo("Today’s word is waiting")
        assertThat(notification.extras.getString(NotificationCompat.EXTRA_TEXT))
            .isEqualTo("In Italian, still to find. A touch opens it.")
        assertThat(notification.channelId).isEqualTo(ReminderNotifications.CHANNEL_ID)
        assertThat(notification.category).isEqualTo(NotificationCompat.CATEGORY_REMINDER)
        assertThat(notification.timeoutAfter).isEqualTo(Duration.ofMinutes(3 * 60 + 59).toMillis())
        assertThat(alarms.scheduledAlarms.single().triggerAtTime).isEqualTo(millis("2026-11-21T20:00"))
    }

    @Test
    fun `a word already finished stays quiet, whatever another language's`() = runTest {
        turnOn()
        finishToday(Language.ENGLISH)
        clock = at("2026-11-20T20:00")
        reminder.fire(dueAt("2026-11-20T20:00"))
        assertThat(theReminder()).isNull()
        assertThat(alarms.scheduledAlarms).hasSize(1)

        settings.setGameLanguage(Language.SPANISH)
        reminder.fire(dueAt("2026-11-20T20:00"))
        assertThat(theReminder()).isNotNull()
    }

    @Test
    fun `an alarm held back to the next day stays quiet`() = runTest {
        turnOn()
        clock = at("2026-11-21T07:00")
        reminder.fire(dueAt("2026-11-20T20:00"))
        assertThat(theReminder()).isNull()
        assertThat(alarms.scheduledAlarms.single().triggerAtTime).isEqualTo(millis("2026-11-21T20:00"))
    }

    @Test
    fun `turned off, the alarm and the reminder in the shade go`() = runTest {
        turnOn()
        clock = at("2026-11-20T20:00")
        reminder.fire(dueAt("2026-11-20T20:00"))
        assertThat(theReminder()).isNotNull()
        settings.setReminder(false)
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(theReminder()).isNull()
    }
}
