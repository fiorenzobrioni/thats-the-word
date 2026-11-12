package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.model.GameState

/**
 * The statistics of a set of games (PLANNING.md §4.8), by query from the games, never counters
 * that could drift. The caller chooses the set: a language and a mode. Practice replays and games
 * not finished are left out.
 *
 * [distribution] counts the games won by the attempts they used, a hint being one (PLANNING.md
 * §4.5): the same number as a result's "4/6". Games with hints and games with Hard are counted apart
 * (VISION §9).
 */
data class Statistics(
    val played: Int,
    val won: Int,
    val distribution: Map<Int, Int>,
    val withHints: Int,
    val withHard: Int,
) {
    /** The share won, from 0 to 1; 0 when nothing was played. */
    val winShare: Double get() = if (played == 0) 0.0 else won.toDouble() / played

    companion object {
        fun of(records: List<GameRecord>): Statistics {
            val counted = records.filter { it.counted }
            val won = counted.filter { it.state == GameState.WON }
            return Statistics(
                played = counted.size,
                won = won.size,
                distribution = won.groupingBy { it.attemptsUsed }.eachCount().toSortedMap(),
                withHints = counted.count { it.hints > 0 },
                withHard = counted.count { it.hard },
            )
        }
    }
}
