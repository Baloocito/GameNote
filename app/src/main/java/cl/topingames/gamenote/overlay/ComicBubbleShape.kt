package cl.topingames.gamenote.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val BubbleBg     = Color(0xFF0D1117).copy(alpha = 0.93f)
private val BubbleBorder = Color(0xFF4CAF50).copy(alpha = 0.80f)
private val TailH        = 10f   // altura de la cola en px (se escala con density)

/**
 * Shape de bocadillo de cómic con cola triangular abajo-izquierda.
 * Compatible con Modifier.clip() y Modifier.shadow().
 */
class ComicBubbleShape(
    private val cornerRadius: Dp = 12.dp,
    private val tailLeft: Boolean = true
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { cornerRadius.toPx() }
        val tailH = with(density) { TailH.dp.toPx() }
        val tailW = tailH * 1.6f
        val bodyH = size.height - tailH
        val tailX = if (tailLeft) r * 1.5f else size.width - r * 1.5f - tailW

        val path = Path().apply {
            // Rectángulo redondeado para el cuerpo del bocadillo
            addRoundRect(RoundRect(0f, 0f, size.width, bodyH, r, r))
            // Cola triangular
            moveTo(tailX, bodyH)
            lineTo(tailX + tailW * 0.3f, bodyH + tailH)
            lineTo(tailX + tailW, bodyH)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Composable que dibuja el bocadillo con fondo oscuro y borde verde
 * sin usar shadow (compatible con cualquier API level).
 */
@Composable
fun ComicSpeechBubble(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = ComicBubbleShape(cornerRadius = 12.dp, tailLeft = true)

    Box(
        modifier = modifier
            .drawBehind {
                drawComicBubble(size, shape = ComicBubbleShape(12.dp, true))
            }
            .clip(shape)
            .background(BubbleBg)
            .padding(bottom = 10.dp)   // espacio para la cola
    ) {
        content()
    }
}

private fun DrawScope.drawComicBubble(size: Size, shape: ComicBubbleShape) {
    val outline = shape.createOutline(size, LayoutDirection.Ltr, this)
    if (outline is Outline.Generic) {
        // Borde verde
        drawPath(
            path = outline.path,
            color = Color(0xFF4CAF50).copy(alpha = 0.80f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
