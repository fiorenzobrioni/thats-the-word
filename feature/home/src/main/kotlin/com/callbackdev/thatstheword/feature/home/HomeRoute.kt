package com.callbackdev.thatstheword.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The start page on screen, from its view model; [onOpenGame] takes the id of the game a touch
 * started or took up. Its state is collected in the page's lifecycle, so the ticker that sees the
 * change of day runs only while the page is seen (PLANNING.md §9).
 */
@Composable
fun HomeRoute(
    onOpenGame: (Long) -> Unit,
    onOpenNotebook: () -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    HomeScreen(
        state = current,
        onLanguage = viewModel::selectLanguage,
        onPlayToday = { viewModel.openDaily(current.today, onOpenGame) },
        onDay = { viewModel.openDaily(it, onOpenGame) },
        onMonth = viewModel::showMonth,
        onLevel = viewModel::setLevel,
        onPlayFree = { viewModel.openFree(onOpenGame) },
        onHard = viewModel::setHard,
        onNotebook = onOpenNotebook,
        onProgress = onOpenProgress,
        modifier = modifier,
    )
}
