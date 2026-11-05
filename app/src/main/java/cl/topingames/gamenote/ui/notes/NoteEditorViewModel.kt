package cl.topingames.gamenote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.GameRepository
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.domain.model.Game
import cl.topingames.gamenote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteEditorUiState(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val gameId: Long? = null,
    val selectedGame: Game? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null,
    val availableGames: List<Game> = emptyList()
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val games = gameRepository.getAllGames().first()
            _uiState.value = _uiState.value.copy(availableGames = games, isLoading = false)
        }
    }

    fun loadNote(noteId: Long?, presetGameId: Long?) {
        viewModelScope.launch {
            if (noteId != null) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val game = note.gameId?.let { gameRepository.getGameById(it) }
                    _uiState.value = _uiState.value.copy(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        tags = note.tags,
                        gameId = note.gameId,
                        selectedGame = game,
                        isFavorite = note.isFavorite,
                        isLoading = false
                    )
                }
            } else if (presetGameId != null) {
                val game = gameRepository.getGameById(presetGameId)
                _uiState.value = _uiState.value.copy(
                    gameId = presetGameId,
                    selectedGame = game,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onContentChange(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
    }

    fun onTagInputChange(input: String) {
        _uiState.value = _uiState.value.copy(tagInput = input)
    }

    fun addTag() {
        val tag = _uiState.value.tagInput.trim().lowercase()
        if (tag.isNotBlank() && tag !in _uiState.value.tags) {
            _uiState.value = _uiState.value.copy(
                tags = _uiState.value.tags + tag,
                tagInput = ""
            )
        }
    }

    fun removeTag(tag: String) {
        _uiState.value = _uiState.value.copy(tags = _uiState.value.tags - tag)
    }

    fun toggleFavorite() {
        _uiState.value = _uiState.value.copy(isFavorite = !_uiState.value.isFavorite)
    }

    fun selectGame(game: Game?) {
        _uiState.value = _uiState.value.copy(gameId = game?.id, selectedGame = game)
    }

    fun saveNote() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "El título no puede estar vacío")
            return
        }
        viewModelScope.launch {
            try {
                val note = Note(
                    id = state.id,
                    gameId = state.gameId,
                    title = state.title.trim(),
                    content = state.content,
                    tags = state.tags,
                    isFavorite = state.isFavorite
                )
                if (state.id == 0L) noteRepository.saveNote(note)
                else noteRepository.updateNote(note)
                _uiState.value = _uiState.value.copy(isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al guardar: ${e.message}")
            }
        }
    }

    fun deleteNote() {
        val state = _uiState.value
        if (state.id == 0L) return
        viewModelScope.launch {
            noteRepository.deleteNote(Note(id = state.id, title = state.title))
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
