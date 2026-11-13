package com.callbackdev.thatstheword.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.thatstheword.core.domain.daily.DayStanding
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.domain.daily.TodayWords
import com.callbackdev.thatstheword.core.model.AppPalette
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.WidgetLook
import java.time.LocalDate

/**
 * The picker's sample day (Android 15+, `providePreview`) and the settings screen's before the
 * first game: a fixed Friday, the 13th daily, Italian found in 4, English under way, Spanish
 * waiting, a streak of 6. The words are the player's language's, as the picker draws them.
 */
object WidgetSamples {
    val FourByTwo = DpSize(340.dp, 189.dp)
    val TwoByTwo = DpSize(159.dp, 189.dp)

    private val Day: LocalDate = LocalDate.of(2026, 11, 13)

    val today = TodayWords(
        date = Day,
        number = 13,
        gameLanguage = Language.ITALIAN,
        days = listOf(
            DayStanding(Language.ITALIAN, DayStatus.SOLVED, 4, 6),
            DayStanding(Language.ENGLISH, DayStatus.IN_PROGRESS, 2, 6),
            DayStanding(Language.SPANISH, DayStatus.NOT_PLAYED, 0, 6),
        ),
        streak = 6,
    )

    fun model(
        look: WidgetLook = WidgetLook(),
        palette: AppPalette = AppPalette.VIVID,
        today: TodayWords = this.today,
    ): WidgetModel = WidgetModel(look = look, palette = palette, wallpaperColors = false, today = today)
}
