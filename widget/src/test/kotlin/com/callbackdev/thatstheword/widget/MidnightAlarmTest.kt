package com.callbackdev.thatstheword.widget

import android.app.AlarmManager
import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.widget.refresh.MidnightAlarm
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The card's one alarm (PLANNING.md §7.8, §9): the next local midnight, inexact and non-wakeup,
 * only while a card is placed; none at all without one.
 */
@RunWith(AndroidJUnit4::class)
class MidnightAlarmTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val rome: ZoneId = ZoneId.of("Europe/Rome")
    private val clock = Clock.fixed(LocalDateTime.parse("2026-11-13T21:40").atZone(rome).toInstant(), rome)
    private val alarm = MidnightAlarm(context) { clock }

    private fun placeACard() {
        val info = AppWidgetProviderInfo().apply { provider = ComponentName(context, TodayCardReceiver::class.java) }
        shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(41, info)
    }

    @Test
    fun `without a card, no alarm`() {
        alarm.arm()
        assertThat(shadowOf(alarms).scheduledAlarms).isEmpty()
    }

    @Test
    fun `with a card, one non-wakeup alarm just past the next local midnight`() {
        placeACard()
        alarm.arm()
        alarm.arm()
        val scheduled = shadowOf(alarms).scheduledAlarms
        assertThat(scheduled).hasSize(1)
        assertThat(scheduled.single().type).isEqualTo(AlarmManager.RTC)
        val midnight = LocalDateTime.parse("2026-11-14T00:00").atZone(rome).toInstant().toEpochMilli()
        assertThat(scheduled.single().triggerAtTime).isIn(midnight..midnight + 5_000)
    }

    @Test
    fun `the last card gone takes the alarm with it`() {
        placeACard()
        alarm.arm()
        alarm.disarm()
        assertThat(shadowOf(alarms).scheduledAlarms).isEmpty()
    }

    @Test
    fun `a configure request for anything but the game's placed card opens nothing`() {
        assertThat(HomeCards.isCard(context, 41)).isFalse()
        placeACard()
        assertThat(HomeCards.isCard(context, 41)).isTrue()
        assertThat(HomeCards.hasCards(context)).isTrue()
    }
}
