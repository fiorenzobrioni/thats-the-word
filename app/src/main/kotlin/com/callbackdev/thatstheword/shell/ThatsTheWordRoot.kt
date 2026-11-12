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
import kotlinx.serialization.Serializable

/** Where the shell can go: the start page, and a game by its id. Saved with the activity's state. */
@Serializable
data object StartKey : NavKey

@Serializable
data class GameKey(val gameId: Long) : NavKey

/**
 * The app's pages (Navigation 3): the start page, and the game page above it. Each page's view
 * models live as long as its entry on the back stack; predictive back pops it.
 *
 * [dailyRequest] is a language whose daily of today a launcher shortcut asked for (PLANNING.md
 * §8): it is opened over the start page, whatever page was open, and [onDailyRequestHandled]
 * clears it, so it is opened once.
 */
@Composable
fun ThatsTheWordRoot(dailyRequest: Language? = null, onDailyRequestHandled: () -> Unit = {}) {
    val backStack = rememberNavBackStack(StartKey)
    val shell = hiltViewModel<ShellViewModel>()
    val reduced = reducedMotion()
    LaunchedEffect(dailyRequest) {
        val language = dailyRequest ?: return@LaunchedEffect
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
                entry<StartKey> { HomeRoute(onOpenGame = { backStack.add(GameKey(it)) }) }
                entry<GameKey> { key ->
                    GameRoute(
                        gameId = key.gameId,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenGame = { backStack.replaceTop(GameKey(it)) },
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
