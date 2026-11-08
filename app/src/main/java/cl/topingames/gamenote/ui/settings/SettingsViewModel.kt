package cl.topingames.gamenote.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = true,
    val exportStatus: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _exportStatus = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        themeRepository.isDarkMode,
        _exportStatus
    ) { dark, status -> SettingsUiState(darkMode = dark, exportStatus = status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setDarkMode(enabled) }
    }

    fun clearExportStatus() {
        _exportStatus.value = null
    }

    fun exportNotes() {
        viewModelScope.launch {
            try {
                val notes = noteRepository.getAllNotes().first()
                if (notes.isEmpty()) {
                    _exportStatus.value = "No hay notas para exportar."
                    return@launch
                }

                val sb = StringBuilder()
                sb.appendLine("# GameNote — Notas exportadas")
                sb.appendLine("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                sb.appendLine("Total: ${notes.size} notas")
                sb.appendLine()

                // Agrupar por juego
                val byGame = notes.groupBy { it.gameName ?: "Sin juego" }
                byGame.forEach { (gameName, gameNotes) ->
                    sb.appendLine("## $gameName")
                    gameNotes.forEach { note ->
                        sb.appendLine("### ${note.title}")
                        if (note.content.isNotBlank()) sb.appendLine(note.content)
                        if (note.tags.isNotEmpty()) sb.appendLine("*Etiquetas: ${note.tags.joinToString(", ")}*")
                        sb.appendLine()
                    }
                }

                // Escribir en caché y compartir
                val file = File(context.cacheDir, "gamenote_export.md")
                file.writeText(sb.toString())

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/markdown"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Notas de GameNote")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Exportar notas").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })

            } catch (e: Exception) {
                _exportStatus.value = "Error al exportar: ${e.message}"
            }
        }
    }

    fun importNotes() {
        // Phase 5: SAF import — abre selector de archivo .md/.txt
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/markdown"))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
