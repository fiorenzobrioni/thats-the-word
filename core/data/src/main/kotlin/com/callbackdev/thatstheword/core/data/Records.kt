package com.callbackdev.thatstheword.core.data

import com.callbackdev.thatstheword.core.data.GameRepository.Companion.MODE_DAILY
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.MODE_FREE
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.MODE_GUIDED
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.languageOf
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.parseStored
import com.callbackdev.thatstheword.core.data.GameRepository.Companion.stateOf
import com.callbackdev.thatstheword.core.data.db.GameEntity
import com.callbackdev.thatstheword.core.data.db.GameSummary
import com.callbackdev.thatstheword.core.domain.daily.DailyCalendar
import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.Level
import java.time.Clock
import java.time.LocalDate

/** A row of the games table as the progress reads it (PLANNING.md §4.8), without its moves. */
internal fun recordOf(summary: GameSummary, clock: Clock): GameRecord {
    val entity = summary.game
    val language = languageOf(entity.language)
    return GameRecord(
        language = language,
        interfaceLanguage = languageOf(entity.uiLanguage),
        mode = modeOf(entity, clock),
        solution = Alphabet.of(language).parseStored(entity.solution),
        state = stateOf(entity.status),
        attemptsUsed = summary.guessCount + summary.hintCount,
        attempts = entity.attempts,
        hints = summary.hintCount,
        hard = entity.hard,
        startedOn = LocalDate.ofEpochDay(entity.startedOn),
        practice = entity.practice > 0,
    )
}

internal fun modeOf(entity: GameEntity, clock: Clock): GameMode = when (entity.mode) {
    MODE_DAILY -> {
        val date = LocalDate.ofEpochDay(checkNotNull(entity.dailyDate) { "a daily without its date" })
        GameMode.Daily(date, DailyCalendar(clock).number(date))
    }

    MODE_FREE -> GameMode.Free(Level.ofCode(checkNotNull(entity.level) { "free play without its level" }))

    MODE_GUIDED -> GameMode.Guided

    else -> error("no mode is called ${entity.mode}")
}
