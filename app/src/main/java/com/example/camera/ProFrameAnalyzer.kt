package com.example.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.model.FrameTelemetry
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class ProFrameAnalyzer(
    private val shouldAnalyzeProvider: () -> Boolean,
    private val zebraThresholdProvider: () -> Int,
    private val minIntervalMsProvider: () -> Long,
    private val onTelemetryUpdated: (FrameTelemetry) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalysisTimeMs = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        val minInterval = minIntervalMsProvider()
        if (!shouldAnalyzeProvider() || now - lastAnalysisTimeMs < minInterval) {
            image.close()
            return
        }
        lastAnalysisTimeMs = now

        try {
            val planes = image.planes
            if (planes.isNotEmpty()) {
                val yPlane = planes[0]
                val uPlane = if (planes.size > 1) planes[1] else null
                val vPlane = if (planes.size > 2) planes[2] else null

                val width = image.width
                val height = image.height
                val yBuffer = yPlane.buffer
                val yRowStride = yPlane.rowStride
                val yPixelStride = yPlane.pixelStride

                val uBuffer = uPlane?.buffer
                val vBuffer = vPlane?.buffer
                val uvRowStride = uPlane?.rowStride ?: 0
                val uvPixelStride = uPlane?.pixelStride ?: 1

                val lumaBins = IntArray(64)
                val redBins = IntArray(64)
                val greenBins = IntArray(64)
                val blueBins = IntArray(64)

                val gridW = 48
                val gridH = 36
                val overlayCells = IntArray(gridW * gridH)
                val lumaGrid = IntArray(gridW * gridH)

                val zebraThresholdLuma = ((zebraThresholdProvider().coerceIn(70, 100) / 100f) * 255f).toInt()
                var totalLuma = 0L
                var sampleCount = 0
                var highlightClips = 0
                var shadowClips = 0

                for (gy in 0 until gridH) {
                    val py = (gy * height) / gridH
                    val yRowOffset = py * yRowStride
                    val uvRowOffset = (py / 2) * uvRowStride

                    for (gx in 0 until gridW) {
                        val px = (gx * width) / gridW
                        val yIdx = yRowOffset + px * yPixelStride
                        val yVal = if (yIdx >= 0 && yIdx < yBuffer.limit()) {
                            yBuffer.get(yIdx).toInt() and 0xFF
                        } else {
                            128
                        }

                        var uVal = 128
                        var vVal = 128
                        if (uBuffer != null && vBuffer != null) {
                            val uvIdx = uvRowOffset + (px / 2) * uvPixelStride
                            if (uvIdx >= 0 && uvIdx < uBuffer.limit() && uvIdx < vBuffer.limit()) {
                                uVal = uBuffer.get(uvIdx).toInt() and 0xFF
                                vVal = vBuffer.get(uvIdx).toInt() and 0xFF
                            }
                        }

                        // Fast YUV to RGB approximation for RGB Histogram
                        val c = yVal - 16
                        val d = uVal - 128
                        val e = vVal - 128
                        val r = ((298 * c + 409 * e + 128) shr 8).coerceIn(0, 255)
                        val g = ((298 * c - 100 * d - 208 * e + 128) shr 8).coerceIn(0, 255)
                        val b = ((298 * c + 516 * d + 128) shr 8).coerceIn(0, 255)

                        lumaBins[(yVal * 63) / 255]++
                        redBins[(r * 63) / 255]++
                        greenBins[(g * 63) / 255]++
                        blueBins[(b * 63) / 255]++

                        totalLuma += yVal
                        sampleCount++
                        if (yVal >= zebraThresholdLuma) highlightClips++
                        if (yVal <= 12) shadowClips++

                        lumaGrid[gy * gridW + gx] = yVal
                    }
                }

                // Second pass on the 48x36 grid to classify Zebra, Focus Peaking edges, and False Color IRE
                for (gy in 0 until gridH) {
                    for (gx in 0 until gridW) {
                        val idx = gy * gridW + gx
                        val luma = lumaGrid[idx]

                        // Sobel/Laplacian gradient with neighbors for Focus Peaking
                        var edgeStrength = 0
                        if (gx in 1 until (gridW - 1) && gy in 1 until (gridH - 1)) {
                            val left = lumaGrid[gy * gridW + (gx - 1)]
                            val right = lumaGrid[gy * gridW + (gx + 1)]
                            val up = lumaGrid[(gy - 1) * gridW + gx]
                            val down = lumaGrid[(gy + 1) * gridW + gx]
                            edgeStrength = abs(right - left) + abs(down - up)
                        }

                        // Encode flags into cell integer:
                        // Lower 8 bits = luma (0..255)
                        // Bit 8 (256) = Zebra clipping active
                        // Bit 9 (512) = Focus Peaking sharp edge active
                        var packed = luma and 0xFF
                        if (luma >= zebraThresholdLuma) {
                            packed = packed or 256
                        }
                        if (edgeStrength >= 42) {
                            packed = packed or 512
                        }
                        overlayCells[idx] = packed
                    }
                }

                val avgLuma = if (sampleCount > 0) (totalLuma / sampleCount).toInt() else 128
                val hiPct = if (sampleCount > 0) (highlightClips * 100f) / sampleCount else 0f
                val loPct = if (sampleCount > 0) (shadowClips * 100f) / sampleCount else 0f

                // Estimate dynamic range in stops based on non-empty histogram span
                val minNonZero = lumaBins.indexOfFirst { it > 2 }.coerceAtLeast(1)
                val maxNonZero = lumaBins.indexOfLast { it > 2 }.coerceAtLeast(minNonZero + 1)
                val drStops = (ln((maxNonZero.toFloat() / minNonZero.toFloat()).coerceAtLeast(1.5f)) / ln(2f) + 5.5f)
                    .coerceIn(4.0f, 12.8f)

                onTelemetryUpdated(
                    FrameTelemetry(
                        lumaBins = lumaBins,
                        redBins = redBins,
                        greenBins = greenBins,
                        blueBins = blueBins,
                        averageLuma = avgLuma,
                        highlightClipPercent = hiPct,
                        shadowClipPercent = loPct,
                        dynamicRangeStopsEstimate = drStops,
                        gridWidth = gridW,
                        gridHeight = gridH,
                        overlayCells = overlayCells
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore transient buffer close exceptions during lens switch
        } finally {
            image.close()
        }
    }

    companion object {
        /**
         * Generates real FrameTelemetry from an existing Bitmap (used when inspecting
         * studio calibration frames or when running in environments without an active camera stream).
         */
        fun analyzeBitmap(bitmap: Bitmap, zebraThresholdPercent: Int): FrameTelemetry {
            val lumaBins = IntArray(64)
            val redBins = IntArray(64)
            val greenBins = IntArray(64)
            val blueBins = IntArray(64)

            val gridW = 48
            val gridH = 36
            val overlayCells = IntArray(gridW * gridH)
            val lumaGrid = IntArray(gridW * gridH)
            val zebraThresholdLuma = ((zebraThresholdPercent.coerceIn(70, 100) / 100f) * 255f).toInt()

            var totalLuma = 0L
            var sampleCount = 0
            var hiClips = 0
            var loClips = 0

            val w = max(1, bitmap.width)
            val h = max(1, bitmap.height)

            for (gy in 0 until gridH) {
                val py = min(h - 1, (gy * h) / gridH)
                for (gx in 0 until gridW) {
                    val px = min(w - 1, (gx * w) / gridW)
                    val pixel = bitmap.getPixel(px, py)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                    val clampedY = yVal.coerceIn(0, 255)

                    lumaBins[(clampedY * 63) / 255]++
                    redBins[(r * 63) / 255]++
                    greenBins[(g * 63) / 255]++
                    blueBins[(b * 63) / 255]++

                    totalLuma += clampedY
                    sampleCount++
                    if (clampedY >= zebraThresholdLuma) hiClips++
                    if (clampedY <= 12) loClips++
                    lumaGrid[gy * gridW + gx] = clampedY
                }
            }

            for (gy in 0 until gridH) {
                for (gx in 0 until gridW) {
                    val idx = gy * gridW + gx
                    val luma = lumaGrid[idx]
                    var edgeStrength = 0
                    if (gx in 1 until (gridW - 1) && gy in 1 until (gridH - 1)) {
                        val left = lumaGrid[gy * gridW + (gx - 1)]
                        val right = lumaGrid[gy * gridW + (gx + 1)]
                        val up = lumaGrid[(gy - 1) * gridW + gx]
                        val down = lumaGrid[(gy + 1) * gridW + gx]
                        edgeStrength = abs(right - left) + abs(down - up)
                    }
                    var packed = luma and 0xFF
                    if (luma >= zebraThresholdLuma) packed = packed or 256
                    if (edgeStrength >= 38) packed = packed or 512
                    overlayCells[idx] = packed
                }
            }

            return FrameTelemetry(
                lumaBins = lumaBins,
                redBins = redBins,
                greenBins = greenBins,
                blueBins = blueBins,
                averageLuma = if (sampleCount > 0) (totalLuma / sampleCount).toInt() else 128,
                highlightClipPercent = if (sampleCount > 0) (hiClips * 100f) / sampleCount else 0f,
                shadowClipPercent = if (sampleCount > 0) (loClips * 100f) / sampleCount else 0f,
                dynamicRangeStopsEstimate = 9.6f,
                gridWidth = gridW,
                gridHeight = gridH,
                overlayCells = overlayCells
            )
        }
    }
}
