package cl.topingames.gamenote.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.remote.RawgApi
import cl.topingames.gamenote.data.remote.RawgGame
import cl.topingames.gamenote.data.repository.GameRepository
import cl.topingames.gamenote.domain.model.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddGameUiState(
    val query: String = "",
    val results: List<RawgGame> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AddGameViewModel @Inject constructor(
    private val rawgApi: RawgApi,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddGameUiState())
    val uiState: StateFlow<AddGameUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _queryFlow
                .debounce(400)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { q -> search(q) }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(query = q, error = null)
        _queryFlow.value = q
        if (q.length < 2) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
        }
    }

    private fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            try {
                val response = rawgApi.searchGames(search = query)
                _uiState.value = _uiState.value.copy(
                    results = response.results,
                    isSearching = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "Sin conexión — puedes agregar el juego manualmente."
                )
            }
        }
    }

    /** Guarda el juego seleccionado de RAWG */
    fun saveFromRawg(rawgGame: RawgGame) {
        viewModelScope.launch {
            val platforms = rawgGame.platforms
                ?.take(3)
                ?.joinToString(", ") { it.platform.name }
            gameRepository.saveGame(
                Game(
                    name     = rawgGame.name,
                    platform = platforms,
                    emoji    = null,
                    color    = null
                )
            )
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    /** Guarda el juego con nombre manual */
    fun saveManual(name: String, platform: String?, emoji: String?) {
        viewModelScope.launch {
            gameRepository.saveGame(
                Game(
                    name     = name.trim(),
                    platform = platform?.trim()?.ifBlank { null },
                    emoji    = emoji?.trim()?.ifBlank { null },
                    color    = null
                )
            )
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }
}
