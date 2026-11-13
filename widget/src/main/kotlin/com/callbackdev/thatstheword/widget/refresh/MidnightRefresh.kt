package com.callbackdev.thatstheword.widget.refresh

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.widget.HomeCards
import com.callbackdev.thatstheword.widget.logWidgetFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/*
 * How the card stays fresh (PLANNING.md §7.8, §9): pushed, never polled. Four ways in, nothing else:
 *
 * - a game saved, or a setting the card draws with: `WidgetUpdater`, once the write is committed;
 * - midnight, when a new word begins in every language: one inexact, NON-wakeup alarm at the next
 *   local midnight, armed after every render while a card is placed, delivered when the phone is
 *   next awake, which is when someone can see the card;
 * - the clock, the zone or the language changed, or the game was updated: the exempt broadcasts;
 * - a boot: the system sends the card its update, and the alarm is armed again from it.
 *
 * Whatever asked stays until the cards are drawn (`HomeCards.redrawAll`): Glance draws in a worker
 * of the game's process that no job holds awake, and a process left cached may be frozen with the
 * card half drawn.
 */

/** The card's one alarm: the next local midnight, inexact and non-wakeup, only while a card is placed. */
@Singleton
class MidnightAlarm
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val clock: Provider<Clock>,
) {
    /**
     * `AlarmManager.set` with `RTC`: inexact and non-wakeup, so it needs no permission and never
     * wakes the phone. One pending intent, so a new midnight replaces the old.
     */
    fun arm() {
        if (!HomeCards.hasCards(context)) return disarm()
        val now = clock.get()
        armAt(Instant.now(now).plus(DailyCalendar(now).untilNextDay()).plusMillis(AFTER_MIDNIGHT_MILLIS))
    }

    /** The last card is gone: nothing of the game waits on a midnight no card shows. */
    fun disarm() {
        context.getSystemService(AlarmManager::class.java)?.cancel(intent(context))
    }

    private fun armAt(at: Instant) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { alarms.set(AlarmManager.RTC, at.toEpochMilli(), intent(context)) }
            .onFailure { logWidgetFailure("Arming the midnight alarm failed", it) }
    }

    companion object {
        /** A breath past midnight: an alarm is never early, and today is then the new day. */
        private const val AFTER_MIDNIGHT_MILLIS = 1_000L

        /** How long a receiver stays for the cards it asked to redraw. */
        internal const val RECEIVER_DRAW_TIMEOUT_MILLIS = 8_000L

        internal fun intent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, MidnightReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/** The midnight alarm: a new word in every language. */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = redraw(context, "Midnight")
}

/**
 * The exempt broadcasts (PLANNING.md §7.8): the clock set, the zone changed (a trip moves the
 * days), the phone's language changed, the game updated. Received from the manifest, delivered by
 * the system only.
 */
class WidgetSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action in Actions) redraw(context, action.orEmpty().substringAfterLast('.'))
    }

    private companion object {
        val Actions = setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

/**
 * Every card drawn again within the receiver's own time (`goAsync`), never a service, and the
 * broadcast held open until the cards are drawn. Each render arms the next midnight.
 */
private fun BroadcastReceiver.redraw(context: Context, why: String) {
    val app = context.applicationContext
    if (!HomeCards.hasCards(app)) return
    val pending = goAsync()
    Redraws.launch {
        try {
            HomeCards.redrawAll(app, why, MidnightAlarm.RECEIVER_DRAW_TIMEOUT_MILLIS)
        } finally {
            pending.finish()
        }
    }
}

private val Redraws = CoroutineScope(SupervisorJob() + Dispatchers.Default)
