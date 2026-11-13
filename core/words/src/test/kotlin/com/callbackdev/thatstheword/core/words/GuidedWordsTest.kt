package com.callbackdev.thatstheword.core.words

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.domain.game.GuidedGame
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.domain.words.meaning
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The guided first game's words (PLANNING.md §11 Phase 7), against the committed files: each a
 * Base solution, accepted as a guess, and explained on the end card in every interface language.
 */
@RunWith(AndroidJUnit4::class)
class GuidedWordsTest {
    private val files = AssetWordFiles(ApplicationProvider.getApplicationContext<android.content.Context>().assets)

    @Test
    fun `each guided word is a Base solution, accepted, with a meaning in every interface language`() {
        Language.entries.forEach { language ->
            val lexicon = Lexicon(files, language)
            val solution = GuidedGame.solution(language)
            assertThat(lexicon.solutions.levelOf(solution)).isEqualTo(Level.BASE)
            assertThat(solution in lexicon.accepted).isTrue()
            Language.entries.forEach { speaks ->
                val meaning = lexicon.meaning(solution, speaks)
                assertThat(meaning.translations.isNotEmpty() || meaning.meaning != null).isTrue()
            }
        }
    }
}
