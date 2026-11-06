package cl.topingames.gamenote.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.topingames.gamenote.ui.theme.GameNoteTheme

/**
 * Burbuja flotante — solo visual.
 * El touch/drag/tap se maneja en View layer desde OverlayService
 * para evitar conflictos con FLAG_NOT_FOCUSABLE.
 */
@Composable
fun OverlayBubble() {
    GameNoteTheme {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                // Verde TopinGames al 70% de opacidad — no distrae
                .background(Color(0xFF4CAF50).copy(alpha = 0.50f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
