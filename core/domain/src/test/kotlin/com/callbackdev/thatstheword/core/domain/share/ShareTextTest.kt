package com.callbackdev.thatstheword.core.domain.share

import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.domain.word
import com.callbackdev.thatstheword.core.domain.wordsOf
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.GameSetup
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ShareTextTest {
    private val words = wordsOf("crane", "nacho", "trace", "react", "stale", "slate", "crank")
    private val labels = ShareText.Labels(
        appName = "That’s The Word",
        languageName = "English",
        modeName = "Free play",
        hard = "Hard",
        hints = { if (it == 1) "1 hint" else "$it hints" },
    )

    private fun game(mode: GameMode = GameMode.Daily(LocalDate.of(2027, 3, 14), 123), hard: Boolean = false) =
        Game.start(
            GameSetup(
                language = Language.ENGLISH,
                mode = mode,
                solution = word("crane"),
                hard = hard,
                interfaceLanguage = Language.ITALIAN,
                hintKinds = HintKind.entries.toSet(),
            ),
        )

    private fun Game.play(vararg guesses: String) =
        guesses.fold(this) { g, w -> (g.submit(word(w), words) as Submission.Accepted).game }

    @Test
    fun `a daily won, without the word`() {
        val text = ShareText.of(game().play("nacho", "trace", "crane"), labels)
        assertThat(text).isEqualTo(
            """
            That’s The Word · #123 · English · 3/6

            🟠🟠🟠▫️▫️
            ▫️🟦🟦🟠🟦
            🟦🟦🟦🟦🟦
            """.trimIndent(),
        )
        assertThat(text.lowercase()).doesNotContain("crane")
    }

    @Test
    fun `Hard and the hints are said, and a hint's attempt is in the score`() {
        val text = ShareText.of(game(hard = true).takeHint(HintKind.TRANSLATION).play("crank", "crane"), labels)
        assertThat(
            text.lines().take(2),
        ).containsExactly("That’s The Word · #123 · English · 3/6", "Hard · 1 hint").inOrder()
        assertThat(text.lines().drop(3)).hasSize(2)
    }

    @Test
    fun `a game lost says X`() {
        val text = ShareText.of(
            game(GameMode.Free(Level.BASE)).play("nacho", "trace", "react", "stale", "slate", "crank"),
            labels,
        )
        assertThat(text.lines().first()).isEqualTo("That’s The Word · Free play · English · X/6")
    }

    @Test
    fun `the marks keep their shapes - a square here, a circle elsewhere`() {
        assertThat(ShareText.RIGHT_PLACE).isEqualTo("🟦")
        assertThat(ShareText.ELSEWHERE).isEqualTo("🟠")
        assertThat(ShareText.ABSENT).isEqualTo("▫️")
    }
}
