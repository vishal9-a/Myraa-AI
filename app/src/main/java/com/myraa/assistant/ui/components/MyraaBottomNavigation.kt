package com.myraa.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.ui.fx.MyraaOrb3D
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.depthSurface
import com.myraa.assistant.voice.VoiceSessionManager

@Composable
fun MyraaBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onTalkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val orbState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating glass bar surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .depthSurface(RoundedCornerShape(34.dp), elevation = 16.dp)
                .clip(RoundedCornerShape(34.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem("home", "Home", Icons.Default.Home, currentRoute == "home") { onNavigate("home") }
                NavItem("chat", "Chat", Icons.Default.ChatBubbleOutline, currentRoute == "chat") { onNavigate("chat") }

                // Center placeholder for the docked orb
                Box(modifier = Modifier.size(56.dp))

                NavItem("history", "History", Icons.Default.History, currentRoute == "history") { onNavigate("history") }
                NavItem("settings", "Settings", Icons.Default.Settings, currentRoute == "settings") { onNavigate("settings") }
            }
        }

        // Center Docked Talk Orb (elevated above bar)
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(68.dp)
                .clip(CircleShape)
                .background(colors.background, CircleShape)
                .depthSurface(CircleShape, elevation = 12.dp)
                .clickable { onTalkClick() },
            contentAlignment = Alignment.Center
        ) {
            MyraaOrb3D(
                state = orbState,
                size = 62.dp,
                amplitude = amplitude
            )
        }
    }
}

@Composable
private fun NavItem(
    route: String,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalNovaColors.current
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) colors.accent else colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (isSelected) colors.accent else colors.textSecondary
        )
    }
}
