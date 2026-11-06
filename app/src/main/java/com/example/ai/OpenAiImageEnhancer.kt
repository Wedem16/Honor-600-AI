package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.model.AiEnhanceSettings
import com.example.model.AiSceneProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

sealed class AiEnhanceResult {
    data class Success(
        val enhancedImageBytes: ByteArray,
        val sceneSummary: String
    ) : AiEnhanceResult()

    data class Failure(
        val userFriendlyError: String
    ) : AiEnhanceResult()
}

/**
 * 100% On-Device Auto AI Imaging Engine (HONOR AI Camera style).
 * Performs local scene recognition, adaptive HDR shadow/highlight tone mapping,
 * color matrix grading, and micro-contrast sharpening directly on-device with zero external APIs.
 */
class OnDeviceAutoAiEnhancer {

    suspend fun enhancePhotoOnDevice(
        sourceFile: File,
        settings: AiEnhanceSettings,
        onStatusProgress: (String) -> Unit
    ): AiEnhanceResult = withContext(Dispatchers.Default) {
        try {
            coroutineContext.ensureActive()
            onStatusProgress("Analyzing 4K scene luminance & color distribution…")

            val sourceBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
                ?: return@withContext AiEnhanceResult.Failure("Unable to decode captured 4K photo for AI processing.")

            val stats = analyzeSceneStatistics(sourceBitmap)
            val effectiveProfile = if (settings.sceneProfile == AiSceneProfile.AUTO_SCENE) {
                stats.recommendedProfile
            } else {
                settings.sceneProfile
            }

            val detectedLabel = if (settings.sceneProfile == AiSceneProfile.AUTO_SCENE) {
                "Auto AI (${stats.detectedSceneName})"
            } else {
                effectiveProfile.displayName
            }

            coroutineContext.ensureActive()
            onStatusProgress("Applying $detectedLabel ISP tone mapping…")

            val strength = (settings.intensityPercent.coerceIn(10, 100)) / 100f
            val processedBitmap = applyComputationalIspPipeline(
                source = sourceBitmap,
                profile = effectiveProfile,
                stats = stats,
                strength = strength,
                applyHdrToneMap = settings.smartHdrToneMap,
                applySharpening = settings.detailSharpening
            )

            coroutineContext.ensureActive()
            onStatusProgress("Encoding 4K AI Enhanced JPEG…")

            val out = ByteArrayOutputStream()
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)

            AiEnhanceResult.Success(
                enhancedImageBytes = out.toByteArray(),
                sceneSummary = "$detectedLabel • ${settings.intensityPercent}% Strength"
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AiEnhanceResult.Failure(
                userFriendlyError = "On-device AI processing interrupted: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private data class SceneStats(
        val avgLuma: Float,
        val shadowRatio: Float,
        val highlightRatio: Float,
        val avgRed: Float,
        val avgGreen: Float,
        val avgBlue: Float,
        val detectedSceneName: String,
        val recommendedProfile: AiSceneProfile
    )

    private fun analyzeSceneStatistics(bitmap: Bitmap): SceneStats {
        val stepX = max(1, bitmap.width / 48)
        val stepY = max(1, bitmap.height / 36)
        var totalLuma = 0L
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var shadows = 0
        var highlights = 0
        var count = 0

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val px = bitmap.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                val luma = (r * 77 + g * 150 + b * 29) shr 8

                totalLuma += luma
                totalR += r
                totalG += g
                totalB += b
                if (luma < 45) shadows++
                if (luma > 215) highlights++
                count++
                x += stepX
            }
            y += stepY
        }

        val safeCount = count.coerceAtLeast(1).toFloat()
        val avgLuma = totalLuma / safeCount
        val avgR = totalR / safeCount
        val avgG = totalG / safeCount
        val avgB = totalB / safeCount
        val shadowRatio = shadows / safeCount
        val highlightRatio = highlights / safeCount

        val (sceneName, profile) = when {
            avgLuma < 72f || shadowRatio > 0.45f ->
                "Night & Low-Light HDR" to AiSceneProfile.NIGHT_VISION
            avgB > avgR * 1.15f && avgLuma > 110f ->
                "Blue Sky & Landscape" to AiSceneProfile.VIVID_HDR
            avgR > avgB * 1.18f && avgLuma in 85f..195f ->
                "Warm Portrait / Golden Hour" to AiSceneProfile.PORTRAIT_GLOW
            highlightRatio > 0.12f && shadowRatio > 0.18f ->
                "High-Dynamic-Range Backlit" to AiSceneProfile.VIVID_HDR
            else ->
                "Balanced Studio Optics" to AiSceneProfile.VIVID_HDR
        }

        return SceneStats(
            avgLuma = avgLuma,
            shadowRatio = shadowRatio,
            highlightRatio = highlightRatio,
            avgRed = avgR,
            avgGreen = avgG,
            avgBlue = avgB,
            detectedSceneName = sceneName,
            recommendedProfile = profile
        )
    }

