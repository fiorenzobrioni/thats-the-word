package com.callbackdev.thatstheword.core.domain.notebook

import com.callbackdev.thatstheword.core.domain.progress.GameRecord
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.NotebookEntry
import com.callbackdev.thatstheword.core.model.Word
import java.text.Normalizer
import java.time.LocalDate

/**
 * The notebook's rules (VISION §9, PLANNING.md §5): the words met, one entry a word and language.
 * It gathers the solutions by themselves, and a word tried only when the player adds it from the
 * end card (the owner, PLANNING §15, Open 8). Its meanings are read from the words' files, never
 * copied, so an entry is only what happened to the word.
 */
object Notebook {
    /**
     * The solution's entry after a game finished on [date]: met once more, found or missed, with
     * hints or without. A replay as practice leaves the notebook as it was (PLANNING.md §4.6), as
     * do the guided first game, a lesson, and a game not finished: null then.
     */
    fun afterGame(entry: NotebookEntry?, record: GameRecord, date: LocalDate): NotebookEntry? {
        if (!record.counted) return null
        val won = record.state == GameState.WON
        val hinted = record.hints > 0
        val base = entry ?: NotebookEntry(
            language = record.language,
            word = record.solution,
            firstMet = date,
            lastMet = date,
            timesMet = 0,
            solved = 0,
            missed = 0,
            withHints = 0,
            savedByHand = false,
        )
        return base.copy(
            lastMet = maxOf(base.lastMet, date),
            timesMet = base.timesMet + 1,
            solved = base.solved + if (won) 1 else 0,
            missed = base.missed + if (won) 0 else 1,
            withHints = base.withHints + if (hinted) 1 else 0,
        )
    }

    /**
     * A word tried, added by hand from the end card on [date]. A word already in the notebook keeps
     * what it has, and is marked as kept by hand too.
     */
    fun savedByHand(entry: NotebookEntry?, language: Language, word: Word, date: LocalDate): NotebookEntry =
        entry?.copy(savedByHand = true) ?: NotebookEntry(
            language = language,
            word = word,
            firstMet = date,
            lastMet = date,
            timesMet = 1,
            solved = 0,
            missed = 0,
            withHints = 0,
            savedByHand = true,
        )

    /**
     * What taking a word back from the end card leaves: nothing, for a word that is there only
     * because it was added (a touch undone); the entry without its mark, for a word some game hid.
     */
    fun unsaved(entry: NotebookEntry): NotebookEntry? =
        if (entry.solved + entry.missed == 0) null else entry.copy(savedByHand = false)

    /**
     * Whether a word matches what was typed in the notebook's search: any of [texts] (its written
     * forms, its translations) containing the query, both folded, so *citta* finds *città* and
     * *arbol* finds *árbol*. An empty query matches everything.
     */
    fun matches(query: String, texts: List<String>): Boolean {
        val wanted = fold(query)
        return wanted.isEmpty() || texts.any { fold(it).contains(wanted) }
    }

    /** Lower case, the marks dropped (the ñ's tilde too: a search forgives, a guess does not). */
    fun fold(text: String): String = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
}
