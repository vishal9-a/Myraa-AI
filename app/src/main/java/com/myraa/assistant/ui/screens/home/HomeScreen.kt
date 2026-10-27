package com.myraa.assistant.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.data.AssistantBrand
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.offline.MyraaListenService
import com.myraa.assistant.offline.ModeManager
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.fx.MyraaOrb3D
import com.myraa.assistant.ui.theme.LocalBottomBarInset
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.depthSurface
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.NovaOrbState
import com.myraa.assistant.voice.VoiceSessionManager
import java.util.Calendar

@Composable
fun HomeScreen(
    appState: NovaAppState,
    onNavigateToVoice: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToForge: () -> Unit,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val bottomInset = LocalBottomBarInset.current
    val scrollState = rememberScrollState()

    val orbState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()

    val isOnline by ModeManager.isOnline.collectAsState()
    val isAlwaysOffline by ModeManager.isAlwaysOffline.collectAsState()
    val isHandsFreeListening by MyraaListenService.isServiceActive.collectAsState()

    // Determine greeting by time of day
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    val userGreeting = if (appState.userName.isNotBlank()) "$greeting, ${appState.userName}" else greeting

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .verticalScroll(scrollState)
            .padding(bottom = bottomInset),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .depthSurface(CircleShape, elevation = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✨",
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = AssistantBrand.FULL_NAME,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = appState.personality.displayName + " Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                }
            }

            IconButton(
                onClick = onNavigateToProfile,
                modifier = Modifier
                    .size(44.dp)
                    .depthSurface(CircleShape, elevation = 4.dp)
                    .testTag("home_profile_button")
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = colors.textPrimary
                )
            }
        }

        // Active Status & Hands-Free Banner
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .depthSurface(RoundedCornerShape(16.dp), elevation = 4.dp)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAlwaysOffline || !isOnline) Icons.Default.WifiOff else Icons.Default.Wifi,
                contentDescription = null,
                tint = if (isAlwaysOffline || !isOnline) colors.warning else colors.success,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isAlwaysOffline) "Offline Mode (On-Device)"
                else if (isOnline) "Online · Gemini Live Ready"
                else "Offline · Local Brain Active",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
            if (isHandsFreeListening) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .background(colors.accent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "“Hey Myraa” On",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Time-based voice font greeting
        Text(
            text = userGreeting,
            style = MaterialTheme.typography.displayLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Hero 3D Orb Stage
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .clickable { onNavigateToVoice() }
                .testTag("home_hero_orb"),
            contentAlignment = Alignment.Center
        ) {
            MyraaOrb3D(
                state = orbState,
                size = 230.dp,
                amplitude = amplitude,
                assistantName = appState.assistantName
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Tap me to talk",
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons Row (Talk & Type)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NovaButton(
                text = "Talk",
                onClick = onNavigateToVoice,
                type = NovaButtonType.PRIMARY,
                icon = Icons.Default.Mic,
                modifier = Modifier.weight(1f),
                testTag = "home_talk_button"
            )

            NovaButton(
                text = "Type",
                onClick = onNavigateToChat,
                type = NovaButtonType.SECONDARY,
                icon = Icons.Default.Textsms,
                modifier = Modifier.weight(1f),
                testTag = "home_type_button"
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // "Try Saying" Horizontal Carousel
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "TRY SAYING",
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val suggestions = listOf(
                "Myraa, mala bagh" to "👁 Camera Sight",
                "He screen baghun sang" to "📱 Screen Vision",
                "Maza favorite gaana lakshat thev" to "🧠 Permanent Memory",
                "Read my new notifications" to "🔔 Notification Reader",
                "Myraa, ek cafe ki website banao" to "⚡ 3D Website Forge",
                "Mummy ko call karo" to "📞 Hands-free Calling",
                "Subah 7 baje ka alarm lagao" to "⏰ Alarms & Timers",
                "YouTube open karo" to "📱 Fast App Launch"
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for ((phrase, tag) in suggestions) {
                    NovaGlassCard(
                        modifier = Modifier
                            .width(230.dp)
                            .clickable {
                                if (phrase.contains("website banao")) {
                                    onNavigateToForge()
                                } else {
                                    onPromptSelected(phrase)
                                }
                            },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "“$phrase”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
