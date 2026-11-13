package com.callbackdev.thatstheword.core.domain.game

import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.domain.wordsOf
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.GameState
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.model.Mark
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class GameTest {
    private val words = wordsOf(
        "crane", "crank", "nacho", "trace", "react", "stale", "slate", "speed", "spend", "steep",
        "eerie", "sweep", "spree", "upset", "pause", "tears", "start", "smart",
    )

    private fun setup(
        solution: String = "crane",
        hard: Boolean = false,
        interfaceLanguage: Language = Language.ITALIAN,
        hintKinds: Set<HintKind> = HintKind.entries.toSet(),
    ) = GameSetup(
        language = Language.ENGLISH,
        mode = GameMode.Free(Level.BASE),
        solution = word(solution),
        hard = hard,
        interfaceLanguage = interfaceLanguage,
        hintKinds = hintKinds,
    )

    private fun Game.play(vararg guesses: String): Game = guesses.fold(this) { game, guess ->
        when (val s = game.submit(word(guess), words)) {
            is Submission.Accepted -> s.game
            is Submission.Refused -> throw AssertionError("$guess refused: ${s.reason}")
        }
    }

    private fun Game.refusal(guess: String): Refusal? = (submit(word(guess), words) as? Submission.Refused)?.reason

    @Test
    fun `a guess is marked and costs an attempt`() {
        val game = Game.start(setup()).play("nacho")
        assertThat(game.guesses.single().marks)
            .containsExactly(Mark.ELSEWHERE, Mark.ELSEWHERE, Mark.ELSEWHERE, Mark.ABSENT, Mark.ABSENT).inOrder()
        assertThat(game.attemptsLeft).isEqualTo(5)
        assertThat(game.state).isEqualTo(GameState.PLAYING)
    }

    @Test
    fun `a word too short is refused, and costs nothing`() {
        val game = Game.start(setup())
        assertThat(game.submit(word("cran"), words)).isEqualTo(Submission.Refused(Refusal.TooShort))
        assertThat(game.attemptsLeft).isEqualTo(6)
    }

    @Test
    fun `a word not in the list is refused, and costs nothing`() {
        val game = Game.start(setup())
        assertThat(game.refusal("xxxxx")).isEqualTo(Refusal.NotAWord)
        assertThat(game.attemptsLeft).isEqualTo(6)
    }

    @Test
    fun `won when every letter is in its right place`() {
        val game = Game.start(setup()).play("nacho", "crane")
        assertThat(game.state).isEqualTo(GameState.WON)
        assertThat(game.attemptsUsed).isEqualTo(2)
    }

    @Test
    fun `lost when no attempt is left, and a finished game takes nothing more`() {
        val game = Game.start(setup()).play("nacho", "trace", "react", "stale", "slate", "crank")
        assertThat(game.state).isEqualTo(GameState.LOST)
        assertThat(game.refusal("crane")).isEqualTo(Refusal.Finished)
        assertThat(game.hintsOffered).isEmpty()
    }

    @Test
    fun `a game taken up from its moves is the same game`() {
        val played = Game.start(setup()).play("nacho").takeHint(HintKind.LETTER).play("trace")
        val restored = Game.restore(setup(), played.moves)
        assertThat(restored.guesses).isEqualTo(played.guesses)
        assertThat(restored.hints).isEqualTo(played.hints)
        assertThat(restored.attemptsLeft).isEqualTo(3)
    }

    // A free word left for another (PLANNING.md §4.7).

    @Test
    fun `a free word left is lost at once, and takes nothing more`() {
        val left = Game.start(setup()).play("nacho", "trace").concede()
        assertThat(left.state).isEqualTo(GameState.LOST)
        assertThat(left.attemptsUsed).isEqualTo(2)
        assertThat(left.refusal("crane")).isEqualTo(Refusal.Finished)
        assertThat(left.hintsOffered).isEmpty()
    }

    @Test
    fun `a word left is taken up left, and its moves still say the rest`() {
        val left = Game.start(setup()).play("nacho").concede()
        val restored = Game.restore(setup(), left.moves, conceded = true)
        assertThat(restored.state).isEqualTo(GameState.LOST)
        assertThat(restored.conceded).isTrue()
        assertThat(restored.guesses).isEqualTo(left.guesses)
        // A word won or lost by its moves is that, whatever was said of it.
        val won = Game.start(setup()).play("nacho", "crane")
        assertThat(Game.restore(setup(), won.moves, conceded = true).state).isEqualTo(GameState.WON)
    }

    @Test
    fun `only a free word under way and tried is left`() {
        assertThrows(IllegalStateException::class.java) { Game.start(setup()).concede() }
        val daily = setup().copy(mode = GameMode.Daily(java.time.LocalDate.of(2026, 11, 13), 13))
        assertThrows(IllegalStateException::class.java) { Game.start(daily).play("nacho").concede() }
        val won = Game.start(setup()).play("crane")
        assertThrows(IllegalStateException::class.java) { won.concede() }
    }

    // Hard (PLANNING.md §4.4).

    @Test
    fun `Hard - a right-place letter moved is refused, naming the position`() {
        // TRACE against CRANE: R and A in their places (1 and 2), C and E elsewhere.
        val game = Game.start(setup(hard = true)).play("trace")
        val refusal = game.refusal("stale")
        assertThat(refusal).isEqualTo(Refusal.MustBeAt(1, word("r")[0]))
    }

    @Test
    fun `Hard - an elsewhere letter twice, guessed once, is refused, naming the letter`() {
        // SPEED has two E; EERIE marks two of its E elsewhere.
        val game = Game.start(setup(solution = "speed", hard = true)).play("eerie")
        // SWEEP keeps two E. SPEND has one: refused for the letter E, twice.
        assertThat(game.refusal("sweep")).isNull()
        assertThat(game.refusal("spend")).isEqualTo(Refusal.MustContain(word("e")[0], 2))
    }

    @Test
    fun `Hard holds every clue, not only the last guess's`() {
        val game = Game.start(setup(hard = true)).play("nacho")
        // NACHO found N, A and C, elsewhere: TRACE leaves out the N.
        assertThat(game.refusal("trace")).isEqualTo(Refusal.MustContain(word("n")[0], 1))
        assertThat(game.refusal("crank")).isNull()
    }

    @Test
    fun `Hard and the letter hint - the revealed letter is required in its place`() {
        val game = Game.start(setup(hard = true)).takeHint(HintKind.LETTER)
        assertThat(game.revealed).isEqualTo(0)
        assertThat(game.refusal("trace")).isEqualTo(Refusal.MustBeAt(0, word("c")[0]))
        assertThat(game.refusal("crank")).isNull()
    }

    @Test
    fun `without Hard any word of the list goes`() {
        val game = Game.start(setup()).play("trace")
        assertThat(game.refusal("stale")).isNull()
    }

    @Test
    fun `Hard is chosen before the first guess`() {
        val game = Game.start(setup())
        assertThat(game.withHard(true).setup.hard).isTrue()
        assertThrows(IllegalStateException::class.java) { game.play("nacho").withHard(true) }
    }

    // Hints (PLANNING.md §4.5, the owner, PLANNING §15, Open 11).

    @Test
    fun `a hint costs one attempt`() {
        val game = Game.start(setup()).takeHint(HintKind.TRANSLATION)
        assertThat(game.attemptsLeft).isEqualTo(5)
        assertThat(game.hints.single().kind).isEqualTo(HintKind.TRANSLATION)
    }

    @Test
    fun `each hint once a game`() {
        val game = Game.start(setup()).takeHint(HintKind.DEFINITION)
        assertThat(game.hintsOffered).containsExactly(HintKind.TRANSLATION, HintKind.LETTER)
        assertThrows(IllegalArgumentException::class.java) { game.takeHint(HintKind.DEFINITION) }
    }

    @Test
    fun `no hint in the interface's own language`() {
        val game = Game.start(setup(interfaceLanguage = Language.ENGLISH))
        assertThat(game.hintsOffered).isEmpty()
    }

    @Test
    fun `a word with no translation or definition offers no such hint`() {
        val game = Game.start(setup(hintKinds = setOf(HintKind.LETTER)))
        assertThat(game.hintsOffered).containsExactly(HintKind.LETTER)
    }

    @Test
    fun `a hint with one attempt left is not offered`() {
        val game = Game.start(setup()).play("nacho", "trace", "react", "stale", "slate")
        assertThat(game.attemptsLeft).isEqualTo(1)
        assertThat(game.hintsOffered).isEmpty()
    }

    @Test
    fun `three hints, then three wrong guesses - lost, six attempts used`() {
        val game = Game.start(setup())
            .takeHint(HintKind.TRANSLATION)
            .takeHint(HintKind.DEFINITION)
            .takeHint(HintKind.LETTER)
            .play("nacho", "trace", "react")
        assertThat(game.state).isEqualTo(GameState.LOST)
        assertThat(game.attemptsUsed).isEqualTo(6)
    }

    @Test
    fun `the letter hint reveals the leftmost position not yet found`() {
        // TRACE finds R and A (1 and 2) in place against CRANE: C, at 0, is the leftmost left.
        assertThat(Game.start(setup()).play("trace").takeHint(HintKind.LETTER).revealed).isEqualTo(0)
        // CRANK finds C, R, A and N: E, at 4.
        assertThat(Game.start(setup()).play("crank").takeHint(HintKind.LETTER).revealed).isEqualTo(4)
    }

    @Test
    fun `the letter hint is not offered when every position is found, though not in one guess`() {
        // SPEED: STEEP finds S, E, E (0, 2, 3); SPEND finds S, P, E and D (0, 1, 2, 4).
        val game = Game.start(setup(solution = "speed")).play("steep", "spend")
        assertThat(game.state).isEqualTo(GameState.PLAYING)
        assertThat(game.hintsOffered).doesNotContain(HintKind.LETTER)
    }

    @Test
    fun `the keyboard shows each letter's best mark, not a hint's`() {
        val game = Game.start(setup()).takeHint(HintKind.LETTER).play("nacho")
        assertThat(game.keyboard[word("c")[0]]).isEqualTo(Mark.ELSEWHERE)
    }
}
