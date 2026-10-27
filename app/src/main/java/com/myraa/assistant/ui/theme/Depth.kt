package com.myraa.assistant.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalBottomBarInset = compositionLocalOf { 88.dp }

@Composable
fun Modifier.nightSky(): Modifier {
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current

    val infiniteTransition = rememberInfiniteTransition(label = "StarTwinkle")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TwinkleAlpha"
    )

    return this.drawBehind {
        // Deep gradient from top to bottom
        val gradient = Brush.verticalGradient(
            colors = listOf(
                colors.backgroundGradientTop,
                colors.background,
                colors.background
            )
        )
        drawRect(brush = gradient)

        // Subtle stars field
        val starAlpha = if (reducedMotion) 0.5f else twinkle
        val starBrush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = starAlpha * 0.7f), Color.Transparent),
            center = Offset(size.width * 0.25f, size.height * 0.15f),
            radius = 3f
        )
        drawCircle(
            brush = starBrush,
            radius = 2.5f,
            center = Offset(size.width * 0.25f, size.height * 0.15f)
        )
        drawCircle(
            color = Color.White.copy(alpha = starAlpha * 0.5f),
            radius = 1.8f,
            center = Offset(size.width * 0.78f, size.height * 0.12f)
        )
        drawCircle(
            color = Color.White.copy(alpha = starAlpha * 0.6f),
            radius = 2.0f,
            center = Offset(size.width * 0.85f, size.height * 0.32f)
        )
        drawCircle(
            color = Color.White.copy(alpha = starAlpha * 0.4f),
            radius = 1.5f,
            center = Offset(size.width * 0.12f, size.height * 0.45f)
        )
        drawCircle(
            color = Color.White.copy(alpha = starAlpha * 0.65f),
            radius = 2.2f,
            center = Offset(size.width * 0.65f, size.height * 0.55f)
        )
    }
}

@Composable
fun Modifier.depthSurface(
    shape: Shape,
    elevation: Dp = 8.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    val colors = LocalNovaColors.current
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = colors.depthShadow,
            spotColor = colors.accentGlow
        )
        .background(color = colors.surfaceGlass, shape = shape)
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(colors.rimLight, colors.surfaceBorder)
            ),
            shape = shape
        )
}
