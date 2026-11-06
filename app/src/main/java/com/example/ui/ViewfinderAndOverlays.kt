package com.example.ui

import android.annotation.SuppressLint
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.camera.ProFrameAnalyzer
import com.example.model.AspectRatioOption
import com.example.model.FlashOption
import com.example.model.FrameTelemetry
import com.example.model.GridOption
import com.example.model.ProOverlayType
import com.example.model.ThermalHealthLevel
import com.example.ui.theme.AmberOpticPrimary
import com.example.ui.theme.CinemaCrimsonError
import com.example.ui.theme.CyanAiSecondary
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.IvoryTextPrimary
import com.example.ui.theme.SilverTextSecondary
import com.example.viewmodel.CameraUiState
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max

class CameraCaptureBridge {
    var imageCapture: ImageCapture? = null

    suspend fun captureJpegBytes(context: android.content.Context): ByteArray? =
        suspendCoroutine { cont ->
            val capture = imageCapture
            if (capture == null) {
                cont.resume(null)
                return@suspendCoroutine
            }
            capture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val plane = image.planes.firstOrNull()
                            val buffer: ByteBuffer? = plane?.buffer
                            if (buffer != null) {
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                cont.resume(bytes)
                            } else {
                                cont.resume(null)
                            }
                        } catch (_: Exception) {
                            cont.resume(null)
                        } finally {
                            image.close()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resume(null)
                    }
                }
            )
        }
}

@OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("RestrictedApi")
@Composable
fun CameraViewfinderStage(
    uiState: CameraUiState,
    hasCameraPermission: Boolean,
    captureBridge: CameraCaptureBridge,
    onSwipeMode: (Int) -> Unit,
    onFlipCamera: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    onTapFocus: (Float, Float) -> Unit,
    onTelemetryUpdated: (FrameTelemetry) -> Unit,
    onToggleHistogram: (Boolean) -> Unit,
    onSelectProOverlay: (ProOverlayType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    // Bind CameraX Preview + ImageCapture + ImageAnalysis when permission is granted
    LaunchedEffect(
        hasCameraPermission,
        uiState.selectedLensIndex,
        uiState.currentMode.isVideoMode
    ) {
        if (!hasCameraPermission) return@LaunchedEffect
        try {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    provider.unbindAll()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    captureBridge.imageCapture = imgCapture

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                analysisExecutor,
                                ProFrameAnalyzer(
                                    shouldAnalyzeProvider = {
                                        uiState.currentMode.isProMode &&
                                            (uiState.proState.showHistogram || uiState.proState.activeOverlay != ProOverlayType.NONE)
                                    },
                                    zebraThresholdProvider = { uiState.proState.zebraThresholdPercent },
                                    minIntervalMsProvider = {
                                        if (uiState.thermalHealth.throttlesHeavyFeatures) 450L else 140L
                                    },
                                    onTelemetryUpdated = onTelemetryUpdated
                                )
                            )
                        }

                    val facing = uiState.activeLens?.lensFacing
                        ?: android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
                    val selector = if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    if (provider.hasCamera(selector)) {
                        boundCamera = provider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imgCapture,
                            analyzer
                        )
                    }
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (_: Exception) {
        }
    }

    // Apply live zoom, flash, and Camera2Interop Pro manual parameters to boundCamera
    LaunchedEffect(
        boundCamera,
        uiState.zoomRatio,
        uiState.flashOption,
        uiState.proState,
        uiState.stabilizationEnabled,
        uiState.videoFps
    ) {
        val cam = boundCamera ?: return@LaunchedEffect
        try {
            cam.cameraControl.setZoomRatio(uiState.zoomRatio.coerceAtLeast(1.0f))
            cam.cameraControl.enableTorch(uiState.flashOption == FlashOption.TORCH)

            captureBridge.imageCapture?.flashMode = when (uiState.flashOption) {
                FlashOption.ON -> ImageCapture.FLASH_MODE_ON
                FlashOption.AUTO -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }

            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            val optsBuilder = CaptureRequestOptions.Builder()

            val pro = uiState.proState
            val lens = uiState.activeLens
            if (uiState.currentMode.isProMode && lens?.supportsManualSensor == true &&
                (pro.iso != null || pro.shutterSpeedNs != null)
            ) {
                optsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
                )
                pro.iso?.let {
                    optsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it)
                }
                pro.shutterSpeedNs?.let {
                    optsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it)
                }
            } else {
                optsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                optsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    pro.exposureCompensationIndex
                )
                optsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_LOCK,
                    pro.aeLocked
                )
            }

            if (uiState.currentMode.isProMode) {
                optsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    pro.whiteBalancePreset.awbModeConstant
                )
                if (pro.focusDistanceDiopters != null && lens?.supportsVariableFocus == true) {
                    optsBuilder.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF
                    )
                    optsBuilder.setCaptureRequestOption(
                        CaptureRequest.LENS_FOCUS_DISTANCE,
                        pro.focusDistanceDiopters
                    )
                } else {
                    optsBuilder.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                }
            }

            if (uiState.currentMode.isVideoMode) {
                val fps = uiState.videoFps.fps
                optsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(fps, fps)
                )
            }

            if (lens?.supportsOpticalStabilization == true) {
                optsBuilder.setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    if (uiState.stabilizationEnabled) {
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                    } else {
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
                    }
                )
            }
            if (lens?.supportsVideoStabilization == true) {
                optsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    if (uiState.stabilizationEnabled) {
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    } else {
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                    }
                )
            }

            camera2Control.setCaptureRequestOptions(optsBuilder.build())
        } catch (_: Exception) {
        }
    }

    var cumulativeDragX by remember { mutableFloatStateOf(0f) }
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08090C))
            .testTag("camera_viewfinder_stage")
            .pointerInput(uiState.zoomRatio) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    if (abs(zoomChange - 1f) > 0.01f) {
                        onZoomChanged(uiState.zoomRatio * zoomChange)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        cumulativeDragX = 0f
                        cumulativeDragY = 0f
                    },
                    onDragEnd = {
                        if (abs(cumulativeDragX) > abs(cumulativeDragY) && abs(cumulativeDragX) > 72f) {
                            // Horizontal swipe changes mode
                            if (cumulativeDragX < 0) onSwipeMode(1) else onSwipeMode(-1)
                        } else if (abs(cumulativeDragY) > abs(cumulativeDragX) && abs(cumulativeDragY) > 72f) {
                            // Vertical swipe flips front/rear camera
                            onFlipCamera()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        cumulativeDragX += dragAmount.x
                        cumulativeDragY += dragAmount.y
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                    val normY = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                    onTapFocus(normX, normY)
                    boundCamera?.let { cam ->
                        try {
                            val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            cam.cameraControl.startFocusAndMetering(action)
                        } catch (_: Exception) {
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val ratioModifier = when (uiState.aspectRatio) {
            AspectRatioOption.RATIO_4_3 -> Modifier.fillMaxWidth().aspectRatio(3f / 4f)
            AspectRatioOption.RATIO_16_9 -> Modifier.fillMaxWidth().aspectRatio(9f / 16f)
            AspectRatioOption.RATIO_1_1 -> Modifier.fillMaxWidth().aspectRatio(1f)
            AspectRatioOption.RATIO_FULL -> Modifier.fillMaxSize()
        }

        Box(
            modifier = ratioModifier
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
        ) {
            // Optical studio reference backdrop (visible in headless emulators or while camera initializes)
            Image(
                painter = painterResource(id = R.drawable.img_optics_studio_banner_1793975946933),
                contentDescription = "Camera optical viewfinder scene",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Real-time Viewfinder Overlays (Grid, Horizon Level, Focus Reticle, Single Active Pro Overlay)
            ViewfinderGraphicsOverlay(
                uiState = uiState,
                modifier = Modifier.fillMaxSize()
            )

            // Shutter Flash Effect
            AnimatedVisibility(
                visible = uiState.isShutterFlashing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.72f))
                )
            }

            // Countdown Timer Overlay
            if (uiState.countdownRemainingSec > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${uiState.countdownRemainingSec}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = AmberOpticPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Docked Top-Corner Pro Monitoring HUD (Single Active Overlay + Compact RGB/Luma Histogram)
        if (uiState.currentMode.isProMode) {
            DockedProMonitoringHud(
                telemetry = uiState.frameTelemetry,
                showHistogram = uiState.proState.showHistogram,
                activeOverlay = uiState.proState.activeOverlay,
                zebraThresholdPercent = uiState.proState.zebraThresholdPercent,
                onToggleHistogram = onToggleHistogram,
                onSelectProOverlay = onSelectProOverlay,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun ViewfinderGraphicsOverlay(
    uiState: CameraUiState,
    modifier: Modifier = Modifier
) {
    val grid = uiState.gridOption
    val showLevel = uiState.horizonLevelEnabled
    val rollDeg = uiState.rollAngleDegrees
    val focusX = uiState.focusTapNormX
    val focusY = uiState.focusTapNormY
    val activeOverlay = if (uiState.currentMode.isProMode) uiState.proState.activeOverlay else ProOverlayType.NONE
    val telemetry = uiState.frameTelemetry

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Single Active Pro Monitoring Overlay (Zebra, Focus Peaking, or False Color IRE)
        if (activeOverlay != ProOverlayType.NONE && telemetry.overlayCells.isNotEmpty()) {
            val gw = telemetry.gridWidth.coerceAtLeast(1)
            val gh = telemetry.gridHeight.coerceAtLeast(1)
            val cellW = w / gw
            val cellH = h / gh

            for (gy in 0 until gh) {
                for (gx in 0 until gw) {
                    val idx = gy * gw + gx
                    if (idx >= telemetry.overlayCells.size) continue
                    val packed = telemetry.overlayCells[idx]
                    val luma = packed and 0xFF
                    val isZebra = (packed and 256) != 0
                    val isPeak = (packed and 512) != 0
                    val left = gx * cellW
                    val top = gy * cellH

                    when (activeOverlay) {
                        ProOverlayType.ZEBRA -> {
                            if (isZebra) {
                                drawLine(
                                    color = CinemaCrimsonError.copy(alpha = 0.78f),
                                    start = Offset(left, top + cellH),
                                    end = Offset(left + cellW, top),
                                    strokeWidth = 2.5f
                                )
                            }
                        }
                        ProOverlayType.FOCUS_PEAKING -> {
                            if (isPeak) {
                                drawRect(
                                    color = CyanAiSecondary.copy(alpha = 0.72f),
                                    topLeft = Offset(left + cellW * 0.15f, top + cellH * 0.15f),
                                    size = Size(cellW * 0.7f, cellH * 0.7f),
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                        ProOverlayType.FALSE_COLOR -> {
                            // Standard IRE false-color mapping
                            val ire = (luma * 100) / 255
                            val ireColor = when {
                                ire <= 6 -> Color(0xAA7B1FA2) // Crushed shadows (Purple)
                                ire in 7..22 -> Color(0x881976D2) // Underexposed (Blue)
                                ire in 38..48 -> Color(0x882E7D32) // 18% Middle Grey (Green)
                                ire in 52..68 -> Color(0x88F48FB1) // Skin Tone Reference (Pink)
                                ire in 82..92 -> Color(0x99FBC02D) // Near Clip (Yellow)
                                ire >= 93 -> Color(0xBBFF1744) // Clipped Highlight (Red)
                                else -> Color.Transparent
                            }
                            if (ireColor != Color.Transparent) {
                                drawRect(
                                    color = ireColor,
                                    topLeft = Offset(left, top),
                                    size = Size(cellW + 0.5f, cellH + 0.5f)
                                )
                            }
                        }
                        ProOverlayType.NONE -> Unit
                    }
                }
            }
        }

        // 2. Composition Grid Lines
        val gridColor = Color.White.copy(alpha = 0.28f)
        when (grid) {
            GridOption.OFF -> Unit
            GridOption.RULE_OF_THIRDS -> {
                for (i in 1..2) {
                    val x = (w * i) / 3f
                    val y = (h * i) / 3f
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                }
            }
            GridOption.GOLDEN_RATIO -> {
                val phi1 = 0.382f
                val phi2 = 0.618f
                for (r in listOf(phi1, phi2)) {
                    drawLine(gridColor, Offset(w * r, 0f), Offset(w * r, h), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(0f, h * r), Offset(w, h * r), strokeWidth = 1.5f)
                }
            }
            GridOption.CROSSHAIR -> {
                val cx = w / 2f
                val cy = h / 2f
                val len = 42.dp.toPx()
                drawLine(gridColor, Offset(cx - len, cy), Offset(cx + len, cy), strokeWidth = 2f)
                drawLine(gridColor, Offset(cx, cy - len), Offset(cx, cy + len), strokeWidth = 2f)
                drawCircle(gridColor, radius = 24.dp.toPx(), center = Offset(cx, cy), style = Stroke(1.5f))
            }
        }

        // 3. Horizon Level Indicator
        if (showLevel) {
            val isLevel = abs(rollDeg) <= 1.2f
            val levelColor = if (isLevel) EmeraldOptimal else AmberOpticPrimary.copy(alpha = 0.85f)
            val cx = w / 2f
            val cy = h / 2f
            val halfSpan = 56.dp.toPx()

            // Outer reference ticks
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(cx - halfSpan - 20f, cy),
                end = Offset(cx - halfSpan - 6f, cy),
                strokeWidth = 2.5f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(cx + halfSpan + 6f, cy),
                end = Offset(cx + halfSpan + 20f, cy),
                strokeWidth = 2.5f
            )

            // Rotating horizon bar
            rotate(degrees = -rollDeg, pivot = Offset(cx, cy)) {
                drawLine(
                    color = levelColor,
                    start = Offset(cx - halfSpan, cy),
                    end = Offset(cx + halfSpan, cy),
                    strokeWidth = if (isLevel) 3.5f else 2.5f
                )
            }
        }

        // 4. Tap-to-Focus & Exposure Ring
        if (focusX != null && focusY != null) {
            val fx = focusX * w
            val fy = focusY * h
            val ringRadius = 28.dp.toPx()
            drawCircle(
                color = AmberOpticPrimary,
                radius = ringRadius,
                center = Offset(fx, fy),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = AmberOpticPrimary.copy(alpha = 0.4f),
                radius = 4.dp.toPx(),
                center = Offset(fx, fy)
            )
        }
    }
}

@Composable
fun DockedProMonitoringHud(
    telemetry: FrameTelemetry,
    showHistogram: Boolean,
    activeOverlay: ProOverlayType,
    zebraThresholdPercent: Int,
    onToggleHistogram: (Boolean) -> Unit,
    onSelectProOverlay: (ProOverlayType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(206.dp)
            .testTag("docked_pro_monitoring_hud"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xD910131A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRO MONITOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberOpticPrimary
                )
                Text(
                    text = String.format(java.util.Locale.US, "DR %.1fst", telemetry.dynamicRangeStopsEstimate),
                    style = MaterialTheme.typography.labelSmall,
                    color = SilverTextSecondary
                )
            }

            if (showHistogram) {
                Spacer(modifier = Modifier.height(6.dp))
                LiveHistogramCanvas(
                    telemetry = telemetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF07080C))
                        .padding(4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "BLK %.1f%%", telemetry.shadowClipPercent),
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverTextSecondary
                    )
                    Text(
                        text = "LUMA ${telemetry.averageLuma}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IvoryTextPrimary
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "CLIP %.1f%%", telemetry.highlightClipPercent),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (telemetry.highlightClipPercent > 3f) CinemaCrimsonError else SilverTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Compact Pill Row: HIST toggle + Single Active Overlay selector (ZEBRA / PEAK / FALSE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HudMiniPill(
                    label = "HIST",
                    selected = showHistogram,
                    accentColor = AmberOpticPrimary,
                    contentDesc = "Toggle live histogram in docked HUD",
                    tag = "hud_toggle_histogram",
                    onClick = { onToggleHistogram(!showHistogram) },
                    modifier = Modifier.weight(1f)
                )
                HudMiniPill(
                    label = "${zebraThresholdPercent}%",
                    selected = activeOverlay == ProOverlayType.ZEBRA,
                    accentColor = CinemaCrimsonError,
                    contentDesc = "Toggle Zebra Stripes highlight clipping overlay",
                    tag = "hud_overlay_zebra",
                    onClick = { onSelectProOverlay(ProOverlayType.ZEBRA) },
                    modifier = Modifier.weight(1f)
                )
                HudMiniPill(
                    label = "PEAK",
                    selected = activeOverlay == ProOverlayType.FOCUS_PEAKING,
                    accentColor = CyanAiSecondary,
                    contentDesc = "Toggle Focus Peaking edge overlay",
                    tag = "hud_overlay_peaking",
                    onClick = { onSelectProOverlay(ProOverlayType.FOCUS_PEAKING) },
                    modifier = Modifier.weight(1f)
                )
                HudMiniPill(
                    label = "IRE",
                    selected = activeOverlay == ProOverlayType.FALSE_COLOR,
                    accentColor = EmeraldOptimal,
                    contentDesc = "Toggle False Color IRE exposure overlay",
                    tag = "hud_overlay_false_color",
                    onClick = { onSelectProOverlay(ProOverlayType.FALSE_COLOR) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HudMiniPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    contentDesc: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) accentColor.copy(alpha = 0.25f) else Color(0xFF1E222D))
            .border(
                width = 1.dp,
                color = if (selected) accentColor else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = contentDesc }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accentColor else IvoryTextPrimary
        )
    }
}

@Composable
private fun LiveHistogramCanvas(
    telemetry: FrameTelemetry,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bins = telemetry.lumaBins
        if (bins.isEmpty()) return@Canvas
        val maxVal = max(1, bins.maxOrNull() ?: 1).toFloat()
        val w = size.width
        val h = size.height
        val stepX = w / (bins.size - 1).coerceAtLeast(1)

        // Draw RGB channel curves subtly behind Luminance curve
        fun drawCurve(channelBins: IntArray, strokeColor: Color, fillAlpha: Float) {
            val path = Path()
            val fillPath = Path()
            fillPath.moveTo(0f, h)
            for (i in channelBins.indices) {
                val x = i * stepX
                val norm = (channelBins[i] / maxVal).coerceIn(0f, 1f)
                val y = h - (norm * h)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            fillPath.lineTo(w, h)
            fillPath.close()
            if (fillAlpha > 0f) {
                drawPath(fillPath, strokeColor.copy(alpha = fillAlpha))
            }
            drawPath(path, strokeColor, style = Stroke(width = 1.6f))
        }

        drawCurve(telemetry.redBins, Color(0xFFFF5252), 0.08f)
        drawCurve(telemetry.greenBins, Color(0xFF69F0AE), 0.08f)
        drawCurve(telemetry.blueBins, Color(0xFF40C4FF), 0.08f)
        drawCurve(telemetry.lumaBins, Color.White, 0.22f)
    }
}
