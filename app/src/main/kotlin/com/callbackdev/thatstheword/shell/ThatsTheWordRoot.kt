package com.callbackdev.thatstheword.shell

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.thatstheword.core.designsystem.theme.ThatsTheWordMotion
import com.callbackdev.thatstheword.core.designsystem.theme.reducedMotion
import com.callbackdev.thatstheword.core.model.Language
import com.callbackdev.thatstheword.feature.game.GameRoute
import com.callbackdev.thatstheword.feature.home.HomeRoute
import com.callbackdev.thatstheword.feature.notebook.NotebookRoute
import com.callbackdev.thatstheword.feature.onboarding.OnboardingRoute
import com.callbackdev.thatstheword.feature.progress.ProgressRoute
import com.callbackdev.thatstheword.feature.settings.LicencesScreen
import com.callbackdev.thatstheword.feature.settings.SettingsRoute
import com.callbackdev.thatstheword.feature.settings.data.DataRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Where the shell can go: the first run, the start page, a game by its id, the notebook, the
 * progress, settings, the licences, your data. Saved with the activity's state.
 */
@Serializable
data object OnboardingKey : NavKey

@Serializable
data object StartKey : NavKey

@Serializable
data class GameKey(val gameId: Long, val askToLeave: Boolean = false) : NavKey

@Serializable
data object NotebookKey : NavKey

@Serializable
data object ProgressKey : NavKey

@Serializable
data object DataKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object LicencesKey : NavKey

/**
 * The app's pages (Navigation 3): the first run until it is done (PLANNING.md §11 Phase 7), then
 * the start page, with the game page, the notebook, the progress or settings above it; your data
 * above the progress or settings, the licences above settings. Each page's view models live as
 * long as its entry on the back stack; predictive back pops it.
 *
 * The first run ends on the start page, with the guided first game over it unless the player
 * skipped it: Back from the guided game lands on the start page.
 *
 * [dailyRequest] is a language whose daily of today a launcher shortcut or the daily reminder
 * asked for (PLANNING.md §8): it is opened over the start page, whatever page was open, and
 * [onDailyRequestHandled] clears it, so it is opened once. During the first run it waits for the
 * run's end: the game language is the run's to choose first.
 */
@Composable
fun ThatsTheWordRoot(
    firstRunDone: Boolean = true,
    dailyRequest: Language? = null,
    onDailyRequestHandled: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(if (firstRunDone) StartKey else OnboardingKey)
    val shell = hiltViewModel<ShellViewModel>()
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val inFirstRun = backStack.firstOrNull() == OnboardingKey
    LaunchedEffect(dailyRequest, inFirstRun) {
        val language = dailyRequest ?: return@LaunchedEffect
        if (inFirstRun) return@LaunchedEffect
        val id = shell.openToday(language)
        while (backStack.size > 1) backStack.removeLastOrNull()
        backStack.add(GameKey(id))
        onDailyRequestHandled()
    }
    Surface(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = { pageTransition(reduced, forward = true) },
            popTransitionSpec = { pageTransition(reduced, forward = false) },
            predictivePopTransitionSpec = { pageTransition(reduced, forward = false) },
            entryProvider = entryProvider {
                entry<OnboardingKey> {
                    OnboardingRoute(
                        onDone = { guided ->
                            backStack.clear()
                            backStack.add(StartKey)
                            if (guided != null) backStack.add(GameKey(guided))
                        },
                    )
                }
                entry<StartKey> {
                    HomeRoute(
                        onOpenGame = { backStack.add(GameKey(it)) },
                        onOpenNotebook = { backStack.add(NotebookKey) },
                        onOpenProgress = { backStack.add(ProgressKey) },
                        onOpenSettings = { backStack.add(SettingsKey) },
                        onAskToLeave = { backStack.add(GameKey(it, askToLeave = true)) },
                    )
                }
                entry<SettingsKey> {
                    SettingsRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenData = { backStack.add(DataKey) },
                        onOpenLicences = { backStack.add(LicencesKey) },
                        onPlayGuided = { scope.launch { backStack.add(GameKey(shell.openGuided())) } },
                    )
                }
                entry<LicencesKey> { LicencesScreen(onBack = { backStack.removeLastOrNull() }) }
                entry<NotebookKey> { NotebookRoute(onBack = { backStack.removeLastOrNull() }) }
                entry<ProgressKey> {
                    ProgressRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenData = { backStack.add(DataKey) },
                    )
                }
                entry<DataKey> { DataRoute(onBack = { backStack.removeLastOrNull() }) }
                entry<GameKey> { key ->
                    GameRoute(
                        gameId = key.gameId,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenGame = { backStack.replaceTop(GameKey(it)) },
                        askToLeave = key.askToLeave,
                    )
                }
            },
        )
    }
}

/** The next game in the finished one's place: Back from it lands on the start page, not on the game before. */
private fun NavBackStack<NavKey>.replaceTop(key: NavKey) {
    removeLastOrNull()
    add(key)
}

/** A page arriving slides a little and fades, on the family's spring; a fade alone under reduced motion. */
private fun pageTransition(reduced: Boolean, forward: Boolean): ContentTransform {
    if (reduced) return fadeIn(ThatsTheWordMotion.fade()) togetherWith fadeOut(ThatsTheWordMotion.fade())
    val sign = if (forward) 1 else -1
    return (
        fadeIn(ThatsTheWordMotion.spatial()) + slideInHorizontally(ThatsTheWordMotion.spatial()) {
            sign * it / 6
        }
        ) togetherWith
        (fadeOut(ThatsTheWordMotion.spatial()) + slideOutHorizontally(ThatsTheWordMotion.spatial()) { -sign * it / 6 })
}
