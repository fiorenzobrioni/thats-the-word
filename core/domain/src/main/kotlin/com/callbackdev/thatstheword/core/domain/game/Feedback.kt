package com.callbackdev.thatstheword.core.domain.game

import com.callbackdev.thatstheword.core.model.Guess
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.Word

/**
 * The marks (PLANNING.md §4.2), in two passes, duplicates counted: every letter in its right place
 * first, each taken off the solution's count of that letter; then, left to right, every other
 * letter elsewhere while the solution still has an unclaimed copy of it, absent otherwise. So of
 * two E against a solution with one, the right-placed one is marked, else the leftmost.
 */
object Feedback {
    fun marks(solution: Word, guess: Word): List<Mark> {
        require(solution.length == guess.length) { "a guess is as long as its solution" }
        val marks = Array(guess.length) { Mark.ABSENT }
        val unclaimed = HashMap<Letter, Int>()
        for (i in 0 until solution.length) {
            if (guess[i] == solution[i]) {
                marks[i] = Mark.RIGHT_PLACE
            } else {
                unclaimed.merge(solution[i], 1, Int::plus)
            }
        }
        for (i in 0 until guess.length) {
            if (marks[i] == Mark.RIGHT_PLACE) continue
            val left = unclaimed[guess[i]] ?: 0
            if (left > 0) {
                marks[i] = Mark.ELSEWHERE
                unclaimed[guess[i]] = left - 1
            }
        }
        return marks.asList()
    }

    fun guess(solution: Word, word: Word): Guess = Guess(word, marks(solution, word))

    /**
     * Each letter's best mark so far, as the keyboard shows it (right place over elsewhere over
     * absent); a letter not yet tried is not in the map.
     */
    fun keyboard(guesses: List<Guess>): Map<Letter, Mark> {
        val best = HashMap<Letter, Mark>()
        for (guess in guesses) {
            for (i in 0 until guess.word.length) {
                val letter = guess.word[i]
                val mark = guess.marks[i]
                val current = best[letter]
                if (current == null || mark > current) best[letter] = mark
            }
        }
        return best
    }
}
