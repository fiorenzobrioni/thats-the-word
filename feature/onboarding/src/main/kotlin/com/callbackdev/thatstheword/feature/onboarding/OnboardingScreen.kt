package com.callbackdev.thatstheword.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.thatstheword.core.designsystem.components.LetterTile
import com.callbackdev.thatstheword.core.designsystem.components.inkButtonColors
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordTheme
import com.callbackdev.thatstheword.core.designsystem.theme.balanced
import com.callbackdev.thatstheword.core.designsystem.theme.padding
import com.callbackdev.thatstheword.core.designsystem.theme.pageGutter
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.core.model.Level
import java.util.Locale

/** The first run's pages (PLANNING.md §11 Phase 7). The level's is shown only for a foreign language. */
enum class OnboardingStep {
    WELCOME,
    LANGUAGE,
    LEVEL,
}

/**
 * The first run. [onDone] hears the guided first game's id, to open over the start page, or null
 * when the player knows the game and goes straight to the start page.
 */
@Composable
fun OnboardingRoute(onDone: (Long?) -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<OnboardingViewModel>()
    val finishing by viewModel.isFinishing.collectAsStateWithLifecycle()
    val speaks = viewModel.speaks
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    var language by rememberSaveable { mutableStateOf(speaks) }
    var level by rememberSaveable { mutableStateOf(Level.BASE) }
    OnboardingScreen(
        step = step,
        speaks = speaks,
        language = language,
        level = level,
        finishing = finishing,
        actions = OnboardingActions(
            next = { step = nextStep(step, language, speaks) ?: step },
            back = { step = previousStep(step) },
            language = { language = it },
            level = { level = it },
            finish = { guided ->
                viewModel.finish(language, level.takeIf { language != speaks }, guided, onDone)
            },
        ),
        modifier = modifier,
    )
}

/** The page after [step]; null when the run ends there and the first game follows. */
internal fun nextStep(step: OnboardingStep, language: Language, speaks: Language): OnboardingStep? = when (step) {
    OnboardingStep.WELCOME -> OnboardingStep.LANGUAGE
    OnboardingStep.LANGUAGE -> if (language != speaks) OnboardingStep.LEVEL else null
    OnboardingStep.LEVEL -> null
}

private fun previousStep(step: OnboardingStep): OnboardingStep =
    OnboardingStep.entries.getOrElse(step.ordinal - 1) { OnboardingStep.WELCOME }

/** What the pages can ask for, as functions: the screen is a plain composable a test can draw. */
class OnboardingActions(
    val next: () -> Unit = {},
    val back: () -> Unit = {},
    val language: (Language) -> Unit = {},
    val level: (Level) -> Unit = {},
    /** The run's end: with the guided first game (true), or straight to the start page. */
    val finish: (guided: Boolean) -> Unit = {},
)

/**
 * The first run in the family's frame (PLANNING.md §11 Phase 7): no icon (the player has just
 * touched it to get here), a page's words centred in the room above one wide button that moves on,
 * always in the same place, with the quiet way past under it. The pages slide a little and fade;
 * a fade alone under reduced motion. Its last page starts the guided first game: the rules are
 * learned by playing, never from a page (VISION §12).
 */
@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    speaks: Language,
    language: Language,
    level: Level,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
    finishing: Boolean = false,
) {
    val reduced = reducedMotion()
    val gutter = pageGutter()
    val steps = if (language != speaks) OnboardingStep.entries.size else OnboardingStep.entries.size - 1
    BackHandler(enabled = step != OnboardingStep.WELCOME, onBack = actions.back)
    Surface(modifier = modifier.fillMaxSize().testTag(OnboardingTags.ROOT)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            StepTiles(current = step.ordinal, count = steps, modifier = Modifier.padding(top = 20.dp))
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (reduced) {
                        fadeIn(ThatsTheWordMotion.fade()) togetherWith fadeOut(ThatsTheWordMotion.fade())
                    } else {
                        val spec = tween<IntOffset>(PAGE_MILLIS, easing = FastOutSlowInEasing)
                        val shift = if (forward) SLIDE_DIVISOR else -SLIDE_DIVISOR
                        val enter = fadeIn(tween(PAGE_MILLIS)) + slideInHorizontally(spec) { it / shift }
                        val exit = fadeOut(tween(PAGE_MILLIS)) + slideOutHorizontally(spec) { -it / shift }
                        enter togetherWith exit
                    }
                },
                label = "onboarding",
                modifier = Modifier.weight(1f),
            ) { page ->
                // The page's words, centred in the room above the buttons; a page taller than the
                // room scrolls from its top.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val room = maxHeight
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = room)
                            .padding(gutter)
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                            .testTag(OnboardingTags.PAGE),
                    ) {
                        val where = stringResource(R.string.onboarding_progress, page.ordinal + 1, steps)
                        when (page) {
                            OnboardingStep.WELCOME -> WelcomePage(speaks, where)
                            OnboardingStep.LANGUAGE -> LanguagePage(speaks, language, actions.language, where)
                            OnboardingStep.LEVEL -> LevelPage(language, level, actions.level, where)
                        }
                    }
                }
            }
            Box(Modifier.padding(gutter)) { BottomBar(step, language, speaks, finishing, actions) }
        }
    }
}

