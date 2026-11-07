package cl.topingames.gamenote.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cl.topingames.gamenote.ui.theme.GameNoteTheme

// Panel ultra-minimalista: solo el campo y los botones de acción
// La burbuja de cómic ya muestra el texto — el panel no necesita repetirlo
private val PanelBg     = Color(0xFF0A0E14).copy(alpha = 0.82f)
private val GreenAccent = Color(0xFF4CAF50)
private val FieldBg     = Color.White.copy(alpha = 0.06f)

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(PanelBg)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Campo de texto — toda la línea horizontal
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                placeholder = {
                    Text(
                        "coordenadas, semilla, estrategia…",
                        color = Color.White.copy(alpha = 0.28f),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = GreenAccent.copy(alpha = 0.55f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                    cursorColor          = GreenAccent,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    focusedContainerColor   = FieldBg,
                    unfocusedContainerColor = FieldBg
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(10.dp),
                singleLine = false,
                maxLines = 2
            )

            // Botón "Ver todo" — icono pequeño
            IconButton(
                onClick = onOpenApp,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = "Abrir app",
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Botón guardar — círculo verde
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (content.isNotBlank()) GreenAccent
                        else Color.White.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSave,
                    enabled = content.isNotBlank(),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Guardar",
                        tint = if (content.isNotBlank()) Color.White
                               else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
