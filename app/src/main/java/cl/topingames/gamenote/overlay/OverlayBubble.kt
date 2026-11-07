package cl.topingames.gamenote.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.topingames.gamenote.ui.theme.GameNoteTheme

private val GreenMain  = Color(0xFF4CAF50)
private val BubbleBg   = Color(0xFF0D1117).copy(alpha = 0.92f)
private val GreenDim   = Color(0xFF4CAF50).copy(alpha = 0.50f)

sealed class BubbleState {
    object Idle : BubbleState()
    data class Typing(val text: String) : BubbleState()
    object Saved : BubbleState()
}

/**
 * Burbuja que cambia de forma según el estado:
 *  - Idle  → círculo verde con gamepad (36dp)
 *  - Typing → bocadillo de cómic con el texto en tiempo real
 *  - Saved  → check verde por 1s antes de volver a Idle
 */
@Composable
fun OverlayBubble(state: BubbleState = BubbleState.Idle) {
    GameNoteTheme {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "bubble_state"
        ) { s ->
            when (s) {
                is BubbleState.Idle -> IdleBubble()
                is BubbleState.Typing -> ComicBubble(text = s.text)
                is BubbleState.Saved -> SavedBubble()
            }
        }
    }
}

@Composable
private fun IdleBubble() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(GreenDim),
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

@Composable
private fun ComicBubble(text: String) {
    // Bocadillo de cómic: fondo oscuro, borde verde, texto blanco, cola abajo-izquierda
    ComicSpeechBubble(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 240.dp)
            .shadow(6.dp, ComicBubbleShape(tailLeft = true), clip = false)
    ) {
        Text(
            text = if (text.isBlank()) "…" else text,
            color = if (text.isBlank()) Color.White.copy(0.35f) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun SavedBubble() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(GreenMain),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Guardada",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
