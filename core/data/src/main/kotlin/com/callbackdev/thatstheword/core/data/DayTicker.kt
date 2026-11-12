package com.callbackdev.thatstheword.core.data

import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider

/**
 * Today's local date, and the next one when it begins (PLANNING.md §4.6, §9): the change of day
 * seen live by a page on screen, by one wait until midnight, never by an alarm or a poll. It runs
 * only while collected, so a page collects it in its lifecycle (`collectAsStateWithLifecycle`, the
 * view model's `WhileSubscribed`): nothing runs while the app is off screen, and a page that comes
 * back reads the clock afresh, a time zone crossed meanwhile included.
 */
class DayTicker @Inject constructor(private val clock: Provider<Clock>) {
    val dates: Flow<LocalDate> = flow {
        while (true) {
            val calendar = DailyCalendar(clock.get())
            emit(calendar.today())
            // A moment past midnight, so the clock read next is surely the new day's.
            delay(calendar.untilNextDay().toMillis() + PAST_MIDNIGHT_MILLIS)
        }
    }.distinctUntilChanged()

    private companion object {
        const val PAST_MIDNIGHT_MILLIS = 500L
    }
}
