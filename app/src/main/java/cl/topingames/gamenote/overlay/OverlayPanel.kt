package cl.topingames.gamenote.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cl.topingames.gamenote.ui.theme.GameNoteTheme

private val PanelBg    = Color(0xFF0D1117).copy(alpha = 0.88f)
private val GreenAccent = Color(0xFF4CAF50)
private val TextHint   = Color.White.copy(alpha = 0.35f)

/**
 * Panel sin ViewModel propio — el estado vive en OverlayService
 * para evitar el crash de Hilt fuera de una Activity/Fragment.
 */
@Composable
fun OverlayPanel(
    content: String,
    saved: Boolean,
    error: String?,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80)
        runCatching { focusRequester.requestFocus() }
    }

    GameNoteTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(PanelBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✎  Nota rápida",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenAccent,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onOpenApp,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("Ver todo", color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.width(32.dp).height(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar",
                        tint = Color.White.copy(alpha = 0.4f))
                }
            }

            // Campo texto
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                placeholder = {
                    Text("coordenadas, semilla, estrategia…",
                        color = TextHint,
                        style = MaterialTheme.typography.bodySmall)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = GreenAccent.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    cursorColor          = GreenAccent,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(10.dp)
            )

            // Fila acción
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    saved        -> Text("✓ Guardada", color = GreenAccent,
                                        style = MaterialTheme.typography.labelSmall)
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall)
                    else         -> Spacer(Modifier.width(1.dp))
                }

                IconButton(
                    onClick = onSave,
                    enabled = content.isNotBlank(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (content.isNotBlank()) GreenAccent
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .width(44.dp)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Guardar",
                        tint = if (content.isNotBlank()) Color.White
                               else Color.White.copy(alpha = 0.3f))
                }
            }
        }
    }
}
