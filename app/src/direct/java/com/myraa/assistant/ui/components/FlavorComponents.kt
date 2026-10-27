package com.myraa.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myraa.assistant.agent.MyraaAccessibilityService
import com.myraa.assistant.license.AccessKeyManager
import com.myraa.assistant.ui.theme.LocalNovaColors

@Composable
fun AccessKeyBanner(onActivateClick: () -> Unit) {
    val info by AccessKeyManager.info.collectAsState()
    val colors = LocalNovaColors.current

    if (!info.active || info.blocked) {
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(colors.error.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = colors.error, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Access Key Required", style = MaterialTheme.typography.titleMedium, color = colors.error)
                        Text(info.statusText, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
                NovaButton(
                    text = "Activate",
                    onClick = onActivateClick,
                    type = NovaButtonType.PRIMARY
                )
            }
        }
    }
}

@Composable
fun AccessKeySection(onActivateClick: () -> Unit) {
    val info by AccessKeyManager.info.collectAsState()
    val colors = LocalNovaColors.current

    NovaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Licensing Plan", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(info.statusText, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            NovaButton(
                text = if (info.active) "Manage" else "Enter Key",
                onClick = onActivateClick,
                type = NovaButtonType.SECONDARY
            )
        }
    }
}

@Composable
fun AccessibilitySettingsRow(onNavigateToDisclosure: () -> Unit) {
    val context = LocalContext.current
    val isEnabled = MyraaAccessibilityService.isEnabled(context)
    NovaSettingsRow(
        title = "Screen Control & Automation",
        subtitle = if (isEnabled) "Active (Accessibility Service Connected)" else "Tap to enable screen control & social agent",
        icon = Icons.Default.Accessibility,
        onClick = onNavigateToDisclosure
    )
}
