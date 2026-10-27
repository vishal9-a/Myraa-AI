package com.myraa.assistant.ui.screens.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaOrb
import com.myraa.assistant.ui.components.NovaOrbStyle
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.fx.MyraaOrb3D
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.NovaOrbState

@Composable
fun OrbStyleScreen(
    appState: NovaAppState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current

    var previewState by remember { mutableStateOf(NovaOrbState.IDLE) }
    val currentStyle = remember(appState.orbStyle) {
        try { NovaOrbStyle.valueOf(appState.orbStyle) } catch (_: Exception) { NovaOrbStyle.NOVA }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NovaTopBar(title = "Orb Visual Presence", onBack = onNavigateBack)

        Spacer(modifier = Modifier.height(16.dp))

        // State toggle chips (Idle / Listening / Speaking)
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(NovaOrbState.IDLE, NovaOrbState.LISTENING, NovaOrbState.SPEAKING).forEach { state ->
                FilterChip(
                    selected = previewState == state,
                    onClick = { previewState = state },
                    label = { Text(state.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent,
                        selectedLabelColor = colors.onAccent,
                        containerColor = colors.surfaceRaised,
                        labelColor = colors.textPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Large Live Preview of selected orb
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentStyle == NovaOrbStyle.NOVA) {
                MyraaOrb3D(
                    state = previewState,
                    size = 230.dp,
                    amplitude = if (previewState == NovaOrbState.SPEAKING) 0.5f else 0f
                )
            } else {
                NovaOrb(
                    state = previewState,
                    size = 220.dp,
                    style = currentStyle,
                    amplitude = if (previewState == NovaOrbState.SPEAKING) 0.5f else 0f
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "SELECT VISUAL STYLE",
            style = MaterialTheme.typography.labelMedium,
            color = colors.accent,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Carousel of 7 orb styles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NovaOrbStyle.values().forEach { style ->
                val isSelected = style == currentStyle
                NovaGlassCard(
                    modifier = Modifier
                        .width(140.dp)
                        .scale(if (isSelected) 1.05f else 1.0f)
                        .clickable {
                            appState.applyOrbStyle(context, style.name)
                        },
                    shape = RoundedCornerShape(20.dp),
                    elevation = if (isSelected) 10.dp else 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NovaOrb(
                            state = previewState,
                            size = 72.dp,
                            style = style
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) colors.accent else colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
