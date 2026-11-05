package cl.topingames.gamenote.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.topingames.gamenote.domain.model.Game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long?,
    gameId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var showGamePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(noteId, gameId) {
        viewModel.loadNote(noteId, gameId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    // Auto-save on content change (debounced in production)
    LaunchedEffect(uiState.title, uiState.content) {
        // Real auto-save would use debounce here
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId == null) "Nueva nota" else "Editar nota",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.title.isNotBlank()) viewModel.saveNote()
                        else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver y guardar")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (noteId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = viewModel::saveNote) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Game selector
            GameSelector(
                selectedGame = uiState.selectedGame,
                onClick = { showGamePicker = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Title
            TextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Título de la nota") },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), thickness = 0.5.dp)

            // Content
            TextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp)
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Escribe tu nota aquí…\n\nCoordenadas, semillas, códigos, builds, estrategias…") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            // Tags
            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), thickness = 0.5.dp)
            TagsSection(
                tags = uiState.tags,
                tagInput = uiState.tagInput,
                onTagInputChange = viewModel::onTagInputChange,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Error snackbar
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
        )
    }

    // Game picker
    if (showGamePicker) {
        GamePickerDialog(
            games = uiState.availableGames,
            currentGameId = uiState.gameId,
            onSelect = { game ->
                viewModel.selectGame(game)
                showGamePicker = false
            },
            onDismiss = { showGamePicker = false }
        )
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar nota") },
            text = { Text("¿Eliminar esta nota permanentemente?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote()
                    showDeleteDialog = false
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun GameSelector(selectedGame: Game?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedGame?.let { "${it.emoji ?: ""} ${it.name}".trim() } ?: "Sin juego asignado",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedGame != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
private fun TagsSection(
    tags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("Etiquetas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = onTagInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("coordenadas, semilla, build…") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddTag() })
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(onClick = onAddTag, enabled = tagInput.isNotBlank()) {
                Icon(Icons.Default.Add, contentDescription = "Agregar etiqueta")
            }
        }
        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text("#$tag") },
                        trailingIcon = {
                            IconButton(onClick = { onRemoveTag(tag) }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(12.dp))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GamePickerDialog(
    games: List<Game>,
    currentGameId: Long?,
    onSelect: (Game?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar juego") },
        text = {
            Column {
                // No game option
                TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sin juego asignado")
                }
                Divider()
                games.forEach { game ->
                    TextButton(
                        onClick = { onSelect(game) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Text(game.emoji ?: "🎮")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(game.name)
                            if (game.id == currentGameId) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
