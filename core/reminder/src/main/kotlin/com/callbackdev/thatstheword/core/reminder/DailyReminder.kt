package com.callbackdev.thatstheword.core.reminder

import android.content.Context
import android.util.Log
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.reminder.ReminderSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * The daily reminder (PLANNING.md §8): "Today's word is waiting", at the player's hour, only on a
 * day whose word in the game language is not finished yet, never otherwise.
 *
 * Nothing here runs on a timer. One inexact alarm ([ReminderAlarm]), armed again whenever what it
 * depends on changes: at every start of the process (a boot and an update start it, and a boot
 * clears every alarm), at a change of the setting or its hour ([start]), at a change of the clock
 * or the zone and of the notifications' state ([ReminderReceiver]), and at each firing. While the
 * reminder is off, or its notification could not show, no alarm is armed at all: no wake for
 * nothing (PLANNING.md §9).
 */
@Singleton
class DailyReminder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val games: GameRepository,
    private val clock: Provider<Clock>,
) {
    private val alarm = ReminderAlarm(context)
    private val notifications = ReminderNotifications(context)
    private val scheduleLock = Mutex()
    private val started = AtomicBoolean(false)

    // Whether the reminder could reach the player when the alarm was last armed or cancelled.
    @Volatile private var armedForAccess: Boolean? = null

    /**
     * Keeps the alarm in step with the setting for as long as the process lives. Called once, by
     * the application: the channel is made (so Settings can open its page before the first
     * notification), and the alarm armed now and at every change of the setting or its hour.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        notifications.ensureChannel()
        Background.launch {
            settings.settings
                .map { Inputs(it.reminder, it.reminderTime) }
                .distinctUntilChanged()
                .collect { guarded("Arming the reminder") { reschedule() } }
        }
    }

    /**
     * Arms, moves or cancels the alarm for what the setting says now: the player's hour, next after
     * now, in the zone in effect now. Turned off, the alarm goes, and so does a reminder still in
     * the shade: the player said they do not want it.
     */
    suspend fun reschedule() = scheduleLock.withLock {
        val current = settings.current()
        val mayNotify = mayNotify()
        armedForAccess = mayNotify
        val now = clock.get()
        if (current.reminder && mayNotify) {
            alarm.set(ReminderSchedule.next(Instant.now(now), now.zone, current.reminderTime))
        } else {
            alarm.cancel()
        }
        if (!current.reminder) notifications.cancel()
    }

    /**
     * The alarm went off, due at [dueAt]. The next one is armed first, whatever this one decides.
     * It speaks only if the reminder is still on, its day is not over (Doze or a phone switched off
     * can hold an alarm back), and today's word in the game language is not finished.
     */
    suspend fun fire(dueAt: Instant?) = guarded("The daily reminder") {
        reschedule()
        val current = settings.current()
        if (!current.reminder) return@guarded
        val now = clock.get()
        if (dueAt != null && !ReminderSchedule.stillTimely(dueAt, Instant.now(now), now.zone)) return@guarded
        if (games.dailyFinished(current.gameLanguage, LocalDate.now(now))) return@guarded
        notifications.ensureChannel()
        notifications.post(current.gameLanguage, ReminderSchedule.untilEndOfDay(Instant.now(now), now.zone))
    }

    /**
     * The reminder's notifications may have been turned off or on in the system's settings, or the
     * permission given (the app coming back to the front, the system's broadcast, Settings' own
     * request): the alarm follows, if that changed anything since it was armed.
     */
    suspend fun accessMayHaveChanged() {
        if (armedForAccess != mayNotify()) guarded("Arming the reminder") { reschedule() }
    }

    /**
     * The app came to the front. The reminder in the shade has done its work: the player is here,
     * where it was meant to bring them. And the notifications may have changed meanwhile.
     */
    fun appOpened() {
        notifications.cancel()
        Background.launch { accessMayHaveChanged() }
    }

    private fun mayNotify(): Boolean = ReminderAccess.block(context) == ReminderBlock.NONE

    /** A failure costs one notification or one arming, logged, never the caller (a receiver, the app). */
    private suspend fun guarded(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$what failed", e)
        }
    }

    /** What the alarm depends on among the settings: a change in anything else arms nothing. */
    private data class Inputs(val on: Boolean, val time: LocalTime)

    companion object {
        /**
         * The extra that opens a language's word of today, from the reminder's touch and from the
         * launcher's shortcuts alike: its value is the language's tag.
         */
        const val EXTRA_DAILY_LANGUAGE = "com.callbackdev.thatstheword.extra.DAILY_LANGUAGE"

        private const val TAG = "ThatsTheWordReminder"

        /** Outlives any one caller: the setting is watched for the whole process. */
        private val Background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