    private fun applyComputationalIspPipeline(
        source: Bitmap,
        profile: AiSceneProfile,
        stats: SceneStats,
        strength: Float,
        applyHdrToneMap: Boolean,
        applySharpening: Boolean
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Stage 1: Profile-specific Saturation + Contrast + White-Balance ColorMatrix
        val saturationBoost = when (profile) {
            AiSceneProfile.AUTO_SCENE -> 1.0f + (0.28f * strength)
            AiSceneProfile.VIVID_HDR -> 1.0f + (0.36f * strength)
            AiSceneProfile.NATURAL_OPTIC -> 1.0f + (0.12f * strength)
            AiSceneProfile.PORTRAIT_GLOW -> 1.0f + (0.16f * strength)
            AiSceneProfile.NIGHT_VISION -> 1.0f + (0.24f * strength)
        }

        val contrastGain = when (profile) {
            AiSceneProfile.AUTO_SCENE -> 1.0f + (0.14f * strength)
            AiSceneProfile.VIVID_HDR -> 1.0f + (0.18f * strength)
            AiSceneProfile.NATURAL_OPTIC -> 1.0f + (0.08f * strength)
            AiSceneProfile.PORTRAIT_GLOW -> 1.0f + (0.09f * strength)
            AiSceneProfile.NIGHT_VISION -> 1.0f + (0.20f * strength)
        }

        val brightnessLift = when {
            profile == AiSceneProfile.NIGHT_VISION -> 22f * strength
            stats.avgLuma < 90f && applyHdrToneMap -> 15f * strength
            else -> 6f * strength
        }

        val satMatrix = ColorMatrix().apply { setSaturation(saturationBoost) }
        val offset = 128f * (1f - contrastGain) + brightnessLift
        val rScale = if (profile == AiSceneProfile.PORTRAIT_GLOW) contrastGain * (1f + 0.04f * strength) else contrastGain
        val bScale = if (profile == AiSceneProfile.VIVID_HDR) contrastGain * (1f + 0.05f * strength) else contrastGain

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                rScale, 0f, 0f, 0f, offset,
                0f, contrastGain, 0f, 0f, offset,
                0f, 0f, bScale, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(contrastMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(satMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Stage 2: Local Shadow-Lift & Highlight-Recovery Tone Curve LUT + Micro-Contrast
        if (applyHdrToneMap || applySharpening) {
            val lut = IntArray(256)
            val gamma = if (profile == AiSceneProfile.NIGHT_VISION) 0.78 else 0.88
            for (i in 0..255) {
                val norm = i / 255.0
                // S-curve + shadow lift
                val shadowLifted = norm.pow(gamma)
                // Soft knee highlight compression above 0.82
                val knee = if (shadowLifted > 0.82) {
                    0.82 + (shadowLifted - 0.82) * 0.72
                } else {
                    shadowLifted
                }
                val blended = norm * (1.0 - strength * 0.65) + knee * (strength * 0.65)
                lut[i] = (blended * 255.0).toInt().coerceIn(0, 255)
            }

            val rowPixels = IntArray(width)
            for (y in 0 until height) {
                output.getPixels(rowPixels, 0, width, 0, y, width, 1)
                for (x in 0 until width) {
                    val c = rowPixels[x]
                    val a = c and -0x1000000
                    var r = lut[(c shr 16) and 0xFF]
                    var g = lut[(c shr 8) and 0xFF]
                    var b = lut[c and 0xFF]

                    // Horizontal micro-contrast sharpening with neighbor pixel
                    if (applySharpening && x in 1 until (width - 1)) {
                        val prev = rowPixels[x - 1]
                        val prevG = (prev shr 8) and 0xFF
                        val delta = ((g - prevG) * 0.22f * strength).toInt()
                        r = min(255, max(0, r + delta))
                        g = min(255, max(0, g + delta))
                        b = min(255, max(0, b + delta))
                    }
                    rowPixels[x] = a or (r shl 16) or (g shl 8) or b
                }
                output.setPixels(rowPixels, 0, width, 0, y, width, 1)
            }
        }

        return output
    }
}
