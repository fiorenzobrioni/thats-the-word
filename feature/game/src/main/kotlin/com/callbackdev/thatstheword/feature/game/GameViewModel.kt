package com.callbackdev.thatstheword.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.SavedGame
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.domain.share.ShareText
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Word
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One game on screen (PLANNING.md §4.3, §7.3): the letters typed, the guesses submitted to the
 * domain's `Game`, every accepted move saved before the next can be made, a refusal said and
 * costing nothing. The game is the database's: taken up from its moves, wherever it was left.
 *
 * The letters typed and not yet submitted live in [SavedStateHandle], so a page the system took
 * away comes back with them.
 */
@HiltViewModel(assistedFactory = GameViewModel.Factory::class)
class GameViewModel @AssistedInject constructor(
    @Assisted private val gameId: Long,
    private val savedState: SavedStateHandle,
    private val games: GameRepository,
    private val lexicons: Lexicons,
    settings: SettingsRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(gameId: Long): GameViewModel
    }

    private val saved = MutableStateFlow<SavedGame?>(null)
    private val typed = MutableStateFlow(savedState.get<IntArray>(TYPED)?.map(::Letter) ?: emptyList())
    private val refusal = MutableStateFlow<RefusalUi?>(null)
    private var refusals = 0
    private val saving = Mutex()

    val state: StateFlow<GameUiState?> = combine(saved, typed, refusal) { saved, typed, refusal ->
        saved?.let { GameUiState.of(it.game, typed, refusal, lexicons.of(it.game.setup.language).forms) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Haptics on or off (the owner, PLANNING §15, Open 13: on, the phone's own setting followed). */
    val haptics: StateFlow<Boolean> = settings.settings.map { it.haptics }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        viewModelScope.launch { saved.value = games.load(gameId) }
    }

    fun type(letter: Letter) {
        val game = saved.value?.game ?: return
        if (game.state.finished || typed.value.size >= game.setup.length) return
        setTyped(typed.value + letter)
    }

    /** A character from a hardware keyboard; false when it is not one of the game language's keys. */
    fun typeChar(char: Char): Boolean {
        val game = saved.value?.game ?: return false
        val letter = Alphabet.of(game.setup.language).letterOf(char) ?: return false
        type(letter)
        return true
    }

    fun delete() {
        if (typed.value.isEmpty()) return
        setTyped(typed.value.dropLast(1))
    }

    /** Enter: the letters typed submitted as a guess; refused with its reason, or accepted and saved. */
    fun submit() {
        val current = saved.value ?: return
        val game = current.game
        if (game.state.finished) return
        val word = Word.of(typed.value)
        val submission = if (word.length < game.setup.length) {
            Submission.Refused(Refusal.TooShort)
        } else {
            game.submit(word, lexicons.of(game.setup.language).accepted)
        }
        when (submission) {
            is Submission.Refused -> refusal.value = refusalOf(submission.reason, current)

            is Submission.Accepted -> {
                refusal.value = null
                saved.value = current.copy(game = submission.game)
                setTyped(emptyList())
                viewModelScope.launch { saving.withLock { games.save(current.id, submission.game) } }
            }
        }
    }

    /** The result as text, without the word (PLANNING.md §7.6), once the game is over. */
    fun shareText(labels: ShareText.Labels): String? =
        saved.value?.game?.takeIf { it.state.finished }?.let { ShareText.of(it, labels) }

    private fun refusalOf(reason: Refusal, current: SavedGame): RefusalUi {
        val letter = when (reason) {
            is Refusal.MustBeAt -> reason.letter
            is Refusal.MustContain -> reason.letter
            else -> null
        }
        val char = letter?.let { GameUiState.capitals(Word.of(listOf(it)), current.game.setup.language).single() }
        return RefusalUi(reason, char, ++refusals)
    }

    private fun setTyped(letters: List<Letter>) {
        typed.value = letters
        savedState[TYPED] = letters.map { it.index }.toIntArray()
    }

    private companion object {
        const val TYPED = "typed"
    }
}
