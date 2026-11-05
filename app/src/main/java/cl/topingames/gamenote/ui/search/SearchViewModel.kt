package cl.topingames.gamenote.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Note> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = _query
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else noteRepository.searchNotes(query)
        }
        .combine(_query) { results, query ->
            SearchUiState(query = query, results = results)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(query: String) {
        _query.value = query
    }
}
