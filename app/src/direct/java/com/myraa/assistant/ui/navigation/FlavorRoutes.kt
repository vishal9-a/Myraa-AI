package com.myraa.assistant.ui.navigation

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.nightSky

fun NavGraphBuilder.registerFlavorRoutes(navController: NavController) {
    composable("accessibility_disclosure") {
        val context = LocalContext.current
        val colors = LocalNovaColors.current

        Column(
            modifier = Modifier.fillMaxSize().nightSky(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NovaTopBar(title = "Accessibility Disclosure", onBack = { navController.popBackStack() })

            NovaGlassCard(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Why Myraa uses AccessibilityService",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent
                    )
                    Text(
                        text = "Myraa's Direct edition uses the Android AccessibilityService API exclusively when you ask it to interact with other apps (such as reading screen text, tapping buttons, typing messages, or controlling apps hands-free).\n\n" +
                                "• Zero personal data is logged, transmitted, or uploaded.\n" +
                                "• All actions are initiated strictly upon your explicit voice request.\n" +
                                "• Sensitive actions (like posting content or sending messages) always require your confirmation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    NovaButton(
                        text = "Open Accessibility Settings",
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        type = NovaButtonType.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
