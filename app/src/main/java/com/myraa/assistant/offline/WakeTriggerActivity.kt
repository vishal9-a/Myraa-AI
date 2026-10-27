package com.myraa.assistant.offline

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myraa.assistant.data.NovaPreferences
import com.myraa.assistant.data.PersonalityRepository
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaVoiceVisualizer
import com.myraa.assistant.ui.fx.MyraaOrb3D
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.NovaTheme
import com.myraa.assistant.ui.theme.depthSurface
import com.myraa.assistant.voice.NovaOrbState
import com.myraa.assistant.voice.Personality
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WakeTriggerActivity : ComponentActivity() {

    private var offlineSTT: MyraaOfflineSTT? = null
    private var offlineTTS: OfflineTTS? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        offlineSTT = MyraaOfflineSTT(this)
        offlineTTS = OfflineTTS(this)

        setContent {
            val themeMode = NovaPreferences.getString(this, NovaPreferences.Keys.THEME_MODE, "DARK")
            NovaTheme(mode = themeMode) {
                WakeTriggerScreen(
                    onDismiss = { finish() },
                    onListen = { onPartial, onFinal, onError ->
                        offlineSTT?.startListening(
                            languageLocale = "hi-IN",
                            onPartial = onPartial,
                            onFinal = onFinal,
                            onError = onError
                        )
                    },
                    onSpeak = { text, personality, onDone ->
                        offlineTTS?.speak(text, personality, onDone = onDone)
                    },
                    onStopSpeech = {
                        offlineSTT?.stopListening()
                        offlineTTS?.stop()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        offlineSTT?.stopListening()
        offlineTTS?.release()
        super.onDestroy()
    }
}

@Composable
fun WakeTriggerScreen(
    onDismiss: () -> Unit,
    onListen: (onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onSpeak: (text: String, personality: Personality, onDone: () -> Unit) -> Unit,
    onStopSpeech: () -> Unit
) {
    val colors = LocalNovaColors.current
    val scope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("Listening... (Boliyen)") }
    var userSpokenText by remember { mutableStateOf("") }
    var assistantResponseText by remember { mutableStateOf("") }
    var orbState by remember { mutableStateOf(NovaOrbState.LISTENING) }
    var personality by remember { mutableStateOf(Personality.NORMAL) }

    DisposableEffect(Unit) {
        onDispose { onStopSpeech() }
    }

    LaunchedEffect(Unit) {
        onListen(
            { partial ->
                userSpokenText = partial
                statusText = "Hearing..."
            },
            { finalQuery ->
                userSpokenText = finalQuery
                orbState = NovaOrbState.THINKING
                statusText = "Processing command..."

                scope.launch {
                    val result = IntentRouter.route(com.myraa.assistant.MainActivity.appContext ?: return@launch, finalQuery)
                    when (result) {
                        is IntentResult.Handled -> {
                            assistantResponseText = result.responseText
                            orbState = NovaOrbState.SPEAKING
                            statusText = "Myraa"
                            onSpeak(result.responseText, personality) {
                                scope.launch {
                                    delay(1800)
                                    onDismiss()
                                }
                            }
                        }
                        is IntentResult.NeedsBrain -> {
                            val reply = LocalLLM.generateOfflineReply(
                                context = com.myraa.assistant.MainActivity.appContext ?: return@launch,
                                query = finalQuery,
                                personality = personality
                            )
                            assistantResponseText = reply
                            orbState = NovaOrbState.SPEAKING
                            statusText = "Myraa (Offline)"
                            onSpeak(reply, personality) {
                                scope.launch {
                                    delay(2500)
                                    onDismiss()
                                }
                            }
                        }
                    }
                }
            },
            { errorMsg ->
                statusText = errorMsg
                orbState = NovaOrbState.ERROR
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom sheet modal stage
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .background(colors.surfaceRaised, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (ModeManager.shouldUseOfflineBrain()) "OFFLINE HANDS-FREE" else "ONLINE ASSISTANT",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hero 3D Orb
                MyraaOrb3D(
                    state = orbState,
                    size = 140.dp,
                    amplitude = if (orbState == NovaOrbState.LISTENING) 0.35f else 0f
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.accent,
                    fontStyle = FontStyle.Italic
                )

                if (userSpokenText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "“$userSpokenText”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                if (assistantResponseText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = assistantResponseText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                NovaButton(
                    text = "Close",
                    onClick = onDismiss,
                    type = NovaButtonType.SECONDARY,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
            }
        }
    }
}
