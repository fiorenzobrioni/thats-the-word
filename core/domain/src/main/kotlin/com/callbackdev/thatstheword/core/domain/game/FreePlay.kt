package com.callbackdev.thatstheword.core.domain.game

import com.callbackdev.thatstheword.core.model.Word
import kotlin.random.Random

/**
 * Free play's draw (PLANNING.md §4.7): a solution of the level (cumulative, the owner, PLANNING §15,
 * Open 12), never one of the language's last [RECENT] free games nor one solved in free play
 * before, until the level runs out; then the oldest come back, the word whose last free game is the
 * longest ago first.
 */
object FreePlay {
    const val RECENT = 200

    /** A past free game of the language: its solution, and whether it was solved. */
    data class Played(val solution: Word, val solved: Boolean)

    /**
     * [level] is the level's words, [history] the language's free games, oldest first; [random]
     * is injected, so a test draws the same word twice.
     */
    fun draw(level: List<Word>, history: List<Played>, random: Random): Word {
        require(level.isNotEmpty()) { "a level with no word" }
        val recent = history.takeLast(RECENT).mapTo(HashSet()) { it.solution }
        val solved = history.filter { it.solved }.mapTo(HashSet()) { it.solution }
        val fresh = level.filter { it !in recent && it !in solved }
        if (fresh.isNotEmpty()) return fresh[random.nextInt(fresh.size)]
        val lastPlayed = HashMap<Word, Int>()
        history.forEachIndexed { i, played -> lastPlayed[played.solution] = i }
        // A word of the level never played, if any, is the oldest of all.
        return level.minBy { lastPlayed[it] ?: -1 }
    }
}
