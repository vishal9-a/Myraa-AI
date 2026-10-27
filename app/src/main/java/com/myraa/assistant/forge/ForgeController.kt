package com.myraa.assistant.forge

import android.content.Context
import com.myraa.assistant.data.ApiKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class ForgePhase {
    IDLE,
    BUILDING,
    DONE,
    FAILED,
    CANCELLED
}

data class ForgeMetrics(
    val chars: Int = 0,
    val sections: Int = 0,
    val elapsedSec: Long = 0,
    val tokensPerSec: Float = 0f
)

data class ForgeUiState(
    val phase: ForgePhase = ForgePhase.IDLE,
    val prompt: String = "",
    val isEdit: Boolean = false,
    val stage: ForgeStage = ForgeStage.PLANNING,
    val progress: Int = 0,
    val metrics: ForgeMetrics = ForgeMetrics(),
    val sections: List<String> = emptyList(),
    val log: List<String> = emptyList(),
    val logSeq: Int = 0,
    val buildId: String = "",
    val file: File? = null,
    val error: String? = null
)

sealed class ForgeStartResult(val message: String) {
    object Started : ForgeStartResult("Opening the Website Forge now.")
    object MissingKey : ForgeStartResult("Please enter your Gemini API key in Settings first.")
    object Busy : ForgeStartResult("The Forge is already actively building a website.")
    object NothingToEdit : ForgeStartResult("No previous website found to edit.")
}

object ForgeController {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null
    private val partialHtmlBuilder = StringBuilder()

    private val _uiState = MutableStateFlow(ForgeUiState())
    val uiState: StateFlow<ForgeUiState> = _uiState.asStateFlow()

    private val _pendingOpen = MutableStateFlow(false)
    val pendingOpen: StateFlow<Boolean> = _pendingOpen.asStateFlow()

    fun requestOpen() {
        _pendingOpen.value = true
    }

    fun consumePendingOpen() {
        _pendingOpen.value = false
    }

    fun partialHtml(): String = partialHtmlBuilder.toString()

    fun start(context: Context, prompt: String): ForgeStartResult {
        if (_uiState.value.phase == ForgePhase.BUILDING) {
            return ForgeStartResult.Busy
        }

        val key = ApiKeyStore.getKey(context)
        if (key.isBlank()) {
            return ForgeStartResult.MissingKey
        }

        val cleanPrompt = prompt.trim().ifEmpty { "A modern luxury coffee cafe website with 3D elements" }
        partialHtmlBuilder.clear()

        _uiState.value = ForgeUiState(
            phase = ForgePhase.BUILDING,
            prompt = cleanPrompt,
            isEdit = false,
            stage = ForgeStage.PLANNING,
            progress = 10,
            buildId = "build_${System.currentTimeMillis()}"
        )
        _pendingOpen.value = true

        val startTime = System.currentTimeMillis()
        val client = WebsiteForgeClient(context, key)

        activeJob?.cancel()
        activeJob = scope.launch {
            client.streamBuild(cleanPrompt, isEdit = false).collect { event ->
                when (event) {
                    is ForgeStreamEvent.Chunk -> {
                        partialHtmlBuilder.append(event.text)
                        val text = partialHtmlBuilder.toString()
                        val stage = ForgeStageDetector.detect(text)
                        val sections = ForgeStageDetector.extractSections(text)
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000
                        val estTokens = text.length / 4
                        val tokensPerSec = if (elapsed > 0) estTokens.toFloat() / elapsed else 0f

                        val calcProgress = (stage.basePercent + (text.length / 400).coerceAtMost(10)).coerceIn(10, 95)

                        _uiState.value = _uiState.value.copy(
                            stage = stage,
                            progress = calcProgress,
                            sections = sections,
                            metrics = ForgeMetrics(
                                chars = text.length,
                                sections = sections.size,
                                elapsedSec = elapsed,
                                tokensPerSec = tokensPerSec
                            )
                        )
                    }

                    is ForgeStreamEvent.Complete -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ForgePhase.DONE,
                            stage = ForgeStage.COMPLETE,
                            progress = 100,
                            file = event.file
                        )
                    }

                    is ForgeStreamEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ForgePhase.FAILED,
                            error = event.message
                        )
                    }
                }
            }
        }

        return ForgeStartResult.Started
    }

    fun edit(context: Context, changePrompt: String): ForgeStartResult {
        val currentFile = _uiState.value.file
        if (currentFile == null || !currentFile.exists()) {
            return ForgeStartResult.NothingToEdit
        }
        val currentHtml = currentFile.readText()
        val key = ApiKeyStore.getKey(context)
        if (key.isBlank()) return ForgeStartResult.MissingKey

        partialHtmlBuilder.clear()
        _uiState.value = ForgeUiState(
            phase = ForgePhase.BUILDING,
            prompt = changePrompt,
            isEdit = true,
            stage = ForgeStage.PLANNING,
            progress = 15,
            buildId = "edit_${System.currentTimeMillis()}"
        )
        _pendingOpen.value = true

        val client = WebsiteForgeClient(context, key)
        val startTime = System.currentTimeMillis()

        activeJob?.cancel()
        activeJob = scope.launch {
            client.streamBuild(changePrompt, isEdit = true, currentHtml = currentHtml).collect { event ->
                when (event) {
                    is ForgeStreamEvent.Chunk -> {
                        partialHtmlBuilder.append(event.text)
                        val text = partialHtmlBuilder.toString()
                        val stage = ForgeStageDetector.detect(text)
                        val sections = ForgeStageDetector.extractSections(text)
                        val elapsed = (System.currentTimeMillis() - startTime) / 1000
                        val estTokens = text.length / 4
                        val tokensPerSec = if (elapsed > 0) estTokens.toFloat() / elapsed else 0f

                        _uiState.value = _uiState.value.copy(
                            stage = stage,
                            progress = (stage.basePercent + 5).coerceIn(15, 95),
                            sections = sections,
                            metrics = ForgeMetrics(
                                chars = text.length,
                                sections = sections.size,
                                elapsedSec = elapsed,
                                tokensPerSec = tokensPerSec
                            )
                        )
                    }
                    is ForgeStreamEvent.Complete -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ForgePhase.DONE,
                            stage = ForgeStage.COMPLETE,
                            progress = 100,
                            file = event.file
                        )
                    }
                    is ForgeStreamEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ForgePhase.FAILED,
                            error = event.message
                        )
                    }
                }
            }
        }
        return ForgeStartResult.Started
    }

    fun retry(context: Context) {
        val p = _uiState.value.prompt
        start(context, p)
    }

    fun cancel() {
        activeJob?.cancel()
        activeJob = null
        _uiState.value = _uiState.value.copy(
            phase = ForgePhase.CANCELLED
        )
    }

    fun reset() {
        cancel()
        partialHtmlBuilder.clear()
        _uiState.value = ForgeUiState()
    }

    fun openSaved(file: File) {
        _uiState.value = ForgeUiState(
            phase = ForgePhase.DONE,
            prompt = file.nameWithoutExtension,
            stage = ForgeStage.COMPLETE,
            progress = 100,
            file = file
        )
        _pendingOpen.value = true
    }
}
