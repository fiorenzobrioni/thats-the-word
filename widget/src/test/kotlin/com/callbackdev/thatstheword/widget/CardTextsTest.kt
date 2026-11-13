package com.callbackdev.thatstheword.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.domain.daily.DayStanding
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What the card says, and what TalkBack reads of it, in the three languages. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardTextsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun texts() = CardTexts(context, WidgetSamples.today)

    @Test
    @Config(qualifiers = "en-rGB")
    fun `in English`() {
        val texts = texts()
        assertThat(texts.eyebrow).isEqualTo("TODAY’S WORD · #13")
        assertThat(texts.eyebrowWords).isEqualTo("Today’s word · #13")
        assertThat(texts.date).isEqualTo("Friday 13 November")
        assertThat(texts.streakLong).isEqualTo("6 days in a row in Italian")
        assertThat(texts.streakShort).isEqualTo("6 days in a row")
        val (italian, english, spanish) = WidgetSamples.today.days
        assertThat(texts.status(italian)).isEqualTo("Found in 4")
        assertThat(texts.status(english)).isEqualTo("Under way")
        assertThat(texts.status(spanish)).isEqualTo("Waiting")
        assertThat(texts.rowDescription(italian)).isEqualTo("Italian: found in 4 attempts")
        assertThat(texts.rowDescription(spanish)).isEqualTo("Spanish: waiting")
        assertThat(
            texts.rowDescription(DayStanding(Language.ENGLISH, DayStatus.MISSED, 6, 6)),
        ).isEqualTo("English: missed")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian`() {
        val texts = texts()
        assertThat(texts.eyebrow).isEqualTo("LA PAROLA DI OGGI · #13")
        assertThat(texts.date).isEqualTo("Venerdì 13 novembre")
        assertThat(texts.streakLong).isEqualTo("6 giorni di fila in italiano")
        assertThat(texts.rowDescription(WidgetSamples.today.days.first())).isEqualTo("Italiano: trovata in 4 tentativi")
    }

    @Test
    @Config(qualifiers = "es-rES")
    fun `in Spanish`() {
        val texts = texts()
        assertThat(texts.date).isEqualTo("Viernes, 13 de noviembre")
        assertThat(texts.streakLong).isEqualTo("6 días seguidos en italiano")
        assertThat(texts.status(WidgetSamples.today.days.first())).isEqualTo("Hallada en 4")
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `TalkBack reads each row as one sentence, and the eyebrow as words`() {
        val described = describedViews(context, Grants.FourByTwo) { TodayCardContent(WidgetSamples.model()) }
        assertThat(described).containsAtLeast(
            "Today’s word · #13",
            "Italian: found in 4 attempts",
            "English: under way",
            "Spanish: waiting",
        ).inOrder()
    }
}
