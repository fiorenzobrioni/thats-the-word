package com.callbackdev.thatstheword.feature.notebook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The notebook on screen, from its view model, collected in the page's lifecycle. */
@Composable
fun NotebookRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<NotebookViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    NotebookScreen(
        state = current,
        onBack = onBack,
        onQuery = viewModel::search,
        onLanguage = viewModel::showLanguage,
        modifier = modifier,
    )
}
