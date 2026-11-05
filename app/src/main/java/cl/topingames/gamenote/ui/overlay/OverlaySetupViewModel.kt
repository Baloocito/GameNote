package cl.topingames.gamenote.ui.overlay

import androidx.lifecycle.ViewModel
import cl.topingames.gamenote.overlay.OverlayManager
import cl.topingames.gamenote.overlay.OverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class OverlaySetupUiState(
    val hasPermission: Boolean = false,
    val isRunning: Boolean = false
)

@HiltViewModel
class OverlaySetupViewModel @Inject constructor(
    private val overlayManager: OverlayManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlaySetupUiState())
    val uiState: StateFlow<OverlaySetupUiState> = _uiState.asStateFlow()

    fun checkPermission() {
        _uiState.value = _uiState.value.copy(
            hasPermission = overlayManager.hasPermission()
        )
    }

    fun startOverlay() {
        overlayManager.startOverlay()
        _uiState.value = _uiState.value.copy(isRunning = true)
    }

    fun stopOverlay() {
        overlayManager.stopOverlay()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }
}
