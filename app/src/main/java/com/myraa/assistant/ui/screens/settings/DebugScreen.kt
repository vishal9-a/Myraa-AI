package com.myraa.assistant.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.offline.MyraaListenService
import com.myraa.assistant.offline.ModeManager
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.VoiceSessionManager

@Composable
fun DebugScreen(
    appState: NovaAppState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current

    val liveState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val isMuted by VoiceSessionManager.muted.collectAsState()
    val detectedLang by VoiceSessionManager.detectedLanguage.collectAsState()
    val vadActive by VoiceSessionManager.vadActive.collectAsState()
    val reconnectCount by VoiceSessionManager.latencyTracker.reconnectCount.collectAsState()
    val turnLatency by VoiceSessionManager.latencyTracker.lastTurnLatencyMs.collectAsState()

    val isOnline by ModeManager.isOnline.collectAsState()
    val isHandsFreeService by MyraaListenService.isServiceActive.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .verticalScroll(rememberScrollState())
    ) {
        NovaTopBar(title = "Voice Pipeline Debugger", onBack = onNavigateBack)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DebugCard("Live Voice Pipeline") {
                DebugRow("Session State", liveState.name)
                DebugRow("Active Personality", appState.personality.name)
                DebugRow("Configured Language", appState.language.name)
                DebugRow("Detected Language", detectedLang.name)
                DebugRow("Mic Amplitude (RMS)", String.format("%.4f", amplitude))
                DebugRow("VAD (Voice Activity)", if (vadActive) "ACTIVE (Voice detected)" else "QUIET")
                DebugRow("Mic Muted (EchoGuard)", if (isMuted) "MUTED" else "UNMUTED")
                DebugRow("Last Turn Latency", if (turnLatency > 0) "$turnLatency ms" else "N/A")
                DebugRow("Reconnect Count", "$reconnectCount attempts")
            }

            DebugCard("Hands-Free & Offline Engine") {
                DebugRow("Network Connection", if (isOnline) "ONLINE" else "OFFLINE")
                DebugRow("Active Pipeline Engine", if (ModeManager.shouldUseOfflineBrain()) "Local Engine (On-Device)" else "Cloud Gemini Live")
                DebugRow("Hands-Free Service", if (isHandsFreeService) "RUNNING (Listening for 'Hey Myraa')" else "STOPPED")
                DebugRow("Wake Sensitivity", ModeManager.sensitivity.value.name)
            }
        }
    }
}

@Composable
fun DebugCard(title: String, content: @Composable () -> Unit) {
    val colors = LocalNovaColors.current
    NovaGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.accent
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun DebugRow(label: String, value: String) {
    val colors = LocalNovaColors.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = colors.textPrimary
        )
    }
}
