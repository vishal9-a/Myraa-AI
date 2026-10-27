package com.myraa.assistant.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.LocalNovaReducedMotion
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.Personality

@Composable
fun PersonalityScreen(
    appState: NovaAppState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current
    val reducedMotion = LocalNovaReducedMotion.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
    ) {
        NovaTopBar(title = "Personality", onBack = onNavigateBack)

        Text(
            text = "Changes take effect immediately on active voice sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.accent,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(Personality.values()) { p ->
                val isSelected = p == appState.personality
                val cardScale = if (isSelected && !reducedMotion) 1.04f else 1.0f

                Box(
                    modifier = Modifier
                        .scale(cardScale)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { appState.applyPersonality(context, p) }
                ) {
                    NovaGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) Modifier.border(2.dp, colors.accent, RoundedCornerShape(20.dp))
                                else Modifier
                            ),
                        elevation = if (isSelected) 12.dp else 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = when (p) {
                                    Personality.NORMAL -> "✨"
                                    Personality.FRIENDLY -> "💖"
                                    Personality.PROFESSIONAL -> "💼"
                                    Personality.FUNNY -> "😄"
                                    Personality.COMPANION -> "🛡️"
                                    Personality.GF_MODE -> "🥰"
                                    Personality.TEACHER -> "🎓"
                                    Personality.DEVELOPER -> "💻"
                                    Personality.HUNGRY -> "🍕"
                                    Personality.NAUTANKI -> "🎭"
                                },
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = p.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) colors.accent else colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = p.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
