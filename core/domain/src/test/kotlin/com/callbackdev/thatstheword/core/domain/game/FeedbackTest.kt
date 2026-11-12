package com.callbackdev.thatstheword.core.domain.game

import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.model.Mark
import com.callbackdev.thatstheword.core.model.Mark.ABSENT
import com.callbackdev.thatstheword.core.model.Mark.ELSEWHERE
import com.callbackdev.thatstheword.core.model.Mark.RIGHT_PLACE
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedbackTest {
    private fun marks(solution: String, guess: String): List<Mark> = Feedback.marks(word(solution), word(guess))

    @Test
    fun `every letter in its right place`() {
        assertThat(marks("crane", "crane")).containsExactly(
            RIGHT_PLACE,
            RIGHT_PLACE,
            RIGHT_PLACE,
            RIGHT_PLACE,
            RIGHT_PLACE,
        )
    }

    @Test
    fun `letters elsewhere and absent`() {
        assertThat(marks("crane", "nacho")).containsExactly(ELSEWHERE, ELSEWHERE, ELSEWHERE, ABSENT, ABSENT).inOrder()
    }

    @Test
    fun `a letter twice in the guess, once in the solution, one in place - that one in place, the other absent`() {
        // HELLO has one E, at 1; LEVEE has its E at 1, 3 and 4.
        assertThat(marks("hello", "levee")).containsExactly(ELSEWHERE, RIGHT_PLACE, ABSENT, ABSENT, ABSENT).inOrder()
    }

    @Test
    fun `a letter twice in the guess, once in the solution, neither in place - the leftmost elsewhere`() {
        // CRANE has one E, at 4; EMBED has two, at 0 and 3.
        assertThat(marks("crane", "embed")).containsExactly(ELSEWHERE, ABSENT, ABSENT, ABSENT, ABSENT).inOrder()
    }

    @Test
    fun `the solution with a letter twice, the guess with it once, out of place - elsewhere`() {
        // SPEED has two E (2 and 3); TEACH has one, at 1.
        assertThat(marks("speed", "teach")).containsExactly(ABSENT, ELSEWHERE, ABSENT, ABSENT, ABSENT).inOrder()
    }

    @Test
    fun `the solution and the guess with a letter twice - both found`() {
        assertThat(marks("speed", "eerie")).containsExactly(ELSEWHERE, ELSEWHERE, ABSENT, ABSENT, ABSENT).inOrder()
        assertThat(marks("speed", "steep"))
            .containsExactly(RIGHT_PLACE, ABSENT, RIGHT_PLACE, RIGHT_PLACE, ELSEWHERE).inOrder()
    }

    @Test
    fun `the keyboard keeps each letter's best mark`() {
        val first = Feedback.guess(word("crane"), word("nacho"))
        val second = Feedback.guess(word("crane"), word("crank"))
        val keys = Feedback.keyboard(listOf(first, second))
        // N was elsewhere in NACHO, then in its place in CRANK: the better mark stays.
        assertThat(keys[word("n")[0]]).isEqualTo(RIGHT_PLACE)
        assertThat(keys[word("k")[0]]).isEqualTo(ABSENT)
        assertThat(keys[word("c")[0]]).isEqualTo(RIGHT_PLACE)
        assertThat(keys[word("h")[0]]).isEqualTo(ABSENT)
        assertThat(keys[word("z")[0]]).isNull()
    }
}
