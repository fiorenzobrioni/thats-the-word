package com.callbackdev.thatstheword.feature.game

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.thatstheword.core.domain.share.ShareText
import com.callbackdev.thatstheword.core.model.GameMode

/**
 * The game of [gameId], on screen: its view model, and the share sheet for its result (no
 * permission: the text goes through the system's chooser, PLANNING.md §10). [onOpenGame] takes
 * the id of the game the end card offers next, which stands in this one's place.
 */
@Composable
fun GameRoute(gameId: Long, onBack: () -> Unit, onOpenGame: (Long) -> Unit, modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<GameViewModel, GameViewModel.Factory>(
        key = "game-$gameId",
        creationCallback = { it.create(gameId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics by viewModel.haptics.collectAsStateWithLifecycle()
    val current = state ?: return
    val context = LocalContext.current
    val labels = ShareText.Labels(
        appName = stringResource(R.string.share_app_name),
        languageName = stringResource(languageName(current.language)),
        modeName = stringResource(if (current.mode is GameMode.Guided) R.string.game_guided else R.string.game_free),
        hard = stringResource(R.string.share_hard),
        hints = hintsLabel(),
    )
    val shareTitle = stringResource(R.string.end_share)
    GameScreen(
        state = current,
        haptics = haptics,
        onBack = onBack,
        onLetter = viewModel::type,
        onDelete = viewModel::delete,
        onEnter = viewModel::submit,
        onHardwareChar = viewModel::typeChar,
        onNext = { viewModel.next(onOpenGame) },
        onShare = {
            val text = viewModel.shareText(labels) ?: return@GameScreen
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(send, shareTitle))
        },
        modifier = modifier,
    )
}

/** "1 hint", "2 hints", in the interface's plural, for any count. */
@Composable
private fun hintsLabel(): (Int) -> String {
    val one = pluralStringResource(R.plurals.share_hints, 1, 1)
    val two = pluralStringResource(R.plurals.share_hints, 2, 2)
    val three = pluralStringResource(R.plurals.share_hints, 3, 3)
    // A game has three hints at most (PLANNING.md §4.5).
    return { n -> listOf(one, two, three).getOrElse(n - 1) { three } }
}
