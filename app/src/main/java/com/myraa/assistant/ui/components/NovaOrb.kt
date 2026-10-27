package com.myraa.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.LocalNovaReducedMotion
import com.myraa.assistant.voice.NovaOrbState
import kotlin.math.cos
import kotlin.math.sin

enum class NovaOrbStyle(val displayName: String) {
    NOVA("Nova Pearl"),
    AURORA("Aurora Borealis"),
    PLASMA("Plasma Core"),
    GLASS("Crystal Glass"),
    ENERGY("Pure Energy"),
    MINIMAL("Minimalist"),
    ARCHER("Golden Archer")
}

@Composable
fun NovaOrb(
    state: NovaOrbState,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    amplitude: Float = 0f,
    style: NovaOrbStyle = NovaOrbStyle.NOVA,
    assistantName: String = "Myraa"
) {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current

    val transition = rememberInfiniteTransition(label = "OrbTransitions")

    // Breathing: 4.2 s sine, +- 3.5%
    val breathe by transition.animateFloat(
        initialValue = -0.035f,
        targetValue = 0.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreath"
    )

    // Rotation: 9s regular, 2.6s thinking, 1.1s connecting
    val rotDuration = when (state) {
        NovaOrbState.THINKING -> 2600
        NovaOrbState.CONNECTING -> 1100
        else -> 9000
    }
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val pulseRing by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRing"
    )

    // Color palette per state
    val (coreColor, rimColor, glowColor) = when (state) {
        NovaOrbState.IDLE -> Triple(colors.lotus, colors.lagoon, Color(0x66FF6FAE))
        NovaOrbState.LISTENING -> Triple(colors.lagoon, Color(0xFF67E8F9), Color(0x803FE0D0))
        NovaOrbState.THINKING -> Triple(colors.accent, colors.lotus, Color(0x80FFB547))
        NovaOrbState.SPEAKING -> Triple(colors.accent, colors.accentSecondary, Color(0x99FF8A3D))
        NovaOrbState.CONNECTING -> Triple(Color(0xFFFBBF24), colors.accent, Color(0x80FBBF24))
        NovaOrbState.ERROR -> Triple(colors.error, Color(0xFFB91C1C), Color(0x66FF5C6C))
    }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "$assistantName status: $state"
            }
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseRadius = (this.size.minDimension / 2.6f) * (1f + (if (reducedMotion) 0f else breathe))
        val dynamicRadius = baseRadius * (1f + amplitude * 0.28f)

        // 1. Radial outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor, Color.Transparent),
                center = center,
                radius = dynamicRadius * 1.55f
            ),
            radius = dynamicRadius * 1.55f,
            center = center
        )

        // 2. State-specific animation rings
        if (state == NovaOrbState.LISTENING && !reducedMotion) {
            val ring1 = dynamicRadius * (1f + pulseRing * 0.45f)
            val alpha1 = (1f - pulseRing).coerceIn(0f, 1f)
            drawCircle(
                color = rimColor.copy(alpha = alpha1 * 0.4f),
                radius = ring1,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        if (state == NovaOrbState.CONNECTING && !reducedMotion) {
            drawArc(
                color = rimColor,
                startAngle = rotation,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - dynamicRadius * 1.15f, center.y - dynamicRadius * 1.15f),
                size = Size(dynamicRadius * 2.3f, dynamicRadius * 2.3f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // 3. Radial gradient core
        val coreBrush = Brush.radialGradient(
            colors = listOf(Color.White, coreColor, rimColor.copy(alpha = 0.85f)),
            center = Offset(center.x - dynamicRadius * 0.25f, center.y - dynamicRadius * 0.28f),
            radius = dynamicRadius
        )
        drawCircle(
            brush = coreBrush,
            radius = dynamicRadius,
            center = center
        )

        // 4. Orbiting particles for certain styles
        if (style != NovaOrbStyle.MINIMAL && state != NovaOrbState.ERROR) {
            val particleCount = when (style) {
                NovaOrbStyle.ENERGY -> 8
                NovaOrbStyle.PLASMA -> 6
                NovaOrbStyle.GLASS -> 3
                NovaOrbStyle.ARCHER -> 12
                else -> 5
            }
            val rotRad = Math.toRadians(if (reducedMotion) 0.0 else rotation.toDouble())
            for (i in 0 until particleCount) {
                val angle = rotRad + (i * (2 * Math.PI / particleCount))
                val dist = dynamicRadius * 1.22f
                val px = center.x + (dist * cos(angle)).toFloat()
                val py = center.y + (dist * sin(angle) * 0.5f).toFloat() // tilted orbit
                drawCircle(
                    color = rimColor.copy(alpha = 0.75f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // 5. White highlight specular arc for GLASS
        if (style == NovaOrbStyle.GLASS) {
            drawArc(
                color = Color.White.copy(alpha = 0.6f),
                startAngle = 210f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - dynamicRadius * 0.8f, center.y - dynamicRadius * 0.8f),
                size = Size(dynamicRadius * 1.6f, dynamicRadius * 1.6f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // 6. Pulse ring for ERROR
        if (state == NovaOrbState.ERROR && !reducedMotion) {
            drawCircle(
                color = colors.error.copy(alpha = (1f - pulseRing) * 0.6f),
                radius = dynamicRadius * (1f + pulseRing * 0.3f),
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
