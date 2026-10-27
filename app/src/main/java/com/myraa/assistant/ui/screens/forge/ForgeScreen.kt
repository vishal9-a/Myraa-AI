package com.myraa.assistant.ui.screens.forge

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import com.myraa.assistant.forge.ForgeController
import com.myraa.assistant.forge.ForgePhase
import com.myraa.assistant.forge.ForgeStage
import com.myraa.assistant.ui.components.NovaButton
import com.myraa.assistant.ui.components.NovaButtonType
import com.myraa.assistant.ui.components.NovaGlassCard
import com.myraa.assistant.ui.components.NovaTextField
import com.myraa.assistant.ui.theme.LocalNovaColors
import com.myraa.assistant.ui.theme.depthSurface
import kotlinx.coroutines.delay
import java.io.File

class ForgeJsBridge(
    private val onReady: () -> Unit,
    private val onRevealFinished: () -> Unit,
    private val onWebGLFailed: () -> Unit
) {
    @JavascriptInterface
    fun onReady() { onReady.invoke() }

    @JavascriptInterface
    fun onRevealFinished() { onRevealFinished.invoke() }

    @JavascriptInterface
    fun onWebGLFailed() { onWebGLFailed.invoke() }
}

@Composable
fun ForgeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalNovaColors.current
    val uiState by ForgeController.uiState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editPromptText by remember { mutableStateOf("") }
    var showToolbar by remember { mutableStateOf(true) }

    BackHandler {
        if (uiState.phase == ForgePhase.BUILDING) {
            ForgeController.cancel()
        }
        onNavigateBack()
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        when (uiState.phase) {
            ForgePhase.BUILDING -> {
                // 3D Reactor View
                ForgeWebGlScene(
                    progress = uiState.progress,
                    stage = uiState.stage.displayName
                )

                // Quiet HUD
                ForgeHud(
                    prompt = uiState.prompt,
                    stage = uiState.stage,
                    progress = uiState.progress,
                    chars = uiState.metrics.chars,
                    sections = uiState.sections.size,
                    tokensPerSec = uiState.metrics.tokensPerSec,
                    elapsedSec = uiState.metrics.elapsedSec,
                    onCancel = {
                        ForgeController.cancel()
                        onNavigateBack()
                    }
                )
            }

            ForgePhase.DONE -> {
                // Generated website preview full-screen
                val file = uiState.file
                val html = remember(file) { file?.readText().orEmpty() }

                GeneratedSiteView(html = html)

                // Auto-hiding result toolbar at top
                AnimatedVisibility(
                    visible = showToolbar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ResultToolbar(
                        onBack = onNavigateBack,
                        onShare = { shareWebsite(context, file) },
                        onOpenBrowser = { openInExternalBrowser(context, file) },
                        onEdit = { showEditDialog = true },
                        onRebuild = { ForgeController.retry(context) }
                    )
                }

                // Small floating toggle button to restore toolbar
                if (!showToolbar) {
                    IconButton(
                        onClick = { showToolbar = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .background(colors.surfaceGlass, CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Show Tools", tint = colors.accent)
                    }
                }

                LaunchedEffect(showToolbar) {
                    if (showToolbar) {
                        delay(4500)
                        showToolbar = false
                    }
                }
            }

            ForgePhase.FAILED, ForgePhase.CANCELLED -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NovaGlassCard(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (uiState.phase == ForgePhase.FAILED) "Generation Failed" else "Generation Cancelled",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.error ?: "The website generation was interrupted.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                NovaButton(
                                    text = "Back",
                                    onClick = onNavigateBack,
                                    type = NovaButtonType.SECONDARY
                                )
                                NovaButton(
                                    text = "Retry",
                                    onClick = { ForgeController.retry(context) },
                                    type = NovaButtonType.PRIMARY
                                )
                            }
                        }
                    }
                }
            }

            ForgePhase.IDLE -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Website Forge is idle", color = colors.textSecondary)
                }
            }
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Website", color = colors.textPrimary) },
                text = {
                    Column {
                        Text(
                            "Describe what changes you want Myraa to make to this website:",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NovaTextField(
                            value = editPromptText,
                            onValueChange = { editPromptText = it },
                            placeholder = "e.g., Change hero color to purple and add an order button"
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEditDialog = false
                            if (editPromptText.isNotBlank()) {
                                ForgeController.edit(context, editPromptText)
                            }
                        }
                    ) { Text("Apply Changes", color = colors.accent) }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surfaceRaised
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ForgeWebGlScene(
    progress: Int,
    stage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var useFallback by remember { mutableStateOf(false) }

    if (useFallback) {
        ForgeFallbackScene(progress = progress, stage = stage)
        return
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                setBackgroundColor(0xFF06050E.toInt())
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false

                val assetLoader = WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                    .build()

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean = true
                }

                addJavascriptInterface(
                    ForgeJsBridge(
                        onReady = {},
                        onRevealFinished = {},
                        onWebGLFailed = { useFallback = true }
                    ),
                    "ForgeBridge"
                )

                loadUrl("https://appassets.androidplatform.net/assets/forge/forge.html")
            }
        },
        update = { webView ->
            webView.evaluateJavascript("if (window.Forge) { Forge.update($progress, '$stage'); }", null)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let {
                it.evaluateJavascript("if (window.Forge) { Forge.dispose(); }", null)
                it.destroy()
            }
        }
    }
}

