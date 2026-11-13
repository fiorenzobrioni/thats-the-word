package com.callbackdev.thatstheword.core.domain.game

import com.callbackdev.thatstheword.core.domain.key
import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.domain.wordsOf
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The guided first game's word and its lessons, each said once (PLANNING.md §11 Phase 7). */
class GuidedGameTest {
    private val words = wordsOf("heart", "earth", "pious", "lymph", "board", "tread", "hater", "eaten", "cigar")

    private fun start(interfaceLanguage: Language = Language.ENGLISH) = Game.start(
        GameSetup(
            language = Language.ENGLISH,
            mode = GameMode.Guided,
            solution = GuidedGame.solution(Language.ENGLISH),
            interfaceLanguage = interfaceLanguage,
            hintKinds = if (interfaceLanguage == Language.ENGLISH) emptySet() else HintKind.entries.toSet(),
        ),
    )

    private fun Game.play(guess: String): Game = (submit(word(guess), words) as Submission.Accepted).game

    @Test
    fun `each language has its fixed word, five different letters`() {
        assertThat(GuidedGame.solution(Language.ENGLISH).key()).isEqualTo("heart")
        assertThat(GuidedGame.solution(Language.ITALIAN).key(Language.ITALIAN)).isEqualTo("cuore")
        assertThat(GuidedGame.solution(Language.SPANISH).key(Language.SPANISH)).isEqualTo("amigo")
        Language.entries.forEach { language ->
            val solution = GuidedGame.solution(language)
            assertThat(solution.length).isEqualTo(GameSetup.LENGTH)
            assertThat(solution.letters.toSet()).hasSize(GameSetup.LENGTH)
        }
    }

    @Test
    fun `the first guess is asked for once, before it is made`() {
        val game = start()
        assertThat(GuidedLessons.atStart(game, emptySet())).containsExactly(Lesson.FIRST_GUESS)
        assertThat(GuidedLessons.atStart(game, setOf(Lesson.FIRST_GUESS))).isEmpty()
        assertThat(GuidedLessons.atStart(game.play("pious"), emptySet())).isEmpty()
    }

    @Test
    fun `a refusal teaches its rule the first time only, and Hard's refusals teach nothing`() {
        assertThat(GuidedLessons.afterRefusal(Refusal.TooShort, emptySet())).containsExactly(Lesson.FIVE_LETTERS)
        assertThat(GuidedLessons.afterRefusal(Refusal.NotAWord, emptySet())).containsExactly(Lesson.ONLY_WORDS)
        assertThat(GuidedLessons.afterRefusal(Refusal.NotAWord, setOf(Lesson.ONLY_WORDS))).isEmpty()
        assertThat(GuidedLessons.afterRefusal(Refusal.Finished, emptySet())).isEmpty()
    }

    @Test
    fun `a row says each mark it shows for the first time, in the marks' order`() {
        // EARTH against HEART: every letter elsewhere.
        val first = start().play("earth")
        assertThat(GuidedLessons.afterGuess(first, setOf(Lesson.FIRST_GUESS))).containsExactly(Lesson.ELSEWHERE)
        // BOARD: A and R in their places, B, O and D absent.
        val second = first.play("board")
        val said = setOf(Lesson.FIRST_GUESS, Lesson.ELSEWHERE)
        assertThat(GuidedLessons.afterGuess(second, said))
            .containsExactly(Lesson.RIGHT_PLACE, Lesson.ABSENT).inOrder()
    }

    @Test
    fun `a row with nothing new says the keyboard, then the hints in a foreign game, then nothing`() {
        val marks = setOf(Lesson.FIRST_GUESS, Lesson.RIGHT_PLACE, Lesson.ELSEWHERE, Lesson.ABSENT)
        // The first row has nothing new to say: the keys have just changed under the player's eyes.
        val one = start(Language.ITALIAN).play("pious")
        assertThat(GuidedLessons.afterGuess(one, marks)).isEmpty()
        val two = one.play("lymph")
        assertThat(GuidedLessons.afterGuess(two, marks)).containsExactly(Lesson.KEYBOARD)
        val three = two.play("cigar")
        assertThat(GuidedLessons.afterGuess(three, marks + Lesson.KEYBOARD)).containsExactly(Lesson.HINTS)
        assertThat(GuidedLessons.afterGuess(three, marks + Lesson.KEYBOARD + Lesson.HINTS)).isEmpty()
    }

    @Test
    fun `a game in its own language never says the hints`() {
        val marks = setOf(Lesson.FIRST_GUESS, Lesson.RIGHT_PLACE, Lesson.ELSEWHERE, Lesson.ABSENT, Lesson.KEYBOARD)
        val game = start().play("pious").play("lymph").play("cigar")
        assertThat(GuidedLessons.afterGuess(game, marks)).isEmpty()
    }

    @Test
    fun `a word found says nothing more, the end card speaks`() {
        assertThat(GuidedLessons.afterGuess(start().play("heart"), emptySet())).isEmpty()
    }

    @Test
    fun `a game taken up has taught what its rows showed`() {
        assertThat(GuidedLessons.taught(start())).isEmpty()
        val game = start().play("earth").play("board").play("cigar")
        assertThat(GuidedLessons.taught(game)).containsExactly(
            Lesson.FIRST_GUESS,
            Lesson.ELSEWHERE,
            Lesson.RIGHT_PLACE,
            Lesson.ABSENT,
            Lesson.KEYBOARD,
        )
    }
}