private const val PAGE_MILLIS = 300
private const val SLIDE_DIVISOR = 6

/**
 * Where the run stands, drawn as the game draws a row: a small tile a page, those passed in the
 * right place's ink, the page shown framed, the rest empty. TalkBack hears it on each page's
 * heading instead ("Step 2 of 3").
 */
@Composable
private fun StepTiles(current: Int, count: Int, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { i ->
            val ground by animateColorAsState(
                when {
                    i < current -> colors.inverseSurface
                    i == current -> colors.surfaceContainerHighest
                    else -> Color.Transparent
                },
                ThatsTheWordMotion.effects(reduced),
                label = "step",
            )
            val frame by animateColorAsState(
                if (i == current) colors.onSurface else colors.outlineVariant,
                ThatsTheWordMotion.effects(reduced),
                label = "frame",
            )
            Box(
                Modifier
                    .size(12.dp)
                    .clip(StepShape)
                    .background(ground)
                    .border(1.5.dp, frame, StepShape),
            )
        }
    }
}

private val StepShape = RoundedCornerShape(3.dp)

/**
 * The buttons under every page: one wide button that moves on, in the same place on every page,
 * and on the run's last page the quiet way past the guided game, for a player who knows the genre.
 * The way back is the system's.
 */
@Composable
private fun BottomBar(
    step: OnboardingStep,
    language: Language,
    speaks: Language,
    finishing: Boolean,
    actions: OnboardingActions,
) {
    val last = nextStep(step, language, speaks) == null
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val label = when {
            step == OnboardingStep.WELCOME -> R.string.onboarding_begin
            last -> R.string.onboarding_play_first
            else -> R.string.onboarding_next
        }
        Button(
            onClick = if (last) ({ actions.finish(true) }) else actions.next,
            enabled = !finishing,
            colors = inkButtonColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(OnboardingTags.PRIMARY),
        ) {
            Text(stringResource(label), textAlign = TextAlign.Center)
        }
        if (last) {
            TextButton(
                onClick = { actions.finish(false) },
                enabled = !finishing,
                modifier = Modifier.heightIn(min = 48.dp).testTag(OnboardingTags.SKIP),
            ) {
                Text(stringResource(R.string.onboarding_skip), textAlign = TextAlign.Center)
            }
        } else {
            // The same height, so the wide button stays in its place from page to page.
            Spacer(Modifier.height(48.dp))
        }
    }
}

/** A page's heading, announced with where the run stands ([where]). */
private fun Modifier.stepHeading(where: String): Modifier = semantics {
    heading()
    stateDescription = where
}

