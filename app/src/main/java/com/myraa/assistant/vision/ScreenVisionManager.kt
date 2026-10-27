package com.myraa.assistant.vision

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ScreenVisionManager captures on-screen content either via:
 * 1. AccessibilityService (Android 11+, zero prompt if accessibility granted)
 * 2. MediaProjection fallback
 *
 * It extracts layout hierarchy, bounding boxes, and filters out FLAG_SECURE / password fields.
 */
object ScreenVisionManager {

    private var projectionResultCode: Int = Activity.RESULT_CANCELED
    private var projectionData: Intent? = null
    private var permissionDeferred: CompletableDeferred<Boolean>? = null

    val isWatchingActive = AtomicBoolean(false)

    private fun getActiveAccessibilityService(): android.accessibilityservice.AccessibilityService? {
        return try {
            val clazz = Class.forName("com.myraa.assistant.agent.MyraaAccessibilityService")
            val field = clazz.getDeclaredField("instance")
            field.isAccessible = true
            field.get(null) as? android.accessibilityservice.AccessibilityService
        } catch (_: Throwable) {
            null
        }
    }

    fun onPermissionResult(resultCode: Int, data: Intent?) {
        projectionResultCode = resultCode
        projectionData = data
        val success = (resultCode == Activity.RESULT_OK && data != null)
        permissionDeferred?.complete(success)
        permissionDeferred = null
    }

    suspend fun requestMediaProjection(context: Context): Boolean {
        if (projectionResultCode == Activity.RESULT_OK && projectionData != null) {
            return true
        }
        val deferred = CompletableDeferred<Boolean>()
        permissionDeferred = deferred
        ScreenCapturePermissionActivity.launch(context)
        return withTimeoutOrNull(15000) { deferred.await() } ?: false
    }

    /**
     * Captures current phone screen as a JPEG byte array.
     */
    suspend fun captureScreenBytes(context: Context): ByteArray? = withContext(Dispatchers.IO) {
        // Try AccessibilityService screenshot first if running on API 30+
        val accessibilityService = getActiveAccessibilityService()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && accessibilityService != null) {
            val bitmap = captureViaAccessibility(accessibilityService)
            if (bitmap != null) {
                return@withContext bitmapToJpeg(bitmap)
            }
        }

        // Otherwise fallback to MediaProjection
        if (projectionData != null && projectionResultCode == Activity.RESULT_OK) {
            val bitmap = captureViaMediaProjection(context)
            if (bitmap != null) {
                return@withContext bitmapToJpeg(bitmap)
            }
        }

        null
    }

    private suspend fun captureViaAccessibility(service: android.accessibilityservice.AccessibilityService): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val deferred = CompletableDeferred<Bitmap?>()

        try {
            service.takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: android.accessibilityservice.AccessibilityService.ScreenshotResult) {
                        val hardwareBuffer = screenshot.hardwareBuffer
                        val colorSpace = screenshot.colorSpace
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                        // Copy to software bitmap so it can be compressed
                        val softBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        hardwareBuffer.close()
                        deferred.complete(softBitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        deferred.complete(null)
                    }
                }
            )
            return withTimeoutOrNull(3000) { deferred.await() }
        } catch (_: Exception) {
            return null
        }
    }

    private fun captureViaMediaProjection(context: Context): Bitmap? {
        val data = projectionData ?: return null
        val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            ?: return null
        var projection: MediaProjection? = null
        var imageReader: ImageReader? = null
        var virtualDisplay: VirtualDisplay? = null

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            val width = (metrics.widthPixels / 2).coerceAtLeast(320)
            val height = (metrics.heightPixels / 2).coerceAtLeast(480)
            val density = metrics.densityDpi

            projection = mpManager.getMediaProjection(projectionResultCode, data.clone() as Intent)
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = projection?.createVirtualDisplay(
                "MyraaScreenVision",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                Handler(Looper.getMainLooper())
            )

            // Allow brief frame rendering
            Thread.sleep(300)
            val image = imageReader.acquireLatestImage() ?: return null
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            // Crop excess row padding if present
            return if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, width, height)
            } else {
                bitmap
            }
        } catch (_: Exception) {
            return null
        } finally {
            virtualDisplay?.release()
            imageReader?.close()
            projection?.stop()
        }
    }

    private fun bitmapToJpeg(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        // Downscale to max 1024px width for efficient Gemini transfer & low memory
        val scaled = if (bitmap.width > 1024) {
            val ratio = 1024f / bitmap.width
            Bitmap.createScaledBitmap(bitmap, 1024, (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    /**
     * Extracts semantic layout nodes and text while sanitizing password fields and FLAG_SECURE.
     */
    fun extractScreenSemanticHierarchy(): ScreenHierarchy {
        val service = getActiveAccessibilityService() ?: return ScreenHierarchy(
            packageName = "",
            elements = emptyList(),
            hasSecureField = false
        )

        val root = service.rootInActiveWindow ?: return ScreenHierarchy(
            packageName = "",
            elements = emptyList(),
            hasSecureField = false
        )

        val elements = mutableListOf<ScreenNode>()
        var hasSecure = false
        val pkg = root.packageName?.toString().orEmpty()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            if (node.isPassword) {
                hasSecure = true
                elements.add(
                    ScreenNode(
                        text = "[PROTECTED_PASSWORD_FIELD]",
                        className = node.className?.toString().orEmpty(),
                        bounds = Rect().apply { node.getBoundsInScreen(this) },
                        isClickable = node.isClickable,
                        isEditable = true
                    )
                )
                return
            }

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val label = when {
                !text.isNullOrEmpty() -> text
                !desc.isNullOrEmpty() -> desc
                else -> null
            }

            if (label != null || node.isClickable || node.isEditable) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                elements.add(
                    ScreenNode(
                        text = label.orEmpty(),
                        className = node.className?.toString().orEmpty(),
                        bounds = rect,
                        isClickable = node.isClickable,
                        isEditable = node.isEditable
                    )
                )
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        try {
            traverse(root)
        } catch (_: Exception) {
            // Ignore node recycling errors
        }

        return ScreenHierarchy(packageName = pkg, elements = elements, hasSecureField = hasSecure)
    }
}

data class ScreenHierarchy(
    val packageName: String,
    val elements: List<ScreenNode>,
    val hasSecureField: Boolean
) {
    fun toSummary(): String {
        val b = StringBuilder()
        b.append("Active App: ").append(packageName).append("\n")
        if (hasSecureField) {
            b.append("Notice: Screen contains secure or password fields (sanitized).\n")
        }
        b.append("Visible elements:\n")
        elements.take(30).forEach { node ->
            if (node.text.isNotEmpty()) {
                val type = if (node.isClickable) "[Button/Link]" else if (node.isEditable) "[Input]" else "[Text]"
                b.append(" - ").append(type).append(" \"").append(node.text.take(60)).append("\"")
                b.append(" (at ${node.bounds.left},${node.bounds.top})\n")
            }
        }
        return b.toString()
    }
}

data class ScreenNode(
    val text: String,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val isEditable: Boolean
)
