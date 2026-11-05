package cl.topingames.gamenote.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OverlayUiState(
    val title: String = "",
    val content: String = "",
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OverlayViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value, saved = false, error = null)
    }

    fun onContentChange(value: String) {
        _uiState.value = _uiState.value.copy(content = value, saved = false, error = null)
    }

    fun saveNote(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return

        viewModelScope.launch {
            try {
                val note = Note(
                    title = state.title.ifBlank { "Nota rápida" },
                    content = state.content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                noteRepository.saveNote(note)
                _uiState.value = OverlayUiState(saved = true)
                kotlinx.coroutines.delay(600)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al guardar: ${e.message}"
                )
            }
        }
    }
}
