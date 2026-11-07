package cl.topingames.gamenote.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val GreenMain = Color(0xFF4CAF50)
private val GreenDim  = Color(0xFF4CAF50).copy(alpha = 0.50f)

sealed class BubbleState {
    object Idle : BubbleState()
    data class Typing(val text: String) : BubbleState()
    object Saved : BubbleState()
}

// Clave de animación: solo el TIPO, no el texto — evita parpadeo al escribir
private fun BubbleState.animKey() = when (this) {
    is BubbleState.Idle   -> 0
    is BubbleState.Typing -> 1   // mismo key para todos los Typing → no re-anima
    is BubbleState.Saved  -> 2
}

@Composable
fun OverlayBubble(state: BubbleState = BubbleState.Idle) {
    GameNoteTheme {
        // Anima SOLO cuando cambia el tipo (Idle↔Typing↔Saved), no al escribir
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(tween(150)) togetherWith fadeOut(tween(150))
            },
            contentKey = { it.animKey() },
            label = "bubble_type"
        ) { s ->
            when (s) {
                is BubbleState.Idle   -> IdleBubble()
                is BubbleState.Typing -> ComicBubble(text = s.text)
                is BubbleState.Saved  -> SavedBubble()
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
    ComicSpeechBubble(
        modifier = Modifier.widthIn(min = 120.dp, max = 240.dp)
    ) {
        Text(
            text = text.ifBlank { "…" },
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
