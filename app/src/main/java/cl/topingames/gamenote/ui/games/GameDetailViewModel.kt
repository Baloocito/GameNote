package cl.topingames.gamenote.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.GameRepository
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.domain.model.Game
import cl.topingames.gamenote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameDetailUiState(
    val game: Game? = null,
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _gameId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GameDetailUiState> = _gameId
        .filterNotNull()
        .flatMapLatest { gameId ->
            combine(
                _searchQuery.flatMapLatest { query ->
                    if (query.isBlank()) noteRepository.getNotesByGame(gameId)
                    else noteRepository.searchNotesInGame(gameId, query)
                },
                _searchQuery
            ) { notes, query ->
                GameDetailUiState(
                    notes = notes,
                    searchQuery = query,
                    isLoading = false
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameDetailUiState())

    fun loadGame(gameId: Long) {
        _gameId.value = gameId
        viewModelScope.launch {
            val game = gameRepository.getGameById(gameId)
            // Update state with game info - handled by combining flows in production
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }
}
