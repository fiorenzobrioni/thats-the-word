package com.callbackdev.thatstheword.core.domain

import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import java.time.LocalDate

/** A daily's record for a test: started on its own date unless [startedOn] says otherwise. */
fun daily(
    date: LocalDate,
    state: GameState = GameState.WON,
    language: Language = Language.ITALIAN,
    interfaceLanguage: Language = Language.ITALIAN,
    startedOn: LocalDate = date,
    attemptsUsed: Int = 4,
    hints: Int = 0,
    hard: Boolean = false,
    practice: Boolean = false,
) = GameRecord(
    language = language,
    interfaceLanguage = interfaceLanguage,
    mode = GameMode.Daily(date, 1),
    solution = word("cuore", Language.ITALIAN),
    state = state,
    attemptsUsed = if (state == GameState.LOST) 6 else attemptsUsed,
    attempts = 6,
    hints = hints,
    hard = hard,
    startedOn = startedOn,
    practice = practice,
)

fun free(
    state: GameState = GameState.WON,
    language: Language = Language.ITALIAN,
    interfaceLanguage: Language = Language.ITALIAN,
    attemptsUsed: Int = 3,
) = GameRecord(
    language = language,
    interfaceLanguage = interfaceLanguage,
    mode = GameMode.Free(Level.BASE),
    solution = word("cuore", Language.ITALIAN),
    state = state,
    attemptsUsed = attemptsUsed,
    attempts = 6,
    hints = 0,
    hard = false,
    startedOn = LocalDate.of(2026, 12, 1),
)

val DAY: LocalDate = LocalDate.of(2026, 12, 10)
