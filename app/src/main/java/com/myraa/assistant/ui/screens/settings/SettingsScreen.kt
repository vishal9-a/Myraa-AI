package com.myraa.assistant.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.data.ApiKeyStore
import com.myraa.assistant.data.LanguagePreference
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.data.NovaPreferences
import com.myraa.assistant.offline.AutostartHelper
import com.myraa.assistant.offline.ModelDownloadManager
import com.myraa.assistant.offline.ModeManager
import com.myraa.assistant.offline.OfflineTTS
import com.myraa.assistant.offline.TTSModelManager
import com.myraa.assistant.offline.VoskModelManager
import com.myraa.assistant.offline.VoiceProfile
import com.myraa.assistant.offline.WakeSensitivity
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaSectionHeader
import com.myraa.assistant.ui.components.NovaSettingsRow
import com.myraa.assistant.ui.components.NovaTextField
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalBottomBarInset
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.depthSurface
import com.myraa.assistant.ui.theme.nightSky

@Composable
fun SettingsScreen(
    appState: NovaAppState,
    onNavigateBack: () -> Unit,
    onNavigateToPersonality: () -> Unit,
    onNavigateToOrbStyle: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToMemory: () -> Unit = {},
    onNavigateToAbout: () -> Unit,
    onNavigateToDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current
    val bottomInset = LocalBottomBarInset.current

    var apiKeyText by remember { mutableStateOf(ApiKeyStore.getKey(context)) }
    var assistantNameText by remember { mutableStateOf(appState.assistantName) }

    val isHandsFreeEnabled by ModeManager.isHandsFreeEnabled.collectAsState()
    val isAlwaysOffline by ModeManager.isAlwaysOffline.collectAsState()
    val sensitivity by ModeManager.sensitivity.collectAsState()

    val downloadProgress by ModelDownloadManager.downloadProgress.collectAsState()
    val isDownloadingModel by ModelDownloadManager.isDownloading.collectAsState()
    val isModelInstalled by ModelDownloadManager.isModelInstalled.collectAsState()

    val ttsDownloadProgress by TTSModelManager.downloadProgress.collectAsState()
    val isTtsDownloading by TTSModelManager.isDownloading.collectAsState()
    val isTtsModelInstalled by TTSModelManager.isModelInstalled.collectAsState()

    val voskDownloadProgress by VoskModelManager.downloadProgress.collectAsState()
    val isVoskDownloading by VoskModelManager.isDownloading.collectAsState()
    val isVoskModelInstalled by VoskModelManager.isModelInstalled.collectAsState()
    val selectedSttLang by VoskModelManager.selectedLanguage.collectAsState()

    var showAutostartDialog by remember { mutableStateOf(false) }

    val offlineTts = remember { OfflineTTS(context) }
    DisposableEffect(Unit) {
        onDispose {
            offlineTts.release()
        }
    }

    var selectedVoiceProfile by remember {
        mutableStateOf(
            VoiceProfile.fromId(
                NovaPreferences.getString(context, "voice_profile_id", VoiceProfile.CUTE_GIRL.id)
            )
        )
    }

    LaunchedEffect(Unit) {
        ModeManager.init(context)
        ModelDownloadManager.checkInstalled(context)
        TTSModelManager.checkInstalled(context)
        VoskModelManager.checkInstalled(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomInset)
    ) {
        NovaTopBar(
            title = "Settings",
            onBack = onNavigateBack
        )

        // Profile hero card
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.accent.copy(alpha = 0.2f), CircleShape)
                        .depthSurface(CircleShape, elevation = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "💎",
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = appState.assistantName,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Voice: ${selectedVoiceProfile.displayName.split("·")[0].trim()} · ${appState.language.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Section 1: Offline Hands-Free Mode (Wake Word)
        NovaSectionHeader("OFFLINE HANDS-FREE WAKE WORD")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 8.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "Hands-Free Wake Word",
                    subtitle = "Say 'Myraa' anytime to activate listening without opening the app",
                    icon = Icons.Default.RecordVoiceOver,
                    checked = isHandsFreeEnabled,
                    onCheckedChange = { ModeManager.setHandsFreeEnabled(context, it) }
                )

                NovaSettingsRow(
                    title = "Wake Word Sensitivity",
                    subtitle = sensitivity.label,
                    icon = Icons.Default.Speed,
                    value = sensitivity.name,
                    onClick = {
                        val next = when (sensitivity) {
                            WakeSensitivity.LOW -> WakeSensitivity.MEDIUM
                            WakeSensitivity.MEDIUM -> WakeSensitivity.HIGH
                            WakeSensitivity.HIGH -> WakeSensitivity.LOW
                        }
                        ModeManager.setSensitivity(context, next)
                    }
                )

                NovaSettingsRow(
                    title = "Always Offline Mode",
                    subtitle = "Run speech, logic, and commands purely on-device (Zero Internet)",
                    icon = Icons.Default.WifiOff,
                    checked = isAlwaysOffline,
                    onCheckedChange = { ModeManager.setAlwaysOffline(context, it) }
                )

                NovaSettingsRow(
                    title = "Autostart & Battery Exemption",
                    subtitle = if (AutostartHelper.isIgnoringBatteryOptimizations(context)) "Optimized (Unrestricted background)" else "Tap to allow 24x7 background listening",
                    icon = Icons.Default.BatteryAlert,
                    onClick = { showAutostartDialog = true }
                )
            }
        }

        // Section 2: Offline Cute Girl Voice (TTS)
        NovaSectionHeader("OFFLINE CUTE GIRL VOICE (TTS)")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 9.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Voice Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Neural TTS streaming with cute micro-pauses between sentences (<200ms latency)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                VoiceProfile.entries.forEach { profile ->
                    val isSelected = selectedVoiceProfile == profile
                    val cardBorderColor = if (isSelected) colors.accent else colors.surfaceRaised

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.accent.copy(alpha = 0.08f) else colors.surface.copy(alpha = 0.4f))
                            .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedVoiceProfile = profile
                                offlineTts.setVoiceProfile(profile)
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedVoiceProfile = profile
                                    offlineTts.setVoiceProfile(profile)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.accent,
                                    unselectedColor = colors.textTertiary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = profile.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            NovaButton(
                                text = "Preview",
                                onClick = {
                                    offlineTts.previewVoice(profile)
                                },
                                type = NovaButtonType.SECONDARY
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Piper / VITS Neural Model Local Cache Manager
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceRaised.copy(alpha = 0.6f))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Piper / VITS Neural Model",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isTtsModelInstalled) "Installed in internal storage: files/models/tts/ (Offline Ready)"
                                    else if (isTtsDownloading) "Downloading neural model: ${(ttsDownloadProgress * 100).toInt()}%"
                                    else "Download local Piper female neural model (MR/HI/EN) for 100% offline TTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                            if (!isTtsModelInstalled && !isTtsDownloading) {
                                Spacer(modifier = Modifier.width(8.dp))
                                NovaButton(
                                    text = "Install",
                                    onClick = { TTSModelManager.startDownload(context) },
                                    type = NovaButtonType.SECONDARY
                                )
                            }
                        }

                        if (isTtsDownloading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { ttsDownloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = colors.accent,
                                trackColor = colors.surfaceRaised
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Offline Speech-to-Text (Vosk / Whisper.cpp)
        NovaSectionHeader("OFFLINE STT (VOSK / WHISPER.CPP)")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 9.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vosk / Whisper Offline STT Pipeline",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isVoskModelInstalled) "Installed: files/models/stt/vosk/$selectedSttLang/ (Offline Ready)"
                            else if (isVoskDownloading) "Downloading acoustic model: ${(voskDownloadProgress * 100).toInt()}%"
                            else "Download Kaldi/Vosk small model for 100% offline speech commands without internet",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }

                    if (!isVoskModelInstalled && !isVoskDownloading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        NovaButton(
                            text = "Install",
                            onClick = { VoskModelManager.startDownload(context) },
                            type = NovaButtonType.SECONDARY
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Language Selector for STT Model
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("mr" to "Marathi (मराठी)", "hi" to "Hindi (हिंदी)", "en" to "English (IN)").forEach { (code, label) ->
                        val isSelected = selectedSttLang == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surfaceRaised)
                                .border(1.dp, if (isSelected) colors.accent else colors.surfaceRaised, RoundedCornerShape(8.dp))
                                .clickable {
                                    VoskModelManager.setLanguage(context, code)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) colors.accent else colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (isVoskDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { voskDownloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = colors.accent,
                        trackColor = colors.surfaceRaised
                    )
                }
            }
        }

        // Section 4: On-Device Brain (Gemma 2B / MediaPipe)
        NovaSectionHeader("ON-DEVICE BRAIN (OFFLINE GEMMA)")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 9.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "On-Device Gemma 2B LLM",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isModelInstalled) "Installed on internal storage (Ready offline)"
                            else if (isDownloadingModel) "Downloading over Wi-Fi (${(downloadProgress * 100).toInt()}%)"
                            else "Download MediaPipe LLM weights for advanced conversational reasoning without internet",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        if (isDownloadingModel) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = colors.accent,
                                trackColor = colors.surfaceRaised
                            )
                        }
                    }

                    if (!isModelInstalled && !isDownloadingModel) {
                        Spacer(modifier = Modifier.width(8.dp))
                        NovaButton(
                            text = "Download",
                            onClick = { ModelDownloadManager.startDownload(context) },
                            type = NovaButtonType.SECONDARY
                        )
                    }
                }
            }
        }

        // Section 4: Gemini API Configuration (Online Mode)
        NovaSectionHeader("ONLINE GEMINI LIVE CONFIGURATION")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 10.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Gemini API Key",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Used when internet is available. App automatically switches to offline pipeline when offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                NovaTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        ApiKeyStore.saveKey(context, it)
                    },
                    placeholder = "AIzaSy..."
                )
            }
        }

        // Section 5: Customization & Assistant Features
        NovaSectionHeader("CUSTOMIZATION")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 12.dp
        ) {
            Column {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Assistant Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    NovaTextField(
                        value = assistantNameText,
                        onValueChange = {
                            assistantNameText = it
                            appState.applyAssistantName(context, it)
                        },
                        placeholder = "e.g., Myraa"
                    )
                }

                NovaSettingsRow(
                    title = "Personality",
                    subtitle = appState.personality.displayName,
                    icon = Icons.Default.Face,
                    onClick = onNavigateToPersonality
                )

                NovaSettingsRow(
                    title = "Language",
                    subtitle = appState.language.displayName,
                    icon = Icons.Default.Language,
                    onClick = {
                        val next = when (appState.language) {
                            LanguagePreference.AUTO -> LanguagePreference.MARATHI
                            LanguagePreference.MARATHI -> LanguagePreference.HINDI
                            LanguagePreference.HINDI -> LanguagePreference.HINGLISH
                            LanguagePreference.HINGLISH -> LanguagePreference.ENGLISH
                            LanguagePreference.ENGLISH -> LanguagePreference.AUTO
                        }
                        appState.applyLanguage(context, next)
                    }
                )

                NovaSettingsRow(
                    title = "3D Orb Style",
                    subtitle = appState.orbStyle,
                    icon = Icons.Default.Palette,
                    onClick = onNavigateToOrbStyle
                )

                NovaSettingsRow(
                    title = "Permanent Memory",
                    subtitle = "Review and edit facts remembered about you",
                    icon = Icons.Default.Bookmark,
                    onClick = onNavigateToMemory
                )

                var isBubbleActive by remember { mutableStateOf(com.myraa.assistant.overlay.FloatingBubbleService.instance != null) }
                NovaSettingsRow(
                    title = "Floating Bubble Companion",
                    subtitle = "Keep Myraa floating over apps with quick voice bubble",
                    icon = Icons.Default.BubbleChart,
                    checked = isBubbleActive,
                    onCheckedChange = {
                        isBubbleActive = it
                        if (it) {
                            com.myraa.assistant.overlay.FloatingBubbleService.start(context)
                        } else {
                            com.myraa.assistant.overlay.FloatingBubbleService.stop(context)
                        }
                    }
                )

                NovaSettingsRow(
                    title = "Dark Theme",
                    subtitle = "Pearl of light aesthetic",
                    icon = Icons.Default.Settings,
                    checked = appState.themeMode == "DARK",
                    onCheckedChange = { appState.applyThemeMode(context, if (it) "DARK" else "LIGHT") }
                )

                NovaSettingsRow(
                    title = "Reduced Motion",
                    subtitle = "Disable 3D tilt and particle spins",
                    icon = Icons.Default.Tune,
                    checked = appState.reducedMotion,
                    onCheckedChange = { appState.applyReducedMotion(context, it) }
                )
            }
        }

        // Section 6: System & Diagnostics
        NovaSectionHeader("SYSTEM")
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 14.dp
        ) {
            Column {
                NovaSettingsRow(
                    title = "Permissions Status",
                    subtitle = "Microphone, Overlay, Accessibility & Battery",
                    icon = Icons.Default.Security,
                    onClick = onNavigateToPermissions
                )

                NovaSettingsRow(
                    title = "Voice Pipeline Debugger",
                    subtitle = "Inspect roundtrip latency, VAD & offline status",
                    icon = Icons.Default.Speed,
                    onClick = onNavigateToDebug
                )

                NovaSettingsRow(
                    title = "About Myraa",
                    subtitle = "Version 1.0 · Developer: Vishal Bargal",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAutostartDialog) {
        val guide = AutostartHelper.getOEMAutostartGuide(context)
        AlertDialog(
            onDismissRequest = { showAutostartDialog = false },
            title = { Text(guide.brand, color = colors.textPrimary) },
            text = {
                Column {
                    Text(guide.instructions, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Clicking 'Open Settings' will navigate directly to your phone's power management page.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAutostartDialog = false
                        AutostartHelper.requestIgnoreBatteryOptimizations(context)
                        AutostartHelper.openOEMAutostartSettings(context)
                    }
                ) { Text("Open Settings", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showAutostartDialog = false }) {
                    Text("Close", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfaceRaised
        )
    }
}
