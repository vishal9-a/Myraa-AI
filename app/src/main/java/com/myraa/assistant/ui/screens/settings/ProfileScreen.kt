package com.myraa.assistant.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.license.AccessKeyManager
import com.myraa.assistant.ui.components.AccessKeySection
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTextField
import com.myraa.assistant.ui.components.NovaTopBar
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.depthSurface
import com.myraa.assistant.ui.theme.nightSky
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    appState: NovaAppState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current
    val scope = rememberCoroutineScope()

    var userNameInput by remember { mutableStateOf(appState.userName) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .verticalScroll(rememberScrollState())
    ) {
        NovaTopBar(title = "Your Profile", onBack = onNavigateBack)

        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(colors.accent.copy(alpha = 0.2f), CircleShape)
                    .depthSurface(CircleShape, elevation = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👤", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            NovaGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Your Preferred Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Myraa will address you with this name during voice conversations.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = userNameInput,
                        onValueChange = {
                            userNameInput = it
                            appState.applyUserName(context, it)
                        },
                        placeholder = "e.g., Alex, Rahul, Priya"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Access Key Section (direct flavor or safe stub in play flavor)
            AccessKeySection(onActivateClick = { showKeyDialog = true })
        }
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Activate Pro Access Key", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        "Enter your Myraa license key to unlock direct edition features.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NovaTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = "MYRAA-PRO-XXXX"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showKeyDialog = false
                        scope.launch {
                            AccessKeyManager.activate(context, keyInput)
                        }
                    }
                ) { Text("Activate", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfaceRaised
        )
    }
}
