package cl.topingames.gamenote.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.GameRepository
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.domain.model.Game
import cl.topingames.gamenote.domain.model.Note
import cl.topingames.gamenote.overlay.OverlayManager
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
    private val noteRepository: NoteRepository,
    private val overlayManager: OverlayManager
) : ViewModel() {

    private val _gameId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _game = MutableStateFlow<Game?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GameDetailUiState> = _gameId
        .filterNotNull()
        .flatMapLatest { gameId ->
            combine(
                _game,
                _searchQuery.flatMapLatest { query ->
                    if (query.isBlank()) noteRepository.getNotesByGame(gameId)
                    else noteRepository.searchNotesInGame(gameId, query)
                },
                _searchQuery
            ) { game, notes, query ->
                GameDetailUiState(
                    game = game,
                    notes = notes,
                    searchQuery = query,
                    isLoading = false
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameDetailUiState())

    fun loadGame(gameId: Long) {
        _gameId.value = gameId
        viewModelScope.launch {
            _game.value = gameRepository.getGameById(gameId)
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    /** Lanza la burbuja flotante pre-vinculada al juego actual. */
    fun launchBubbleForGame() {
        val gameId = _gameId.value ?: return
        if (overlayManager.hasPermission()) {
            overlayManager.startOverlay(gameId)
        }
    }
}
