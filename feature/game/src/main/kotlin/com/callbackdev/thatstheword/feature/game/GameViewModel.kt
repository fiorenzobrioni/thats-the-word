package com.callbackdev.thatstheword.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.thatstheword.core.data.GameRepository
import com.callbackdev.thatstheword.core.data.InterfaceLanguage
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.NotebookRepository
import com.callbackdev.thatstheword.core.data.SavedGame
import com.callbackdev.thatstheword.core.data.SettingsRepository
import com.callbackdev.thatstheword.core.domain.game.Game
import com.callbackdev.thatstheword.core.domain.game.Refusal
import com.callbackdev.thatstheword.core.domain.game.Submission
import com.callbackdev.thatstheword.core.domain.share.ShareText
import com.callbackdev.thatstheword.core.domain.words.Alphabet
import com.callbackdev.thatstheword.core.model.GameMode
import com.callbackdev.thatstheword.core.model.HintKind
import com.callbackdev.thatstheword.core.model.Letter
import com.callbackdev.thatstheword.core.model.Milestone
import com.callbackdev.thatstheword.core.model.Word
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val settings: SettingsRepository,
    private val interfaceLanguage: InterfaceLanguage,
    private val notebook: NotebookRepository,
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

    /** The milestones the game reached while the page was open: said once, on its end card. */
    private val reached = MutableStateFlow<List<Milestone>>(emptyList())

    /** The game language's words in the notebook, for the end card's marks. */
    private val kept = combine(saved.map { it?.game?.setup?.language }.distinctUntilChanged(), notebook.entries()) {
            language,
            entries,
        ->
        entries.filter { it.language == language }.associateBy { it.word }
    }

    /** The interface's language, read once a page: the end card's meanings speak it. */
    private val speaks = interfaceLanguage.current()

    val state: StateFlow<GameUiState?> =
        combine(saved, typed, refusal, kept, reached) { saved, typed, refusal, kept, reached ->
            saved?.let {
                GameUiState.of(
                    game = it.game,
                    typed = typed,
                    refusal = refusal,
                    lexicon = lexicons.of(it.game.setup.language),
                    practice = it.practice,
                    interfaceLanguage = speaks,
                    notebook = kept,
                    milestones = reached,
                )
            }
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
                setTyped(emptyList())
                play(current, submission.game)
            }
        }
    }

    /**
     * A hint taken (PLANNING.md §4.5): an attempt spent, the grid's last free row drawn as the
     * hint, saved like a guess. A hint not offered now (taken, or one attempt left) does nothing.
     */
    fun takeHint(kind: HintKind) {
        val current = saved.value ?: return
        if (kind !in current.game.hintsOffered) return
        refusal.value = null
        play(current, current.game.takeHint(kind))
    }

    /** The move made: on screen at once, saved before the next, its milestones kept for the end card. */
    private fun play(current: SavedGame, next: Game) {
        saved.value = current.copy(game = next)
        viewModelScope.launch {
            val milestones = saving.withLock { games.save(current.id, next) }
            if (milestones.isNotEmpty()) reached.value = reached.value + milestones
        }
    }

    /**
     * The end card's mark beside a word tried (the owner, PLANNING §15, Open 8): the word added to
     * the notebook, or, added by hand a moment ago, taken back. A word a game hid stays.
     */
    fun toggleNotebook(index: Int) {
        val game = saved.value?.game?.takeIf { it.state.finished } ?: return
        val word =
            game.guesses.map { it.word }.distinct().filter { it != game.setup.solution }.getOrNull(index) ?: return
        val language = game.setup.language
        val shown = state.value?.tried?.getOrNull(index) ?: return
        viewModelScope.launch {
            if (!shown.inNotebook) {
                val milestones = notebook.save(language, word)
                if (milestones.isNotEmpty()) reached.value = reached.value + milestones
            } else if (shown.removable) {
                notebook.unsave(language, word)
            }
        }
    }

    private var opening: Job? = null

    /**
     * What the end card offers next, started or taken up, its id handed to [onOpened]: the day
     * again as practice (PLANNING.md §4.6), or another free word at the game language's level
     * (PLANNING.md §4.7). Hard and the interface are read now, for the new game.
     */
    fun next(onOpened: (Long) -> Unit) {
        val game = saved.value?.game?.takeIf { it.state.finished } ?: return
        if (opening?.isActive == true) return
        opening = viewModelScope.launch {
            val current = settings.current()
            val language = game.setup.language
            val id = when (val mode = game.setup.mode) {
                is GameMode.Daily -> games.practiceGame(language, mode.date, interfaceLanguage.current(), current.hard)

                is GameMode.Free -> games.freeGame(
                    language,
                    current.levelOf(language),
                    interfaceLanguage.current(),
                    current.hard,
                )

                GameMode.Guided -> return@launch
            }
            onOpened(id)
        }
    }

    /** The result as text, without the word (PLANNING.md §7.6), once the game is over. */
    fun shareText(labels: ShareText.Labels): String? =
        saved.value?.takeIf { it.game.state.finished && !it.practice }?.let { ShareText.of(it.game, labels) }

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
