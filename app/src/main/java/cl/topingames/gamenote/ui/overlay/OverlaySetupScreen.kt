package cl.topingames.gamenote.ui.overlay

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.topingames.gamenote.domain.model.Game

private val Green = Color(0xFF4CAF50)

@Composable
fun OverlaySetupScreen(
    viewModel: OverlaySetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.checkPermission() }

    LaunchedEffect(Unit) { viewModel.checkPermission() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // ── Icono ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Green.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SportsEsports,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Burbuja flotante",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Toma notas mientras juegas sin cerrar tu partida.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ── BLOQUE PERMISO ──────────────────────────────────────────────────
        if (!uiState.hasPermission) {
            PermissionCard()
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    permissionLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Dar permiso en Ajustes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            // ── SELECTOR DE JUEGO ───────────────────────────────────────────
            GameSelectorSection(
                games        = uiState.games,
                selectedGame = uiState.selectedGame,
                isRunning    = uiState.isRunning,
                onSelect     = { viewModel.selectGame(it) }
            )

            Spacer(Modifier.height(24.dp))

            // ── BOTÓN ACTIVAR / ESTADO ACTIVO ───────────────────────────────
            if (!uiState.isRunning) {
                Button(
                    onClick = { viewModel.startOverlay() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("▶  Activar burbuja", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Minimiza GameNote y vuelve a tu juego.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            } else {
                ActiveBubbleSection(
                    selectedGameName = uiState.selectedGame?.name,
                    onStop = { viewModel.stopOverlay() }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── TECLADO FLOTANTE ────────────────────────────────────────────
            KeyboardCard(context = context)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Componentes privados ──────────────────────────────────────────────────────

@Composable
private fun PermissionCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text("Paso 1 — Permiso requerido",
            style = MaterialTheme.typography.labelLarge, color = Green)
        Spacer(Modifier.height(6.dp))
        Text(
            "Android necesita que autorices mostrar sobre otras apps. Se abrirán los ajustes del sistema.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GameSelectorSection(
    games: List<Game>,
    selectedGame: Game?,
    isRunning: Boolean,
    onSelect: (Game?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Juego activo (opcional)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Las notas de esta sesión se guardarán bajo este juego.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(10.dp))

        // Chips horizontales scrollables
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Chip "Sin juego"
            GameChip(
                label     = "Sin juego",
                emoji     = null,
                selected  = selectedGame == null,
                enabled   = !isRunning,
                onClick   = { onSelect(null) }
            )

            games.forEach { game ->
                GameChip(
                    label    = game.name,
                    emoji    = game.emoji,
                    selected = selectedGame?.id == game.id,
                    enabled  = !isRunning,
                    onClick  = { onSelect(game) }
                )
            }
        }
    }
}

@Composable
private fun GameChip(
    label: String,
    emoji: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg     = if (selected) Green else MaterialTheme.colorScheme.surfaceVariant
    val border = if (selected) Green else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val fg     = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (selected && emoji == null) {
            Icon(Icons.Default.Check, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(13.dp))
        }
        if (emoji != null) {
            Text(emoji, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActiveBubbleSection(
    selectedGameName: String?,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Green.copy(alpha = 0.08f))
            .border(1.dp, Green.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Green)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Burbuja activa",
                style = MaterialTheme.typography.titleMedium,
                color = Green,
                fontWeight = FontWeight.Bold
            )
        }

        if (selectedGameName != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Vinculada a: $selectedGameName",
                style = MaterialTheme.typography.bodySmall,
                color = Green.copy(alpha = 0.8f)
            )
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("■  Desactivar burbuja", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KeyboardCard(context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Keyboard, contentDescription = null,
                tint = Green, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Teclado flotante (opcional)",
                style = MaterialTheme.typography.labelLarge, color = Green)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Si tu teclado lo soporta, activa el modo flotante para que no tape el juego.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                val currentIme = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD
                )
                val pkg = currentIme?.substringBefore("/") ?: ""
                val intent = if (pkg.isNotEmpty()) {
                    context.packageManager.getLaunchIntentForPackage(pkg)
                        ?: Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                } else {
                    Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ajustes del teclado →")
        }
    }
}
