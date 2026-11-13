package com.callbackdev.thatstheword.feature.notebook

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.domain.words.Lexicon
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.NotebookEntry
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * The README's picture of the notebook (docs/screenshots): the Italian and Spanish words an English
 * speaker met in a fortnight of practice, each with its meaning read from the real words' files.
 * Run with `./gradlew :feature:notebook:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("thatstheword.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val files = AssetWordFiles(context.assets)
    private val today = LocalDate.of(2026, 11, 13)

    private fun entry(
        language: Language,
        key: String,
        daysAgo: Long,
        solved: Int = 1,
        hints: Int = 0,
        byHand: Boolean = false,
    ) = NotebookEntry(
        language = language,
        word = Alphabet.of(language).parse(key)!!,
        firstMet = today.minusDays(daysAgo),
        lastMet = today.minusDays(daysAgo),
        timesMet = 1,
        solved = solved,
        missed = if (solved == 0 && !byHand) 1 else 0,
        withHints = hints,
        savedByHand = byHand,
    )

    private val entries = listOf(
        entry(Language.ITALIAN, "citta", 0),
        entry(Language.ITALIAN, "torta", 0, solved = 0, byHand = true),
        entry(Language.SPANISH, "arbol", 2, hints = 1),
        entry(Language.ITALIAN, "fiume", 3),
        entry(Language.SPANISH, "nubes", 5, solved = 0),
        entry(Language.ITALIAN, "porta", 6),
        entry(Language.SPANISH, "playa", 9),
    )

    @Test
    fun notebook() {
        val words = entries.map { NotebookWordUi.of(it, Lexicon(files, it.language), Language.ENGLISH) }
        val state = NotebookUiState.of(words, "", null)
        compose.setContent {
            ThatsTheWordTheme {
                NotebookScreen(state = state, onBack = {}, onQuery = {}, onLanguage = {})
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "notebook.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
