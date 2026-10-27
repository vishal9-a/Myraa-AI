package com.myraa.assistant.ui.screens.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.myraa.assistant.data.ApiKeyStore
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.offline.ModeManager
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.fx.MyraaOrb3D
import com.myraa.assistant.ui.fx.ListeningEdgeGlow
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.depthSurface
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.NovaOrbState
import com.myraa.assistant.voice.VoiceForegroundService
import com.myraa.assistant.voice.VoiceSessionManager

@Composable
fun VoiceScreen(
    appState: NovaAppState,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current

    val orbState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val isMuted by VoiceSessionManager.muted.collectAsState()
    val statusText by VoiceSessionManager.statusText.collectAsState()
    val isActive by VoiceSessionManager.isActive.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            VoiceForegroundService.start(context)
        }
    }

    LaunchedEffect(hasMicPermission) {
        if (hasMicPermission) {
            if (!isActive && ApiKeyStore.hasKey(context)) {
                VoiceForegroundService.start(context)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
    ) {
        ListeningEdgeGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = appState.assistantName,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${appState.personality.displayName} · ${appState.language.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .depthSurface(CircleShape, elevation = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textPrimary
                    )
                }
            }

            // Hero 3D Orb Stage
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 48-bar visualizer ring on speaking
                    if (orbState == NovaOrbState.SPEAKING) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val baseRadius = 135.dp.toPx()
                            val barCount = 48
                            for (i in 0 until barCount) {
                                val angle = (i * (2 * Math.PI / barCount)).toFloat()
                                val barLen = (8 + amplitude * 34 * Math.abs(Math.sin(i * 0.4))).dp.toPx()
                                val startX = center.x + Math.cos(angle.toDouble()).toFloat() * baseRadius
                                val startY = center.y + Math.sin(angle.toDouble()).toFloat() * baseRadius
                                val endX = center.x + Math.cos(angle.toDouble()).toFloat() * (baseRadius + barLen)
                                val endY = center.y + Math.sin(angle.toDouble()).toFloat() * (baseRadius + barLen)
                                drawLine(
                                    color = colors.accent.copy(alpha = 0.85f),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = 3f
                                )
                            }
                        }
                    }

                    MyraaOrb3D(
                        state = orbState,
                        size = 260.dp,
                        amplitude = amplitude,
                        assistantName = appState.assistantName
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontStyle = FontStyle.Italic,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                if (orbState == NovaOrbState.ERROR) {
                    Spacer(modifier = Modifier.height(14.dp))
                    NovaButton(
                        text = "Open Settings",
                        onClick = onOpenSettings,
                        type = NovaButtonType.SECONDARY
                    )
                }
            }

            // Bottom Frosted Controls (Mute, Stop, Close)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .depthSurface(RoundedCornerShape(32.dp), elevation = 12.dp)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                IconButton(
                    onClick = { VoiceSessionManager.toggleMute() },
                    modifier = Modifier
                        .size(52.dp)
                        .background(if (isMuted) colors.error.copy(alpha = 0.2f) else colors.surfaceRaised, CircleShape)
                        .testTag("voice_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) colors.error else colors.textPrimary
                    )
                }

                // Stop button (large red-tinted)
                IconButton(
                    onClick = {
                        VoiceForegroundService.stop(context)
                        onClose()
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .background(colors.error, CircleShape)
                        .testTag("voice_stop_button")
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(52.dp)
                        .background(colors.surfaceRaised, CircleShape)
                        .testTag("voice_close_button")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textPrimary
                    )
                }
            }
        }
    }
}
