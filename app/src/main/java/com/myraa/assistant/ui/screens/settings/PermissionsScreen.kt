package com.myraa.assistant.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.myraa.assistant.offline.AutostartHelper
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky

@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current

    var micGranted by remember { mutableStateOf(false) }
    var contactsGranted by remember { mutableStateOf(false) }
    var callGranted by remember { mutableStateOf(false) }
    var notifGranted by remember { mutableStateOf(false) }
    var batteryOptimized by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }
    var accessibilityGranted by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        callGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        batteryOptimized = AutostartHelper.isIgnoringBatteryOptimizations(context)
        overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        accessibilityGranted = try {
            val clazz = Class.forName("com.myraa.assistant.agent.MyraaAccessibilityService")
            val method = clazz.getMethod("isEnabled", Context::class.java)
            method.invoke(null, context) as Boolean
        } catch (_: Exception) { false }
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    val reqMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
    ) {
        NovaTopBar(title = "App Permissions", onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PermissionCard(
                title = "Microphone",
                description = "Required for Gemini Live and offline 'Hey Myraa' wake word.",
                icon = Icons.Default.Mic,
                isGranted = micGranted,
                onGrant = {
                    reqMultipleLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            )

            PermissionCard(
                title = "Contacts",
                description = "Used to look up names when you ask to call or message someone.",
                icon = Icons.Default.Contacts,
                isGranted = contactsGranted,
                onGrant = {
                    reqMultipleLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                }
            )

            PermissionCard(
                title = "Phone Calls",
                description = "Allows Myraa to directly initiate calls when requested.",
                icon = Icons.Default.Call,
                isGranted = callGranted,
                onGrant = {
                    reqMultipleLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    title = "Notifications",
                    description = "Keeps voice session controls and offline wake indicator visible.",
                    icon = Icons.Default.Notifications,
                    isGranted = notifGranted,
                    onGrant = {
                        reqMultipleLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                )
            }

            PermissionCard(
                title = "Floating Bubble Overlay",
                description = "Shows Myraa's interactive orb over other apps without opening the full screen.",
                icon = Icons.Default.BubbleChart,
                isGranted = overlayGranted,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try { context.startActivity(intent) } catch (_: Exception) {}
                    }
                }
            )

            PermissionCard(
                title = "Accessibility Automation",
                description = "Enables reading screen text, auto-scroll, gestures, and hands-free control of apps.",
                icon = Icons.Default.Tune,
                isGranted = accessibilityGranted,
                onGrant = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }
            )

            PermissionCard(
                title = "Background Battery Exemption",
                description = "Prevents Android from putting Myraa to sleep when the screen is locked.",
                icon = Icons.Default.BatteryAlert,
                isGranted = batteryOptimized,
                onGrant = {
                    AutostartHelper.requestIgnoreBatteryOptimizations(context)
                }
            )

            PermissionCard(
                title = "Default Digital Assistant",
                description = "Launch Myraa by holding power button or home button.",
                icon = Icons.Default.RecordVoiceOver,
                isGranted = false,
                onGrant = {
                    val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {}
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            NovaButton(
                text = "Open App System Settings",
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                type = NovaButtonType.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val colors = LocalNovaColors.current
    NovaGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (isGranted) colors.success.copy(alpha = 0.2f) else colors.surfaceRaised, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) colors.success else colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }

            if (!isGranted) {
                NovaButton(
                    text = "Grant",
                    onClick = onGrant,
                    type = NovaButtonType.PRIMARY
                )
            } else {
                Text(
                    text = "Active ✓",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.success,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