@Composable
fun ForgeFallbackScene(progress: Int, stage: String) {
    val colors = LocalNovaColors.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = 90.dp.toPx()
        drawCircle(
            color = colors.accent.copy(alpha = 0.25f),
            radius = radius * (1f + (progress / 100f) * 0.3f),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = colors.lotus,
            radius = radius * 0.6f,
            center = Offset(cx, cy)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GeneratedSiteView(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.setGeolocationEnabled(false)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(intent)
                        } catch (_: Exception) {}
                        return true
                    }
                }
                loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
        }
    )
}

@Composable
fun ForgeHud(
    prompt: String,
    stage: ForgeStage,
    progress: Int,
    chars: Int,
    sections: Int,
    tokensPerSec: Float,
    elapsedSec: Long,
    onCancel: () -> Unit
) {
    val colors = LocalNovaColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top prompt chip
        Box(
            modifier = Modifier
                .padding(top = 28.dp)
                .depthSurface(RoundedCornerShape(20.dp), elevation = 4.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "⚡ $prompt",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                maxLines = 1
            )
        }

        // Bottom status card
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stage.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.accent
                        )
                        Text(
                            text = "Stage ${stage.ordinal + 1} of ${ForgeStage.values().size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = colors.accent,
                    trackColor = colors.surfaceRaised
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${chars / 1024} KB", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Text("$sections sections", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Text("${String.format("%.1f", tokensPerSec)} tok/s", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Text("${elapsedSec}s", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                NovaButton(
                    text = "Cancel Build",
                    onClick = onCancel,
                    type = NovaButtonType.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ResultToolbar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenBrowser: () -> Unit,
    onEdit: () -> Unit,
    onRebuild: () -> Unit
) {
    val colors = LocalNovaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .depthSurface(RoundedCornerShape(28.dp), elevation = 12.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Website", tint = colors.accent)
            }
            IconButton(onClick = onRebuild) {
                Icon(Icons.Default.Refresh, contentDescription = "Rebuild", tint = colors.textPrimary)
            }
            IconButton(onClick = onOpenBrowser) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = colors.textPrimary)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = colors.textPrimary)
            }
        }
    }
}

private fun shareWebsite(context: Context, file: File?) {
    if (file == null || !file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Generated Website"))
    } catch (_: Exception) {}
}

private fun openInExternalBrowser(context: Context, file: File?) {
    if (file == null || !file.exists()) return
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
