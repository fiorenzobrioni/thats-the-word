package com.callbackdev.thatstheword.feature.notebook

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.thatstheword.core.data.InterfaceLanguage
import com.callbackdev.thatstheword.core.data.Lexicons
import com.callbackdev.thatstheword.core.data.NotebookRepository
import com.callbackdev.thatstheword.core.model.Language
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The notebook (VISION §9, PLANNING.md §11 Phase 5): the words met, each explained in the
 * interface's language, searched and filtered by language. The meanings are read once each time
 * the notebook changes, off the main thread; the search only filters them.
 *
 * The search and the language chosen live in [SavedStateHandle], so a page the system took away
 * comes back as it was. Collected while the page is seen (`WhileSubscribed`), nothing runs after.
 */
@HiltViewModel
class NotebookViewModel @Inject constructor(
    notebook: NotebookRepository,
    lexicons: Lexicons,
    interfaceLanguage: InterfaceLanguage,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val query = savedState.getStateFlow(QUERY, "")
    private val filter = savedState.getStateFlow<String?>(FILTER, null)

    private val words = notebook.entries().map { entries ->
        val speaks = interfaceLanguage.current()
        entries.map { NotebookWordUi.of(it, lexicons.of(it.language), speaks) }
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<NotebookUiState?> = combine(words, query, filter) { words, query, filter ->
        NotebookUiState.of(words, query, filter?.let(Language::ofTag))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MILLIS), null)

    fun search(text: String) {
        savedState[QUERY] = text
    }

    fun showLanguage(language: Language?) {
        savedState[FILTER] = language?.tag
    }

    private companion object {
        const val QUERY = "query"
        const val FILTER = "language"

        /** A rotation keeps the state; a page left for longer lets its flows go. */
        const val STOP_AFTER_MILLIS = 5_000L
    }
}
