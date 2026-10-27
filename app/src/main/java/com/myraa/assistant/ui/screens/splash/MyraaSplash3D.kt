package com.myraa.assistant.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myraa.assistant.data.AssistantBrand
import com.myraa.assistant.ui.components.NovaOrb
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.LocalNovaReducedMotion
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.NovaOrbState
import kotlinx.coroutines.delay

object SplashSession {
    var played: Boolean = false
}

@Composable
fun MyraaSplash3D(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (SplashSession.played) {
        onFinished()
        return
    }

    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current

    val orbScale = remember { Animatable(0.2f) }
    val textAlpha = remember { Animatable(0f) }
    val starsProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            delay(500)
            SplashSession.played = true
            onFinished()
            return@LaunchedEffect
        }

        // 1. Stars streak past and orb condenses from particles
        starsProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = LinearEasing)
        )
        orbScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(600, easing = FastOutSlowInEasing)
        )
        // 2. Name appears in serif
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = LinearEasing)
        )

        delay(400)
        SplashSession.played = true
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Skippable on tap
                SplashSession.played = true
                onFinished()
            },
        contentAlignment = Alignment.Center
    ) {
        // Streaking stars animation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = starsProgress.value
            val center = Offset(size.width / 2f, size.height / 2f)
            val count = 28
            for (i in 0 until count) {
                val angle = (i * (2 * Math.PI / count)).toFloat()
                val dist = (size.minDimension * 0.45f) * progress
                val x = center.x + Math.cos(angle.toDouble()).toFloat() * dist
                val y = center.y + Math.sin(angle.toDouble()).toFloat() * dist
                drawCircle(
                    color = colors.accent.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                    radius = 2.5f,
                    center = Offset(x, y)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.scale(orbScale.value)) {
                NovaOrb(
                    state = NovaOrbState.CONNECTING,
                    size = 140.dp
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = AssistantBrand.FULL_NAME,
                style = MaterialTheme.typography.displayLarge,
                color = colors.textPrimary.copy(alpha = textAlpha.value)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = AssistantBrand.TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary.copy(alpha = textAlpha.value)
            )
        }
    }
}
