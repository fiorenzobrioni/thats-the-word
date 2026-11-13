package com.callbackdev.thatstheword.feature.settings.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Your data on screen, from its view model, collected in the page's lifecycle. */
@Composable
fun DataRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<DataViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        DataActions(
            save = viewModel::save,
            read = viewModel::read,
            confirmRestore = viewModel::confirmRestore,
            cancelRestore = viewModel::cancelRestore,
            dismissOutcome = viewModel::dismissOutcome,
            fileName = viewModel::fileName,
        )
    }
    DataScreen(state = state, onBack = onBack, actions = actions, modifier = modifier)
}
