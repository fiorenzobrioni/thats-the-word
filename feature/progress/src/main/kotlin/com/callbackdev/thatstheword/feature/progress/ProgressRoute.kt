package com.callbackdev.thatstheword.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The progress page on screen, from its view model, collected in the page's lifecycle. */
@Composable
fun ProgressRoute(onBack: () -> Unit, onOpenData: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<ProgressViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    ProgressScreen(
        state = current,
        onBack = onBack,
        onLanguage = viewModel::showLanguage,
        onMode = viewModel::showMode,
        onOpenData = onOpenData,
        modifier = modifier,
    )
}
