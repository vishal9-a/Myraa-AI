package com.myraa.assistant.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myraa.assistant.data.AssistantBrand
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.fx.MyraaOrb3D
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.NovaOrbState

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NovaTopBar(title = "About Myraa", onBack = onNavigateBack)

        Spacer(modifier = Modifier.height(24.dp))

        MyraaOrb3D(
            state = NovaOrbState.IDLE,
            size = 120.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = AssistantBrand.FULL_NAME,
            style = MaterialTheme.typography.displayMedium,
            color = colors.textPrimary
        )
        Text(
            text = AssistantBrand.TAGLINE,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Myraa is an intelligent voice-first companion for Android, powered by Gemini Live and custom on-device offline pipelines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "• Developer: Vishal Bargal\n• Version: 1.0 (Production)\n• Real-Time Audio: 16kHz capture & 24kHz playback\n• Offline Engine: Local VAD, Intent Router & On-device Gemma\n• Design: Pearl of Light 3D Theme",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