@Composable
private fun PageTitle(text: String, where: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.stepHeading(where),
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PageBody(text: String) {
    Text(
        text = text,
        // Centred, so balanced: a centred paragraph with one word on its last line reads as a slip.
        style = MaterialTheme.typography.bodyLarge.balanced(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * The welcome: the name in its tracked capitals, the word "word" in the player's language set
 * down tile by tile on the game's spring (a fade under reduced motion), the line under the name,
 * and the three things the game promises, in words only.
 */
@Composable
private fun WelcomePage(speaks: Language, where: String) {
    val locale = LocalConfiguration.current.locales[0]
    val name = stringResource(R.string.onboarding_name)
    Text(
        text = name.uppercase(locale),
        style = ThatsTheWordTheme.type.wordmark,
        textAlign = TextAlign.Center,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = name
            heading()
            stateDescription = where
        },
    )
    Spacer(Modifier.height(28.dp))
    val word = stringResource(R.string.onboarding_word).uppercase(Locale.forLanguageTag(speaks.tag))
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics {},
    ) {
        word.forEachIndexed { i, c ->
            LetterTile(
                letter = c,
                size = WELCOME_TILE,
                appearDelayMillis =
                WELCOME_DELAY + i * ThatsTheWordMotion.TILE_STAGGER_MILLIS * 2,
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    val tagline = stringResource(R.string.onboarding_tagline)
    Text(
        text = tagline.uppercase(locale),
        style = ThatsTheWordTheme.type.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.clearAndSetSemantics { contentDescription = tagline },
    )
    Spacer(Modifier.height(36.dp))
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Promise(R.string.onboarding_promise_daily, R.string.onboarding_promise_daily_line)
        Promise(R.string.onboarding_promise_learn, R.string.onboarding_promise_learn_line)
        Promise(R.string.onboarding_promise_private, R.string.onboarding_promise_private_line)
    }
}

@Composable
private fun Promise(@StringRes title: Int, @StringRes line: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(line),
            style = MaterialTheme.typography.bodyMedium.balanced(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The game language: the three, the player's own first and named so ("your language: the classic
 * game"), each other one a language to practise, its hints and meanings in the player's.
 */
@Composable
private fun LanguagePage(speaks: Language, language: Language, onPick: (Language) -> Unit, where: String) {
    PageTitle(stringResource(R.string.onboarding_language_title), where)
    PageBody(stringResource(R.string.onboarding_language_body))
    Spacer(Modifier.height(28.dp))
    val order = listOf(speaks) + Language.entries.filter { it != speaks }
    Column(
        Modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        order.forEach { option ->
            val line = if (option == speaks) {
                stringResource(R.string.onboarding_language_own)
            } else {
                stringResource(R.string.onboarding_language_foreign, stringResource(languageInSentence(speaks)))
            }
            Choice(
                title = stringResource(languageName(option)),
                line = line,
                selected = option == language,
                onClick = { onPick(option) },
                tag = OnboardingTags.language(option),
            )
        }
    }
}

/** The level for a foreign language: Base first and chosen, each level with its line. */
@Composable
private fun LevelPage(language: Language, level: Level, onPick: (Level) -> Unit, where: String) {
    PageTitle(stringResource(R.string.onboarding_level_title), where)
    PageBody(stringResource(R.string.onboarding_level_body, stringResource(languageInSentence(language))))
    Spacer(Modifier.height(28.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Level.entries.forEach { option ->
            Choice(
                title = stringResource(levelName(option)),
                line = stringResource(levelLine(option)),
                selected = option == level,
                onClick = { onPick(option) },
                tag = OnboardingTags.level(option),
            )
        }
    }
}

/** A choice of the run: a card with its radio, framed in the dress's ink when it is the one. */
@Composable
private fun Choice(title: String, line: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    val reduced = reducedMotion()
    val colors = MaterialTheme.colorScheme
    val ground by animateColorAsState(
        if (selected) colors.surfaceContainerHighest else colors.surfaceContainerLow,
        ThatsTheWordMotion.effects(reduced),
        label = "ground",
    )
    val frame by animateColorAsState(
        if (selected) colors.onSurface else Color.Transparent,
        ThatsTheWordMotion.effects(reduced),
        label = "frame",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(ChoiceShape)
            .background(ground)
            .border(2.dp, frame, ChoiceShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(tag)
            .padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium.balanced(),
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(4.dp))
    }
}

private val ChoiceShape = RoundedCornerShape(20.dp)
private val WELCOME_TILE = 44.dp
private const val WELCOME_DELAY = 250L

@StringRes
private fun languageName(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.onboarding_language_it
    Language.ENGLISH -> R.string.onboarding_language_en
    Language.SPANISH -> R.string.onboarding_language_es
}

@StringRes
private fun languageInSentence(language: Language): Int = when (language) {
    Language.ITALIAN -> R.string.onboarding_language_it_in_sentence
    Language.ENGLISH -> R.string.onboarding_language_en_in_sentence
    Language.SPANISH -> R.string.onboarding_language_es_in_sentence
}

@StringRes
private fun levelName(level: Level): Int = when (level) {
    Level.BASE -> R.string.onboarding_level_base
    Level.INTERMEDIATE -> R.string.onboarding_level_intermediate
    Level.ADVANCED -> R.string.onboarding_level_advanced
    Level.COMPLETE -> R.string.onboarding_level_complete
}

@StringRes
private fun levelLine(level: Level): Int = when (level) {
    Level.BASE -> R.string.onboarding_level_base_line
    Level.INTERMEDIATE -> R.string.onboarding_level_intermediate_line
    Level.ADVANCED -> R.string.onboarding_level_advanced_line
    Level.COMPLETE -> R.string.onboarding_level_complete_line
}

/** What the tests find the run's parts by. */
object OnboardingTags {
    const val ROOT = "onboarding"
    const val PAGE = "onboarding_page"
    const val PRIMARY = "onboarding_primary"
    const val SKIP = "onboarding_skip"

    fun language(language: Language) = "onboarding_language_${language.tag}"

    fun level(level: Level) = "onboarding_level_${level.name.lowercase()}"
}
