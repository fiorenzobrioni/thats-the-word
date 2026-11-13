package com.callbackdev.thatstheword.feature.progress

import androidx.compose.runtime.Immutable
import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.domain.progress.LanguageProgress
import com.callbackdev.thatstheword.core.domain.progress.Milestones
import com.callbackdev.thatstheword.core.domain.progress.ModeProgress
import com.callbackdev.thatstheword.core.domain.progress.ProgressMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Milestone
import java.time.LocalDate

/**
 * A milestone as the page shows it (VISION §11): the date it was reached, or how near it is.
 * Recognition, never a key: nothing waits on it.
 */
@Immutable
data class MilestoneUi(val milestone: Milestone, val reachedOn: LocalDate?, val reached: Int, val target: Int)

/**
 * The progress page, all of it, from the games, the notebook, the milestones and what the player
 * looks at (PLANNING.md §11 Phase 6): drawn from this alone, so a test draws any progress without a
 * database.
 *
 * @property languages the languages with a game counted, in the languages' order: the page offers
 *   them as a choice when there are two or more.
 * @property language the language shown.
 * @property modes the shown language's modes with a game counted: a choice when there are two.
 * @property mode the mode shown, one of [modes] (or the daily, when the language has none).
 * @property progress the shown language's statistics, streaks and latest games.
 * @property attempts the attempts a game has, so the distribution has a row for each.
 */
@Immutable
data class ProgressUiState(
    val today: LocalDate,
    val languages: List<Language>,
    val language: Language,
    val modes: List<ProgressMode>,
    val mode: ProgressMode,
    val progress: LanguageProgress,
    val milestones: List<MilestoneUi>,
    val attempts: Int = GameSetup.ATTEMPTS,
) {
    val shown: ModeProgress get() = progress.of(mode)

    /** Nothing counted in any language yet: the page says so, and shows only the milestones and your data. */
    val nothingPlayed: Boolean get() = languages.isEmpty()

    val milestonesReached: Int get() = milestones.count { it.reachedOn != null }

    companion object {
        /**
         * The page for [records] and the notebook's [notebookSize] on [today], the milestones
         * [unlocked] on their dates; [wanted] and [wantedMode] are what the player chose to look
         * at, if anything, else the [gameLanguage] and its daily.
         */
        fun of(
            records: List<GameRecord>,
            notebookSize: Int,
            unlocked: Map<Milestone, LocalDate>,
            today: LocalDate,
            gameLanguage: Language,
            wanted: Language?,
            wantedMode: ProgressMode?,
        ): ProgressUiState {
            val languages = LanguageProgress.played(records)
            val language = listOfNotNull(wanted, gameLanguage).firstOrNull { it in languages }
                ?: languages.firstOrNull()
                ?: gameLanguage
            val progress = LanguageProgress.of(records, language, today)
            val modes = ProgressMode.entries.filter { progress.of(it).statistics.played > 0 }
            val mode = wantedMode?.takeIf { it in modes } ?: modes.firstOrNull() ?: ProgressMode.DAILY
            val near = Milestones.progress(Milestones.Facts(records, notebookSize))
            return ProgressUiState(
                today = today,
                languages = languages,
                language = language,
                modes = modes,
                mode = mode,
                progress = progress,
                milestones = Milestone.entries.map { milestone ->
                    val p = near.getValue(milestone)
                    MilestoneUi(milestone, unlocked[milestone], p.reached, p.target)
                },
            )
        }
    }
}
