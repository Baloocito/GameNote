package cl.topingames.gamenote.ui.overlay

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.GameRepository
import cl.topingames.gamenote.domain.model.Game
import cl.topingames.gamenote.overlay.OverlayManager
import cl.topingames.gamenote.overlay.OverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class OverlaySetupUiState(
    val hasPermission: Boolean = false,
    val isRunning: Boolean = false,
    val games: List<Game> = emptyList(),
    val selectedGame: Game? = null,
    val isLoadingGames: Boolean = true
)

@HiltViewModel
class OverlaySetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlayManager: OverlayManager,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _permission = MutableStateFlow(overlayManager.hasPermission())
    private val _running    = MutableStateFlow(isServiceRunning())
    private val _selected   = MutableStateFlow<Game?>(null)

    val uiState: StateFlow<OverlaySetupUiState> = combine(
        _permission,
        _running,
        gameRepository.getAllGames(),
        _selected
    ) { permission, running, games, selected ->
        OverlaySetupUiState(
            hasPermission   = permission,
            isRunning       = running,
            games           = games,
            selectedGame    = selected,
            isLoadingGames  = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverlaySetupUiState()
    )

    fun checkPermission() {
        _permission.value = overlayManager.hasPermission()
        _running.value    = isServiceRunning()
    }

    fun selectGame(game: Game?) {
        _selected.value = game
    }

    fun startOverlay() {
        val gameId = _selected.value?.id
        overlayManager.startOverlay(gameId)
        _running.value = true
    }

    fun stopOverlay() {
        overlayManager.stopOverlay()
        _running.value = false
    }

    /** Detecta si OverlayService está corriendo via ActivityManager. */
    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getRunningServices(50).any {
            it.service.className == OverlayService::class.java.name
        }
    }
}
