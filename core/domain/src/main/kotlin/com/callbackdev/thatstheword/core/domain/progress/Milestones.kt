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

    fun reached(facts: Facts): Set<Milestone> = Milestone.entries.filterTo(LinkedHashSet()) { holds(it, facts) }

    fun newlyReached(facts: Facts, unlocked: Set<Milestone>): Set<Milestone> = reached(facts) - unlocked

    private fun holds(milestone: Milestone, facts: Facts): Boolean = when (milestone) {
        Milestone.SEVEN_DAILIES_IN_A_ROW ->
            Language.entries.any { Streaks.longest(facts.games, it) >= STREAK }

        Milestone.FIRST_FOREIGN_WIN ->
            facts.games.any { it.counted && it.state == GameState.WON && it.language != it.interfaceLanguage }

        Milestone.HUNDRED_WORDS -> facts.notebookSize >= NOTEBOOK

        Milestone.ONE_DAY_THREE_LANGUAGES ->
            facts.games
                .filter { it.counted && it.state == GameState.WON && it.mode is GameMode.Daily }
                .groupBy({ (it.mode as GameMode.Daily).date }, { it.language })
                .values.any { it.toSet().size == Language.entries.size }
    }
}
