package cl.topingames.gamenote.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.domain.model.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OverlayUiState(
    val content: String = "",
    val bubbleState: BubbleState = BubbleState.Idle,
    val error: String? = null
)

/**
 * ViewModel SIN @HiltViewModel — se instancia manualmente desde OverlayService
 * usando OverlayViewModelFactory para evitar el crash de Hilt fuera de Activity/Fragment.
 */
class OverlayViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    fun onContentChange(value: String) {
        val newBubble = if (value.isBlank()) BubbleState.Idle else BubbleState.Typing(value)
        _uiState.value = _uiState.value.copy(content = value, bubbleState = newBubble, error = null)
    }

    fun saveNote(onSuccess: () -> Unit) {
        val text = _uiState.value.content.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                noteRepository.saveNote(
                    Note(
                        title   = text.take(50),
                        content = text,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                // Mostrar check en la burbuja 1 segundo antes de cerrar
                _uiState.value = OverlayUiState(bubbleState = BubbleState.Saved)
                delay(900)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al guardar")
            }
        }
    }

    fun reset() {
        _uiState.value = OverlayUiState()
    }

    // Factory para instanciar desde Service sin Hilt
    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OverlayViewModel(repository) as T
    }
}
