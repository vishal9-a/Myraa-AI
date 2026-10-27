package com.myraa.assistant.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraVisionManager provides ephemeral single-frame camera sight for Myraa.
 * Features:
 * - Explicit active HUD indicator state (never secretly runs).
 * - Ephemeral RAM buffer (never saved to disk or persistent storage).
 * - Front camera ("Myraa, mala bagh") or back camera ("look at what is in front of me").
 */
object CameraVisionManager {

    val isCameraActive = AtomicBoolean(false)
    val lastActiveTimestamp = AtomicBoolean(false)

    /**
     * Ephemeral camera frame capture using CameraX ImageAnalysis.
     * Automatically binds, captures a single frame to RAM, unbinds, and returns JPEG bytes.
     */
    suspend fun captureEphemeralFrame(
        context: Context,
        useFrontCamera: Boolean = true
    ): ByteArray? = withContext(Dispatchers.Main) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = try {
            val deferred = CompletableDeferred<ProcessCameraProvider>()
            cameraProviderFuture.addListener({
                try {
                    deferred.complete(cameraProviderFuture.get())
                } catch (e: Exception) {
                    deferred.completeExceptionally(e)
                }
            }, ContextCompat.getMainExecutor(context))
            deferred.await()
        } catch (_: Exception) {
            return@withContext null
        }

        isCameraActive.set(true)
        val frameDeferred = CompletableDeferred<ByteArray?>()
        val executor = Executors.newSingleThreadExecutor()

        // Synthetic standalone lifecycle owner so we don't need a visible Activity on screen
        val lifecycleOwner = object : LifecycleOwner {
            private val registry = LifecycleRegistry(this).apply {
                currentState = Lifecycle.State.RESUMED
            }
            override val lifecycle: Lifecycle get() = registry
            fun destroy() {
                registry.currentState = Lifecycle.State.DESTROYED
            }
        }

        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            if (!frameDeferred.isCompleted) {
                try {
                    val jpeg = imageProxyToJpeg(imageProxy, useFrontCamera)
                    frameDeferred.complete(jpeg)
                } catch (e: Exception) {
                    frameDeferred.complete(null)
                }
            }
            imageProxy.close()
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)

            val result = withTimeoutOrNull(4000) {
                frameDeferred.await()
            }
            return@withContext result
        } catch (_: Exception) {
            return@withContext null
        } finally {
            isCameraActive.set(false)
            try {
                cameraProvider.unbindAll()
                lifecycleOwner.destroy()
                executor.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    private fun imageProxyToJpeg(image: ImageProxy, isFrontCamera: Boolean): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
        val imageBytes = out.toByteArray()

        // Decode to handle rotation
        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees == 0 && !isFrontCamera) {
            return imageBytes
        }

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f) // Mirror front camera natural view
        }

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val finalOut = ByteArrayOutputStream()
        // Downscale to max 800px width for fast Gemini network transfer
        val scaled = if (rotated.width > 800) {
            val ratio = 800f / rotated.width
            Bitmap.createScaledBitmap(rotated, 800, (rotated.height * ratio).toInt(), true)
        } else {
            rotated
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, finalOut)
        return finalOut.toByteArray()
    }
}
