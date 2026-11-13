package com.callbackdev.thatstheword.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.thatstheword.core.designsystem.sound.rememberGameSounds
import com.callbackdev.thatstheword.core.domain.share.ShareText
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.feature.game.share.ShareSheet

/**
 * The game of [gameId], on screen: its view model, and the share sheet for its result, the picture
 * or the text (no permission: both go through the system's chooser, PLANNING.md §7.6, §10).
 * [onOpenGame] takes the id of the game the end card offers next, which stands in this one's place;
 * [onGuidedDone] is the guided first game's end: the game itself, from its start page. With
 * [askToLeave], the page opens on the question whether to leave this free word for a new one
 * (PLANNING.md §4.7), asked once.
 *
 * The sounds are decoded while the page is started and released when it stops (PLANNING.md §9).
 */
@Composable
fun GameRoute(
    gameId: Long,
    onBack: () -> Unit,
    onOpenGame: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onGuidedDone: () -> Unit = onBack,
    askToLeave: Boolean = false,
) {
    val viewModel = hiltViewModel<GameViewModel, GameViewModel.Factory>(
        key = "game-$gameId",
        creationCallback = { it.create(gameId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics by viewModel.haptics.collectAsStateWithLifecycle()
    val sound by viewModel.sound.collectAsStateWithLifecycle()
    val play = rememberGameSounds(enabled = sound)
    val current = state ?: return
    val labels = ShareText.Labels(
        appName = stringResource(R.string.share_app_name),
        languageName = stringResource(languageName(current.language)),
        modeName = stringResource(if (current.mode is GameMode.Guided) R.string.game_guided else R.string.game_free),
        hard = stringResource(R.string.share_hard),
        hints = hintsLabel(),
    )
    // Saved, so a sheet open when the phone turns is open after.
    var sharing by rememberSaveable { mutableStateOf(false) }
    // Saved too: answered once, the question is not asked again when the phone turns.
    var asking by rememberSaveable { mutableStateOf(askToLeave) }
    GameScreen(
        state = current,
        haptics = haptics,
        onBack = onBack,
        onLetter = viewModel::type,
        onDelete = viewModel::delete,
        onEnter = viewModel::submit,
        onHardwareChar = viewModel::typeChar,
        onNext = { viewModel.next(onGuidedDone, onOpenGame) },
        onTakeHint = viewModel::takeHint,
        onToggleNotebook = viewModel::toggleNotebook,
        onShare = { sharing = true },
        onSound = play,
        askToLeave = asking,
        onLeave = {
            asking = false
            viewModel.leave()
        },
        onKeep = { asking = false },
        modifier = modifier,
    )
    if (sharing) {
        viewModel.resultFacts()?.let { facts ->
            ShareSheet(facts = facts, text = { viewModel.shareText(labels) }, onDismiss = { sharing = false })
        }
    }
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
