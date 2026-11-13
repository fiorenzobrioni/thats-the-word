package com.callbackdev.thatstheword.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.domain.daily.DayStanding
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.WidgetCardColor
import com.callbackdev.thatstheword.core.model.WidgetGround
import com.callbackdev.thatstheword.core.model.WidgetLook
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's picture of the home-screen card (docs/screenshots), drawn by Glance as a launcher
 * draws it, on a stand-in wallpaper: the start page's sample day (English found in four, the
 * Italian word under way, the Spanish one waiting, six days in a row). Run with
 * `./gradlew :widget:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val output = System.getProperty("thatstheword.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private val day = WidgetSamples.today.copy(
        gameLanguage = Language.ENGLISH,
        days = listOf(
            DayStanding(Language.ENGLISH, DayStatus.SOLVED, 4, 6),
            DayStanding(Language.ITALIAN, DayStatus.IN_PROGRESS, 2, 6),
            DayStanding(Language.SPANISH, DayStatus.NOT_PLAYED, 0, 6),
        ),
        streak = 6,
    )

    /**
     * The card at four by two on its blue; two by two in plum and on the light ground, side by
     * side; one row high in teal.
     */
    @Test
    fun widget() {
        val panel = DpSize(361.dp, 189.dp)
        val half = DpSize(172.dp, 189.dp)
        val row = DpSize(361.dp, 85.dp)
        fun card(size: DpSize, look: WidgetLook = WidgetLook()) =
            size to renderCard(context, size) { TodayCardContent(WidgetSamples.model(look, today = day)) }
        val board = HomeBoard(context, widthDp = 393)
            .row(card(panel))
            .row(
                card(half, WidgetLook(cardColor = WidgetCardColor.PLUM)),
                card(half, WidgetLook(ground = WidgetGround.LIGHT)),
            )
            .row(card(row, WidgetLook(cardColor = WidgetCardColor.TEAL)))
        board.draw().saveTo(File(checkNotNull(output)), "widget")
    }
}
