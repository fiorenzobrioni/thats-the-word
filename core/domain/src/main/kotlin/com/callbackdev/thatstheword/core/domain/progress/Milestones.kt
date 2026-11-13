package com.callbackdev.thatstheword.core.domain.progress

import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Milestone

/**
 * The milestones' predicates (PLANNING.md §4.8, VISION §11): each a question to the facts, the
 * games and the notebook, asked after every finished game and every notebook change. A milestone
 * is unlocked once, with its date, and never taken back: [newlyReached] leaves out those already
 * unlocked, so a fact that changes later takes nothing away.
 */
object Milestones {
    /** What the predicates read. */
    data class Facts(val games: List<GameRecord>, val notebookSize: Int)

    const val STREAK = 7
    const val NOTEBOOK = 100

    /**
     * How near a milestone is (VISION §11): the facts' best so far against what it asks, never
     * past it. The longest streak in any language against seven; the words in the notebook against
     * a hundred; the most languages whose daily was won on one date against three; a win in a
     * foreign language, one or none. A recognition said, never a key: nothing waits on it.
     */
    fun progress(facts: Facts): Map<Milestone, Progress> = Milestone.entries.associateWith { milestone ->
        when (milestone) {
            Milestone.SEVEN_DAILIES_IN_A_ROW ->
                Progress(Language.entries.maxOf { Streaks.longest(facts.games, it) }, STREAK)

            Milestone.FIRST_FOREIGN_WIN ->
                Progress(if (holds(milestone, facts)) 1 else 0, 1)

            Milestone.HUNDRED_WORDS -> Progress(facts.notebookSize, NOTEBOOK)

            Milestone.ONE_DAY_THREE_LANGUAGES -> Progress(
                dailyWins(facts).values.maxOfOrNull { it.size } ?: 0,
                Language.entries.size,
            )
        }
    }

    /** How far a milestone is: [reached] of [target], [reached] held to the target. */
    data class Progress(private val count: Int, val target: Int) {
        val reached: Int get() = count.coerceIn(0, target)

        val done: Boolean get() = count >= target
    }

    fun reached(facts: Facts): Set<Milestone> = Milestone.entries.filterTo(LinkedHashSet()) { holds(it, facts) }

    fun newlyReached(facts: Facts, unlocked: Set<Milestone>): Set<Milestone> = reached(facts) - unlocked

    private fun holds(milestone: Milestone, facts: Facts): Boolean = when (milestone) {
        Milestone.SEVEN_DAILIES_IN_A_ROW ->
            Language.entries.any { Streaks.longest(facts.games, it) >= STREAK }

        Milestone.FIRST_FOREIGN_WIN ->
            facts.games.any { it.counted && it.state == GameState.WON && it.language != it.interfaceLanguage }

        Milestone.HUNDRED_WORDS -> facts.notebookSize >= NOTEBOOK

        Milestone.ONE_DAY_THREE_LANGUAGES ->
            dailyWins(facts).values.any { it.size == Language.entries.size }
    }

    /** Each date's languages whose daily was won, its counted result, from the archive too. */
    private fun dailyWins(facts: Facts) = facts.games
        .filter { it.counted && it.state == GameState.WON && it.mode is GameMode.Daily }
        .groupBy({ (it.mode as GameMode.Daily).date }, { it.language })
        .mapValues { it.value.toSet() }
}
