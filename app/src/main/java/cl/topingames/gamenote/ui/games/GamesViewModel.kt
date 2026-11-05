package cl.topingames.gamenote.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.GameRepository
import cl.topingames.gamenote.domain.model.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GamesUiState(
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    val uiState: StateFlow<GamesUiState> = gameRepository.getAllGames()
        .map { GamesUiState(games = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamesUiState())

    fun saveGame(name: String, platform: String?, emoji: String?, color: Int?) {
        viewModelScope.launch {
            gameRepository.saveGame(Game(name = name, platform = platform, emoji = emoji, color = color))
        }
    }

    fun deleteGame(game: Game) {
        viewModelScope.launch {
            gameRepository.deleteGame(game)
        }
    }
}
