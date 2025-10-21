package com.example.jasmineplayer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveProgressBar(
    progress: Float,
    isAnimating: Boolean,
    onSeekStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    waveColor: androidx.compose.ui.graphics.Color,
    backgroundColor: androidx.compose.ui.graphics.Color,
) {
    val waveLength = 120f
    val baseAmplitude = 12f

    val amplitude by animateFloatAsState(
        targetValue = if (isAnimating) baseAmplitude else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "amplitude_animation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = waveLength,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onSeekStart()
                        val initialProgress = down.position.x.coerceIn(0f, size.width.toFloat()) / size.width
                        onSeek(initialProgress)

                        val dragResult = horizontalDrag(down.id) { change ->
                            val newProgress = change.position.x.coerceIn(0f, size.width.toFloat()) / size.width
                            onSeek(newProgress)
                        }

                        if (dragResult) {
                            onSeekFinished()
                        }
                    }
                }
            }
    ) { 
        val canvasWidth = size.width
        val canvasHeight = size.height
        val midY = canvasHeight / 2f

        // Background wave
        val backgroundPath = Path().apply {
            moveTo(0f, midY)
            for (x in 0..canvasWidth.toInt() step 2) {
                val phase = 2 * Math.PI * ((x + waveOffset) / waveLength)
                val y = midY + amplitude * sin(phase).toFloat()
                lineTo(x.toFloat(), y)
            }
        }
        drawPath(backgroundPath, color = backgroundColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

        // Progress wave
        val progressX = progress * canvasWidth
        clipRect(right = progressX) {
            val progressPath = Path().apply {
                moveTo(0f, midY)
                for (x in 0..canvasWidth.toInt() step 2) {
                    val phase = 2 * Math.PI * ((x + waveOffset) / waveLength)
                    val y = midY + amplitude * sin(phase).toFloat()
                    lineTo(x.toFloat(), y)
                }
                lineTo(progressX, canvasHeight)
                lineTo(0f, canvasHeight)
                close()
            }
            drawPath(progressPath, color = waveColor)
        }
    }
}
