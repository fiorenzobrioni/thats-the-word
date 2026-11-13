package com.callbackdev.thatstheword.widget

import android.content.Context
import android.text.format.DateFormat
import com.callbackdev.thatstheword.core.domain.daily.DayStanding
import com.callbackdev.thatstheword.core.domain.daily.DayStatus
import com.callbackdev.thatstheword.core.domain.daily.TodayWords
import com.callbackdev.thatstheword.core.domain.widget.CardWords
import com.callbackdev.thatstheword.core.model.Language
import java.time.format.DateTimeFormatter

/**
 * Every word the card says, in the interface's language (the context Glance composes with), and
 * their widths in the launcher's face ([measured]), for [com.callbackdev.thatstheword.core.domain
 * .widget.CardFit] to decide what fits. Capitals are drawn ([eyebrow]), never written in a string,
 * and TalkBack is given the words ([eyebrowWords]).
 */
internal class CardTexts(private val context: Context, val today: TodayWords) {
    private val locale = context.widgetLocale()

    fun name(language: Language): String = context.getString(
        when (language) {
            Language.ITALIAN -> R.string.widget_language_it
            Language.ENGLISH -> R.string.widget_language_en
            Language.SPANISH -> R.string.widget_language_es
        },
    )

    fun code(language: Language): String = context.getString(
        when (language) {
            Language.ITALIAN -> R.string.widget_code_it
            Language.ENGLISH -> R.string.widget_code_en
            Language.SPANISH -> R.string.widget_code_es
        },
    )

    private fun inSentence(language: Language): String = context.getString(
        when (language) {
            Language.ITALIAN -> R.string.widget_language_it_in_sentence
            Language.ENGLISH -> R.string.widget_language_en_in_sentence
            Language.SPANISH -> R.string.widget_language_es_in_sentence
        },
    )

    /** "Found in 4", "Waiting": a row's few words. */
    fun status(day: DayStanding): String = when (day.status) {
        DayStatus.NOT_PLAYED -> context.getString(R.string.widget_status_waiting)
        DayStatus.IN_PROGRESS -> context.getString(R.string.widget_status_under_way)
        DayStatus.SOLVED -> context.getString(R.string.widget_status_found, day.attemptsUsed)
        DayStatus.MISSED -> context.getString(R.string.widget_status_missed)
    }

    /** What TalkBack reads for a row, which opens its word: "Italian: found in 4 attempts". */
    fun rowDescription(day: DayStanding): String {
        val said = when (day.status) {
            DayStatus.NOT_PLAYED -> context.getString(R.string.widget_status_waiting_description)

            DayStatus.IN_PROGRESS -> context.getString(R.string.widget_status_under_way_description)

            DayStatus.SOLVED -> context.resources.getQuantityString(
                R.plurals.widget_status_found_description,
                day.attemptsUsed,
                day.attemptsUsed,
            )

            DayStatus.MISSED -> context.getString(R.string.widget_status_missed_description)
        }
        return context.getString(R.string.widget_row_description, name(day.language), said)
    }

    val eyebrowWords: String = context.getString(R.string.widget_eyebrow, today.number)
    val eyebrow: String = eyebrowWords.uppercase(locale)
    val eyebrowShort: String = context.getString(R.string.widget_eyebrow_short, today.number)

    /** "Friday 13 November", "Venerdì 13 novembre": the locale's own order, its first letter a capital. */
    val date: String = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale)
        .format(today.date)
        .replaceFirstChar { it.titlecase(locale) }

    val streakLong: String = context.resources.getQuantityString(
        R.plurals.widget_streak_long,
        today.streak,
        today.streak,
        inSentence(today.gameLanguage),
    )
    val streakShort: String =
        context.resources.getQuantityString(R.plurals.widget_streak_short, today.streak, today.streak)
    val streakFigure: String = today.streak.toString()
    val streakWord: String = context.resources.getQuantityString(R.plurals.widget_streak_word, today.streak)

    /** The words' widths, in ems of the launcher's face, the widest of each kind. */
    fun measured(): CardWords {
        val languages = today.days.map { it.language }
        val streak = today.streak > 0
        return CardWords(
            // Bold: the game language's name is set so, and a row's width holds whichever it is.
            nameEm = languages.maxOf { textEm(context, name(it), TextWeight.BOLD) },
            codeEm = languages.maxOf { textEm(context, code(it), TextWeight.BOLD) },
            statusEm = today.days.maxOf { textEm(context, status(it), TextWeight.REGULAR) },
            eyebrowEm = textEm(context, eyebrow, TextWeight.MEDIUM),
            eyebrowShortEm = textEm(context, eyebrowShort, TextWeight.MEDIUM),
            dateEm = textEm(context, date, TextWeight.MEDIUM),
            streakEm = if (streak) textEm(context, streakLong, TextWeight.REGULAR) else 0f,
            streakShortEm = if (streak) textEm(context, streakShort, TextWeight.REGULAR) else 0f,
            streakFigureEm = if (streak) textEm(context, streakFigure, TextWeight.BOLD) else 0f,
            streakWordEm = if (streak) textEm(context, streakWord, TextWeight.REGULAR) else 0f,
        )
    }
}
