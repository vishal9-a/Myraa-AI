package com.myraa.assistant.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky

@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .verticalScroll(rememberScrollState())
    ) {
        NovaTopBar(title = "Privacy & Safety", onBack = onNavigateBack)

        NovaGlassCard(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Zero Audio Recording / Storage", style = MaterialTheme.typography.titleMedium, color = colors.accent)
                Text(
                    "Myraa only processes audio in a rolling in-memory buffer while listening. No audio recordings are ever stored on device storage or uploaded to telemetry servers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )

                Text("On-Device API Key Security", style = MaterialTheme.typography.titleMedium, color = colors.accent)
                Text(
                    "Your Gemini API key is saved locally in private SharedPreferences and transmitted solely directly to Google's official Gemini endpoint via secure TLS WebSockets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )

                Text("Sensitive Action Protections", style = MaterialTheme.typography.titleMedium, color = colors.accent)
                Text(
                    "Irreversible tasks such as posting on social media, sending public messages, or initiating outbound phone calls always require explicit user review or confirmation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )
            }
        }
    }
}
