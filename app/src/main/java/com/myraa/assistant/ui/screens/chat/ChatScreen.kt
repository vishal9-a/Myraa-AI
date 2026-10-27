package com.myraa.assistant.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myraa.assistant.data.ApiKeyStore
import com.myraa.assistant.data.NovaAppState
import com.myraa.assistant.data.PersonalityRepository
import com.myraa.assistant.forge.ForgeController
import com.myraa.assistant.forge.ForgeIntent
import com.myraa.assistant.offline.LocalLLM
import com.myraa.assistant.offline.ModeManager
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTypingDots
import com.myraa.assistant.ui.fx.MyraaOrb3D
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.depthSurface
import com.myraa.assistant.ui.theme.nightSky
import com.myraa.assistant.voice.ChatPromptBuilder
import com.myraa.assistant.voice.ChatReplyResult
import com.myraa.assistant.voice.ChatTurn
import com.myraa.assistant.voice.GeminiTextClient
import com.myraa.assistant.voice.NovaOrbState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class ChatMessage(
    val id: String = "msg_${System.currentTimeMillis()}_${(0..9999).random()}",
    val text: String,
    val isUser: Boolean,
    val isForgePreview: Boolean = false,
    val isError: Boolean = false
)

@Composable
fun ChatScreen(
    appState: NovaAppState,
    onNavigateBack: () -> Unit,
    onOpenForge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Namaste! I am ${appState.assistantName}. How can I assist you today?",
                isUser = false
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val q = inputText.trim()
        if (q.isEmpty() || isTyping) return
        inputText = ""

        messages.add(ChatMessage(text = q, isUser = true))

        // Check for Forge creation intent
        val forgePrompt = ForgeIntent.extract(q)
        if (forgePrompt != null) {
            messages.add(
                ChatMessage(
                    text = "Opening the 3D Website Forge to build your site: “$forgePrompt”...",
                    isUser = false,
                    isForgePreview = true
                )
            )
            ForgeController.start(context, forgePrompt)
            return
        }

        isTyping = true

        scope.launch {
            if (ModeManager.shouldUseOfflineBrain()) {
                delay(400)
                val reply = LocalLLM.generateOfflineReply(
                    context = context,
                    query = q,
                    personality = appState.personality,
                    assistantName = appState.assistantName
                )
                streamReplyWordByWord(reply) { partial ->
                    if (messages.last().isUser) {
                        messages.add(ChatMessage(text = partial, isUser = false))
                    } else {
                        val lastIdx = messages.size - 1
                        messages[lastIdx] = messages[lastIdx].copy(text = partial)
                    }
                }
                isTyping = false
            } else {
                val apiKey = ApiKeyStore.getKey(context)
                if (apiKey.isBlank()) {
                    isTyping = false
                    messages.add(
                        ChatMessage(
                            text = "Please configure your Gemini API Key in Settings to chat online.",
                            isUser = false,
                            isError = true
                        )
                    )
                    return@launch
                }

                val history = messages.filter { !it.isError && !it.isForgePreview }
                    .map { ChatTurn(isUser = it.isUser, text = it.text) }

                val sysPrompt = ChatPromptBuilder.build(
                    personality = appState.personality,
                    language = appState.language,
                    assistantName = appState.assistantName
                )

                val result = withContext(Dispatchers.IO) {
                    GeminiTextClient.reply(apiKey, sysPrompt, history)
                }

                when (result) {
                    is ChatReplyResult.Success -> {
                        streamReplyWordByWord(result.text) { partial ->
                            if (messages.last().isUser) {
                                messages.add(ChatMessage(text = partial, isUser = false))
                            } else {
                                val lastIdx = messages.size - 1
                                messages[lastIdx] = messages[lastIdx].copy(text = partial)
                            }
                        }
                    }
                    is ChatReplyResult.Error -> {
                        messages.add(
                            ChatMessage(
                                text = "Error: ${result.message}",
                                isUser = false,
                                isError = true
                            )
                        )
                    }
                }
                isTyping = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                MyraaOrb3D(
                    state = if (isTyping) NovaOrbState.THINKING else NovaOrbState.IDLE,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = appState.assistantName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isTyping) "Thinking..." else if (ModeManager.shouldUseOfflineBrain()) "Offline Mode" else "Here to help",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent
                    )
                }
            }
        }

        // Messages LazyColumn
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    onSpeak = { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) },
                    onCopy = { text ->
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("Myraa", text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onOpenForge = onOpenForge
                )
            }
            if (isTyping) {
                item {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        NovaTypingDots()
                    }
                }
            }
        }

        // Glass Floating Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .depthSurface(RoundedCornerShape(28.dp), elevation = 8.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "Ask Myraa or type 'build website'...",
                        color = colors.textTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                singleLine = false,
                maxLines = 4
            )

            IconButton(
                onClick = { sendMessage() },
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.accent, CircleShape)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = colors.onAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private suspend fun streamReplyWordByWord(fullText: String, onUpdate: (String) -> Unit) {
    val words = fullText.split(" ")
    val sb = StringBuilder()
    for (w in words) {
        if (sb.isNotEmpty()) sb.append(" ")
        sb.append(w)
        onUpdate(sb.toString())
        delay(20)
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onOpenForge: () -> Unit
) {
    val colors = LocalNovaColors.current
    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .depthSurface(
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 20.dp
                    ),
                    elevation = 4.dp
                )
                .background(
                    if (message.isUser) colors.accent.copy(alpha = 0.16f) else colors.surfaceGlass
                )
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isError) colors.error else colors.textPrimary
                )

                if (message.isForgePreview) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceRaised, RoundedCornerShape(12.dp))
                            .clickable { onOpenForge() }
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "⚡ View Website in 3D Forge Reactor →",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!message.isUser && !message.isForgePreview && !message.isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { onSpeak(message.text) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { onCopy(message.text) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
