package cl.topingames.gamenote.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOverlay: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ajustes", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Gaming", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                ListItem(
                    headlineContent = { Text("Burbuja flotante") },
                    supportingContent = { Text("Toma notas sin salir de tu juego") },
                    leadingContent = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickableSettings { onNavigateToOverlay() }
                )
            }

            item {
                Divider()
                Text("Apariencia", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                ListItem(
                    headlineContent = { Text("Tema oscuro") },
                    supportingContent = { Text("Siempre activo (optimizado para gaming)") },
                    leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    trailingContent = { Switch(checked = uiState.darkMode, onCheckedChange = viewModel::setDarkMode) }
                )
            }

            item {
                Divider()
                Text("Datos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                ListItem(
                    headlineContent = { Text("Exportar notas") },
                    supportingContent = { Text("Guardar todas las notas como archivo Markdown") },
                    leadingContent = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    modifier = Modifier.clickableSettings { viewModel.exportNotes() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Importar notas") },
                    supportingContent = { Text("Cargar notas desde archivo .md o .txt") },
                    leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    modifier = Modifier.clickableSettings { viewModel.importNotes() }
                )
            }

            item {
                Divider()
                Text("Acerca de", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                ListItem(
                    headlineContent = { Text("GameNote") },
                    supportingContent = { Text("v1.0.0 · TopinGames · Xiombo") },
                    leadingContent = { Icon(Icons.Default.SportsEsports, contentDescription = null) }
                )
            }
        }
    }
}

private fun Modifier.clickableSettings(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
