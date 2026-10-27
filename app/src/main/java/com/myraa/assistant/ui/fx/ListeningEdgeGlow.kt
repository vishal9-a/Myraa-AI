package com.myraa.assistant.ui.fx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.myraa.assistant.ui.theme.LocalNovaReducedMotion
import com.myraa.assistant.voice.NovaOrbState
import com.myraa.assistant.voice.VoiceSessionManager

@Composable
fun ListeningEdgeGlow(modifier: Modifier = Modifier) {
    val isActive by VoiceSessionManager.isActive.collectAsState()
    val state by VoiceSessionManager.state.collectAsState()
    val reducedMotion = LocalNovaReducedMotion.current

    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(400))
    ) {
        val transition = rememberInfiniteTransition(label = "EdgeGlowTransition")
        val pulse by transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "EdgePulse"
        )

        val glowColor = when (state) {
            NovaOrbState.SPEAKING -> Color(0xFFFF6FAE) // pink
            NovaOrbState.ERROR -> Color(0xFFFF7A85)    // red
            NovaOrbState.LISTENING -> Color(0xFF3FE0D0) // lagoon
            else -> Color(0xFF8B5CF6)                   // resting violet
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            val strokeWidth = 26.dp.toPx()
            val alpha = if (reducedMotion) 0.6f else pulse

            // Draw border glowing stroke
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = alpha * 0.7f), Color.Transparent),
                    center = Offset(this.size.width / 2f, this.size.height / 2f),
                    radius = this.size.maxDimension
                ),
                style = Stroke(width = strokeWidth)
            )

            // Top and bottom accent lines
            drawRect(
                color = glowColor.copy(alpha = alpha * 0.4f),
                topLeft = Offset(0f, 0f),
                size = Size(this.size.width, strokeWidth * 0.5f)
            )
            drawRect(
                color = glowColor.copy(alpha = alpha * 0.4f),
                topLeft = Offset(0f, this.size.height - strokeWidth * 0.5f),
                size = Size(this.size.width, strokeWidth * 0.5f)
            )
        }
    }
}
