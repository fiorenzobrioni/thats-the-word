package com.callbackdev.thatstheword.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.domain.daily.DayStanding
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.WidgetCardColor
import com.callbackdev.thatstheword.core.model.WidgetGround
import com.callbackdev.thatstheword.core.model.WidgetLook
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every form of the card, every ground, every state of a day, drawn to `build/screenshots` to be
 * looked at after a change to the card (CLAUDE.md). Not an assertion: the arithmetic is pinned by
 * `CardFitTest`, and what is cut by `WidgetFitTest`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
class WidgetGalleryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val out = File("build/screenshots")

    /** Every state at once: found, missed, under way; no streak (today's word missed ends it). */
    private val mixed = WidgetSamples.today.copy(
        days = listOf(
            DayStanding(Language.ITALIAN, DayStatus.MISSED, 6, 6),
            DayStanding(Language.ENGLISH, DayStatus.SOLVED, 3, 6),
            DayStanding(Language.SPANISH, DayStatus.IN_PROGRESS, 1, 6),
        ),
        streak = 0,
    )

    @Test
    fun `every size`() {
        val board = HomeBoard(context)
        Grants.all.forEach { size ->
            board.row(size to renderCard(context, size) { TodayCardContent(WidgetSamples.model()) })
        }
        board.draw().saveTo(out, "widget-sizes")
    }

    @Test
    fun `every state of a day, and no streak running`() {
        val board = HomeBoard(context)
        listOf(Grants.OneByOne, Grants.TwoByOne, Grants.FourByOne, Grants.TwoByTwo, Grants.FourByTwo).forEach { size ->
            board.row(size to renderCard(context, size) { TodayCardContent(WidgetSamples.model(today = mixed)) })
        }
        board.draw().saveTo(out, "widget-states")
    }

    @Test
    fun `every ground, solid and see-through`() {
        val looks = listOf(
            WidgetLook(),
            WidgetLook(ground = WidgetGround.LIGHT),
            WidgetLook(ground = WidgetGround.DARK),
            WidgetLook(ground = WidgetGround.SYSTEM),
            WidgetLook(ground = WidgetGround.LIGHT, opacityPct = 70),
            WidgetLook(ground = WidgetGround.COLOR, opacityPct = 30),
        ) + WidgetCardColor.entries.drop(1).map { WidgetLook(cardColor = it) }
        listOf(false, true).forEach { lightWallpaper ->
            val board = HomeBoard(context)
            looks.forEach { look ->
                board.row(
                    Grants.FourByTwo to
                        renderCard(context, Grants.FourByTwo) { TodayCardContent(WidgetSamples.model(look)) },
                )
            }
            board.draw(
                dark = !lightWallpaper,
            ).saveTo(out, if (lightWallpaper) "widget-grounds-light" else "widget-grounds")
        }
    }

    @Test
    fun `before the first game, and a card that could not read the games`() {
        val waiting = WidgetSamples.today.copy(
            days = Language.entries.map { DayStanding(it, DayStatus.NOT_PLAYED, 0, 6) },
            streak = 0,
        )
        val board = HomeBoard(context)
        board.row(
            Grants.FourByTwo to
                renderCard(context, Grants.FourByTwo) { TodayCardContent(WidgetSamples.model(today = waiting)) },
        )
        board.row(
            Grants.FourByTwo to renderCard(context, Grants.FourByTwo) {
                TodayCardContent(WidgetModel.unavailable())
            },
        )
        board.row(
            Grants.TwoByOne to renderCard(context, Grants.TwoByOne) {
                TodayCardContent(WidgetModel.unavailable())
            },
        )
        board.draw().saveTo(out, "widget-empty")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `in Italian`() {
        val board = HomeBoard(context)
        listOf(
            Grants.TwoByOne,
            Grants.FourByOne,
            Grants.TwoByTwo,
            Grants.FourByTwo,
            Grants.FourByThree,
        ).forEach { size ->
            board.row(size to renderCard(context, size) { TodayCardContent(WidgetSamples.model()) })
        }
        board.draw().saveTo(out, "widget-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xhdpi")
    fun `in Spanish`() {
        val board = HomeBoard(context)
        listOf(
            Grants.TwoByOne,
            Grants.FourByOne,
            Grants.TwoByTwo,
            Grants.FourByTwo,
            Grants.FourByThree,
        ).forEach { size ->
            board.row(size to renderCard(context, size) { TodayCardContent(WidgetSamples.model(today = mixed)) })
        }
        board.draw().saveTo(out, "widget-es")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi", fontScale = 2f)
    fun `twice the text size`() {
        val board = HomeBoard(context)
        Grants.all.forEach { size ->
            board.row(size to renderCard(context, size) { TodayCardContent(WidgetSamples.model()) })
        }
        board.draw().saveTo(out, "widget-large-text")
    }
}
