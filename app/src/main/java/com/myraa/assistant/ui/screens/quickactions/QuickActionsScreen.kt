package com.myraa.assistant.ui.screens.quickactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myraa.assistant.offline.IntentRouter
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class QuickActionTile(
    val title: String,
    val examplePhrase: String,
    val icon: ImageVector,
    val actionCommand: String
)

@Composable
fun QuickActionsScreen(
    onNavigateBack: () -> Unit,
    onOpenForge: () -> Unit,
    onTriggerVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current

    val tiles = listOf(
        QuickActionTile("3D Website Forge", "“Myraa, ek cafe ki website banao”", Icons.Default.Build, "forge"),
        QuickActionTile("Voice Assistant", "“Start Live Voice Mode”", Icons.Default.Mic, "voice"),
        QuickActionTile("Call Contact", "“Mummy ko phone lagao”", Icons.Default.Call, "call"),
        QuickActionTile("WhatsApp Message", "“Send WhatsApp to Rahul”", Icons.Default.Message, "message"),
        QuickActionTile("Toggle Flashlight", "“Flashlight on karo”", Icons.Default.FlashlightOn, "torch"),
        QuickActionTile("Set Alarm", "“Subah 7 baje ka alarm lagao”", Icons.Default.Alarm, "alarm"),
        QuickActionTile("Launch YouTube", "“YouTube kholo”", Icons.Default.PlayArrow, "youtube"),
        QuickActionTile("Set Timer", "“5 minute ka timer shuru karo”", Icons.Default.Timer, "timer")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
    ) {
        NovaTopBar(title = "Quick Actions", onBack = onNavigateBack)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tiles) { tile ->
                NovaGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (tile.actionCommand) {
                                "forge" -> onOpenForge()
                                "voice" -> onTriggerVoice()
                                "torch" -> {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        IntentRouter.route(context, "flashlight on")
                                    }
                                }
                                "youtube" -> {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        IntentRouter.route(context, "open youtube")
                                    }
                                }
                                "timer" -> {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        IntentRouter.route(context, "5 minute ka timer")
                                    }
                                }
                                else -> onTriggerVoice()
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    elevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(colors.accent.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tile.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tile.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tile.examplePhrase,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
