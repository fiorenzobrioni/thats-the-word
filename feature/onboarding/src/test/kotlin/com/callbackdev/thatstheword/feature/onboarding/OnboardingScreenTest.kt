package com.callbackdev.thatstheword.feature.onboarding

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.data.db.ThatsTheWordDatabase
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import com.callbackdev.thatstheword.core.testing.assertAccessible
import com.callbackdev.thatstheword.core.testing.walkPage
import com.callbackdev.thatstheword.core.testing.writeScreenshot
import com.callbackdev.thatstheword.core.words.AssetWordFiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock

/**
 * The first run drawn from its pages (PLANNING.md §12): the welcome, the language, the level of a
 * foreign one; in the three languages, dark, at twice the text size and on an open foldable; and
 * its end, which writes the choices and starts the guided game.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    @get:Rule val folder = TemporaryFolder()

    private val picked = mutableListOf<Any>()
    private val finished = mutableListOf<Boolean>()
    private var nexts = 0

    private fun draw(
        step: OnboardingStep,
        speaks: Language = Language.ENGLISH,
        language: Language = speaks,
        level: Level = Level.BASE,
        dark: Boolean = false,
    ) {
        compose.setContent {
            ThatsTheWordTheme(darkTheme = dark) {
                OnboardingScreen(
                    step = step,
                    speaks = speaks,
                    language = language,
                    level = level,
                    actions = OnboardingActions(
                        next = { nexts++ },
                        language = { picked += it },
                        level = { picked += it },
                        finish = { finished += it },
                    ),
                )
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
    }

    @Test
    fun `the welcome says the name, the word set down, and three promises`() {
        draw(OnboardingStep.WELCOME)
        compose.onNodeWithContentDescription("That’s The Word").assertIsDisplayed()
        compose.onNodeWithText("A language to practise").assertIsDisplayed()
        compose.onNodeWithText("No account, no ads. That’s The Word sends nothing.").assertIsDisplayed()
        compose.onNodeWithText("Begin").performClick()
        assertThat(nexts).isEqualTo(1)
        compose.assertAccessible()
        compose.writeScreenshot("onboarding-welcome")
    }

    @Test
    fun `the player's own language comes first, and ends the run with the first game or without`() {
        draw(OnboardingStep.LANGUAGE)
        compose.onNodeWithTag(OnboardingTags.language(Language.ENGLISH)).assertIsSelected()
        compose.onNodeWithText("Your language: the classic game.").assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTags.language(Language.SPANISH)).performClick()
        assertThat(picked).containsExactly(Language.SPANISH)
        compose.onNodeWithText("Play the first game").performClick()
        compose.onNodeWithText("I know how to play").performClick()
        assertThat(finished).containsExactly(true, false).inOrder()
        compose.assertAccessible()
        compose.writeScreenshot("onboarding-language")
    }

    @Test
    fun `a foreign language goes on to its level`() {
        draw(OnboardingStep.LANGUAGE, language = Language.ITALIAN)
        compose.onNodeWithText("Continue").performClick()
        assertThat(nexts).isEqualTo(1)
        compose.onNodeWithText("I know how to play").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the level of a foreign language, in Italian`() {
        draw(OnboardingStep.LEVEL, speaks = Language.ITALIAN, language = Language.SPANISH)
        compose.onNodeWithText(
            "In spagnolo le parole sono ordinate da quanto sono comuni. Base è il punto da cui partire.",
        ).assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTags.level(Level.BASE)).assertIsSelected()
        compose.onNodeWithTag(OnboardingTags.level(Level.ADVANCED)).performClick()
        assertThat(picked).containsExactly(Level.ADVANCED)
        compose.assertAccessible()
        compose.writeScreenshot("onboarding-level-it")
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
    fun `the welcome in Spanish, dark`() {
        draw(OnboardingStep.WELCOME, speaks = Language.SPANISH, dark = true)
        compose.onNodeWithText("Un idioma para practicar").assertIsDisplayed()
        compose.writeScreenshot("onboarding-welcome-es-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size the pages scroll`() {
        draw(OnboardingStep.WELCOME)
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw(OnboardingStep.LANGUAGE, language = Language.ITALIAN)
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-foldable")
    }

    @Test
    fun `the run's end writes the choices and starts the guided game, or not`() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database =
            Room.inMemoryDatabaseBuilder(context, ThatsTheWordDatabase::class.java).allowMainThreadQueries().build()
        try {
            val settings = SettingsRepository(
                PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("settings.preferences_pb") },
            ) { Language.ENGLISH }
            val games = GameRepository(database, Lexicons(AssetWordFiles(context.assets))) { Clock.systemUTC() }
            val model = OnboardingViewModel(settings, games) { Language.ENGLISH }
            var opened: Long? = -1L
            model.finish(Language.SPANISH, Level.INTERMEDIATE, guided = true) { opened = it }
            runBlocking { withTimeout(10_000) { while (opened == -1L) kotlinx.coroutines.delay(10) } }
            val current = runBlocking { settings.current() }
            assertThat(current.gameLanguage).isEqualTo(Language.SPANISH)
            assertThat(current.levelOf(Language.SPANISH)).isEqualTo(Level.INTERMEDIATE)
            assertThat(current.firstRunDone).isTrue()
            val id = checkNotNull(opened)
            assertThat(runBlocking { games.load(id) }?.game?.setup?.mode).isEqualTo(GameMode.Guided)

            val again = OnboardingViewModel(settings, games) { Language.ENGLISH }
            var skipped: Long? = -1L
            again.finish(Language.ENGLISH, null, guided = false) { skipped = it }
            runBlocking { withTimeout(10_000) { while (skipped == -1L) kotlinx.coroutines.delay(10) } }
            assertThat(skipped).isNull()
        } finally {
            database.close()
            scope.cancel()
            Dispatchers.resetMain()
        }
    }
}
