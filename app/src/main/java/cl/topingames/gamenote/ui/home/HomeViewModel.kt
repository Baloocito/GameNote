package cl.topingames.gamenote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.GameRepository
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.domain.model.Game
import cl.topingames.gamenote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val recentGames: List<Game> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    gameRepository: GameRepository,
    noteRepository: NoteRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        gameRepository.getRecentGames(6),
        noteRepository.getRecentNotes(10)
    ) { games, notes ->
        HomeUiState(
            recentGames = games,
            recentNotes = notes,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
