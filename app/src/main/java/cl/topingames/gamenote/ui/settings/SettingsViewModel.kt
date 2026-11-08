package cl.topingames.gamenote.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.topingames.gamenote.data.repository.NoteRepository
import cl.topingames.gamenote.data.repository.ThemeRepository
import cl.topingames.gamenote.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = true,
    val exportStatus: String? = null,
    val importStatus: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _exportStatus = MutableStateFlow<String?>(null)
    private val _importStatus = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        themeRepository.isDarkMode,
        _exportStatus,
        _importStatus
    ) { dark, export, import ->
        SettingsUiState(darkMode = dark, exportStatus = export, importStatus = import)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setDarkMode(enabled) }
    }

    fun clearExportStatus() { _exportStatus.value = null }
    fun clearImportStatus() { _importStatus.value = null }

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

    /**
     * Llamado desde el Screen después de que el usuario selecciona un archivo
     * via ActivityResultLauncher (ACTION_OPEN_DOCUMENT).
     * Parsea Markdown exportado por GameNote o texto plano genérico.
     */
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            _importStatus.value = "Importando…"
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.readText()
                    ?: run { _importStatus.value = "No se pudo leer el archivo."; return@launch }

                val notes = parseMarkdownExport(text)
                if (notes.isEmpty()) {
                    _importStatus.value = "El archivo no tiene notas reconocibles."
                    return@launch
                }
                notes.forEach { noteRepository.saveNote(it) }
                _importStatus.value = "✓ ${notes.size} nota${if (notes.size != 1) "s" else ""} importada${if (notes.size != 1) "s" else ""}."
            } catch (e: Exception) {
                _importStatus.value = "Error al importar: ${e.message}"
            }
        }
    }

    /**
     * Parsea el Markdown exportado por GameNote:
     *   ## NombreJuego
     *   ### TítuloNota
     *   contenido…
     * También acepta texto plano donde cada párrafo separado por línea vacía es una nota.
     */
    private fun parseMarkdownExport(text: String): List<Note> {
        val notes = mutableListOf<Note>()
        val lines = text.lines()

        // Detectar si es formato GameNote (tiene encabezados ### )
        val isGameNoteFormat = lines.any { it.startsWith("### ") }

        if (isGameNoteFormat) {
            var currentTitle = ""
            var currentContent = StringBuilder()

            fun flush() {
                if (currentTitle.isNotBlank()) {
                    notes.add(Note(title = currentTitle, content = currentContent.toString().trim()))
                }
            }

            for (line in lines) {
                when {
                    line.startsWith("# ") || line.startsWith("## ") -> {
                        // Sección de juego o encabezado principal — no es nota
                    }
                    line.startsWith("### ") -> {
                        flush()
                        currentTitle = line.removePrefix("### ").trim()
                        currentContent = StringBuilder()
                    }
                    line.startsWith("*Etiquetas:") -> {
                        // Ignoramos etiquetas en la importación básica
                    }
                    else -> {
                        if (currentTitle.isNotBlank()) {
                            if (currentContent.isNotEmpty() || line.isNotBlank()) {
                                currentContent.appendLine(line)
                            }
                        }
                    }
                }
            }
            flush()
        } else {
            // Texto plano: cada párrafo (bloques separados por línea vacía) = una nota
            val paragraphs = text.split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotBlank() }
            paragraphs.forEach { para ->
                val paraLines = para.lines()
                val title = paraLines.first().take(80)
                val content = if (paraLines.size > 1) paraLines.drop(1).joinToString("\n").trim() else ""
                notes.add(Note(title = title, content = content))
            }
        }

        return notes
    }
}
