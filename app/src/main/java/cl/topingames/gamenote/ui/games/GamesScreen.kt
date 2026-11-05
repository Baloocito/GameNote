package cl.topingames.gamenote.ui.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.topingames.gamenote.domain.model.Game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    onNavigateToGame: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: GamesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var gameToDelete by remember { mutableStateOf<Game?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mis Juegos", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar juego")
            }
        }
    ) { padding ->
        if (uiState.games.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎮", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Agrega tu primer juego",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.games, key = { it.id }) { game ->
                    GameListItem(
                        game = game,
                        onClick = { onNavigateToGame(game.id) },
                        onDelete = { gameToDelete = game }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddGameDialog(
            onConfirm = { name, platform, emoji ->
                viewModel.saveGame(name, platform, emoji, null)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    gameToDelete?.let { game ->
        AlertDialog(
            onDismissRequest = { gameToDelete = null },
            title = { Text("Eliminar juego") },
            text = { Text("¿Eliminar \"${game.name}\"? Las notas no se borrarán.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGame(game)
                    gameToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { gameToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun GameListItem(game: Game, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (game.emoji != null) {
                Text(text = game.emoji, style = MaterialTheme.typography.headlineSmall)
            } else {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = game.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                if (!game.platform.isNullOrBlank()) {
                    Text(
                        text = game.platform,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun AddGameDialog(onConfirm: (String, String?, String?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo juego") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del juego *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = platform,
                    onValueChange = { platform = it },
                    label = { Text("Plataforma (opcional)") },
                    singleLine = true,
                    placeholder = { Text("PC, Android, PS5…") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji (opcional)") },
                    singleLine = true,
                    placeholder = { Text("🎮 ⚔️ 🏰") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), platform.trim().ifBlank { null }, emoji.trim().ifBlank { null })
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
