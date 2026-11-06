package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.DeviceEnvironmentMonitor
import com.example.ui.AiEnhanceSettingsSheet
import com.example.ui.BottomCameraControlDeck
import com.example.ui.CameraCaptureBridge
import com.example.ui.CameraViewfinderStage
import com.example.ui.DualVersionGallerySheet
import com.example.ui.FourDotQuickSettingsSheet
import com.example.ui.HardwareAndChecklistSheet
import com.example.ui.PresetsManagerSheet
import com.example.ui.TopTelemetryUtilityBar
import com.example.ui.theme.AmberOpticPrimary
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.IvoryTextPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverTextSecondary
import com.example.ui.theme.ThermalWarningOrange
import com.example.viewmodel.ActiveSheet
import com.example.viewmodel.CameraViewModel

class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LuminaCameraAppScreen(viewModel = cameraViewModel)
            }
        }
    }
}

@Composable
fun LuminaCameraAppScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val captureBridge = remember { CameraCaptureBridge() }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val camGranted = result[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        hasCameraPerm = camGranted
        if (camGranted) {
            viewModel.refreshHardwareAndSettings()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPerm) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    // Lifecycle-scoped sensor & thermal monitor (zero leaks on background/stop)
    val envMonitor = remember {
        DeviceEnvironmentMonitor(
            context = context.applicationContext,
            onOrientationChanged = { roll, pitch -> viewModel.updateOrientation(roll, pitch) },
            onThermalChanged = { level, tempC -> viewModel.updateThermalState(level, tempC) }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> envMonitor.start()
                Lifecycle.Event.ON_STOP -> envMonitor.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            envMonitor.stop()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Accessibility Polite LiveRegion for TalkBack announcements
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = uiState.screenReaderAnnouncement
                    }
                    .testTag("accessibility_live_region")
            )

            // Main Viewfinder Stage (occupies full canvas behind ergonomic top/bottom bars)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CameraViewfinderStage(
                        uiState = uiState,
                        hasCameraPermission = hasCameraPerm,
                        captureBridge = captureBridge,
                        onSwipeMode = { delta -> viewModel.swipeMode(delta) },
                        onFlipCamera = { viewModel.flipFrontRearCamera() },
                        onZoomChanged = { zoom -> viewModel.setZoomRatio(zoom) },
                        onTapFocus = { nx, ny -> viewModel.setTapFocusPoint(nx, ny) },
                        onTelemetryUpdated = { telemetry -> viewModel.updateLiveTelemetry(telemetry) },
                        onToggleHistogram = { show -> viewModel.toggleHistogram(show) },
                        onSelectProOverlay = { overlay -> viewModel.setSingleActiveProOverlay(overlay) }
                    )

                    // Top Utility Telemetry Bar
                    TopTelemetryUtilityBar(
                        uiState = uiState,
                        onCycleFlash = { viewModel.cycleFlashOption() },
                        onCycleTimer = { viewModel.cycleTimerOption() },
                        onCycleAspectRatio = {
                            viewModel.toggle4kFpsOrPhotoRatio()
                        },
                        onOpenSheet = { sheet -> viewModel.openSheet(sheet) },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Non-blocking Camera Permission Banner (if declined in emulator/device)
                    if (!hasCameraPerm) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth()
                                .testTag("camera_permission_banner"),
                            color = GraphiteSurface.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberOpticPrimary)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = AmberOpticPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Camera Permission Needed for Live Hardware Feed",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = IvoryTextPrimary
                                    )
                                    Text(
                                        text = "Using optical studio calibration frame until granted.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SilverTextSecondary
                                    )
                                }
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.CAMERA,
                                                Manifest.permission.RECORD_AUDIO
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberOpticPrimary,
                                        contentColor = Color.Black
                                    ),
                                    modifier = Modifier.testTag("grant_permission_button")
                                ) {
                                    Text("Grant")
                                }
                            }
                        }
                    }

                    // Transient Hardware / Compatibility / AI Status Banner
                    if (uiState.statusBannerMessage != null) {
                        Surface(
                            color = Color(0xEE181B24),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ThermalWarningOrange),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 68.dp, start = 14.dp, end = 14.dp)
                                .fillMaxWidth()
                                .testTag("status_banner_toast")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = ThermalWarningOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.statusBannerMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IvoryTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissBanner() },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss status banner",
                                        tint = SilverTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom One-Handed Ergonomic Control Deck
                BottomCameraControlDeck(
                    uiState = uiState,
                    onSelectLensIndex = { idx -> viewModel.selectLensIndex(idx) },
                    onSetZoomRatio = { z -> viewModel.setZoomRatio(z) },
                    onSelectMode = { mode -> viewModel.selectMode(mode) },
                    onShutterTap = {
                        viewModel.triggerShutter(
                            rawJpegBytesProvider = if (hasCameraPerm) {
                                { captureBridge.captureJpegBytes(context) }
                            } else {
                                null
                            }
                        )
                    },
                    onShutterLongPress = {
                        if (uiState.isBurstCapturing) {
                            viewModel.stopBurstCapture()
                        } else {
                            viewModel.startBurstCapture()
                        }
                    },
                    onFlipCamera = { viewModel.flipFrontRearCamera() },
                    onOpenSheet = { sheet -> viewModel.openSheet(sheet) },
                    onCancelAiJob = { viewModel.cancelActiveAiEnhancement() },
                    onUpdateProIso = { iso -> viewModel.updateProIso(iso) },
                    onUpdateProShutterNs = { ns -> viewModel.updateProShutterNs(ns) },
                    onUpdateProWb = { preset, kelvin -> viewModel.updateProWhiteBalance(preset, kelvin) },
                    onUpdateProFocus = { diopters -> viewModel.updateProFocusDiopters(diopters) },
                    onUpdateExposureComp = { ev -> viewModel.updateExposureCompensation(ev) },
                    onToggleAeLock = { viewModel.toggleAeLock() },
                    onToggleAfLock = { viewModel.toggleAfLock() },
                    onUpdateZebraThreshold = { pct -> viewModel.setZebraThreshold(pct) },
                    onResetProAuto = { viewModel.resetProControlsToAuto() }
                )
            }

            // Full-Screen / Bottom-Weighted Sheets
            when (uiState.activeSheet) {
                ActiveSheet.NONE -> Unit
                ActiveSheet.QUICK_SETTINGS -> {
                    FourDotQuickSettingsSheet(
                        uiState = uiState,
                        onSetAspectRatio = { viewModel.setAspectRatio(it) },
                        onSetPhotoResolutionIndex = { viewModel.setPhotoResolutionIndex(it) },
                        onSetVideoResolution = { viewModel.setVideoResolution(it) },
                        onSetVideoFps = { viewModel.setVideoFps(it) },
                        onToggleHdr = { viewModel.toggleHdr(it) },
                        onToggleStabilization = { viewModel.toggleStabilization(it) },
                        onSetGridOption = { viewModel.setGridOption(it) },
                        onToggleHorizonLevel = { viewModel.toggleHorizonLevel(it) },
                        onToggleAudio = { viewModel.toggleAudioRecording(it) },
                        onSaveCustomFolder = { viewModel.setCustomStorageFolder(it) },
                        onNavigateSheet = { viewModel.openSheet(it) },
                        onClose = { viewModel.closeSheet() }
                    )
                }
                ActiveSheet.AI_SETTINGS -> {
                    AiEnhanceSettingsSheet(
                        uiState = uiState,
                        onUpdateAiSettings = { viewModel.updateAiSettings(it) },
                        onResetAiSettings = { viewModel.resetAiSettingsToDefault() },
                        onClose = { viewModel.closeSheet() }
                    )
                }
                ActiveSheet.GALLERY -> {
                    DualVersionGallerySheet(
                        uiState = uiState,
                        onSelectMediaItem = { viewModel.selectGalleryItem(it) },
                        onToggleOriginalVsAi = { id, showAi -> viewModel.toggleGalleryOriginalVsAi(id, showAi) },
                        onTriggerManualAiEnhance = { id -> viewModel.requestAiEnhancementForMedia(id) },
                        onCancelAiJob = { viewModel.cancelActiveAiEnhancement() },
                        onOpenAiSettings = { viewModel.openSheet(ActiveSheet.AI_SETTINGS) },
                        onClose = { viewModel.closeSheet() }
                    )
                }
                ActiveSheet.PRESETS -> {
                    PresetsManagerSheet(
                        uiState = uiState,
                        onSavePreset = { viewModel.saveCurrentAsPreset(it) },
                        onLoadPreset = { viewModel.loadPresetWithValidation(it) },
                        onRenamePreset = { id, name -> viewModel.renamePreset(id, name) },
                        onDeletePreset = { viewModel.deletePreset(it) },
                        onClose = { viewModel.closeSheet() }
                    )
                }
                ActiveSheet.HARDWARE_REPORT -> {
                    HardwareAndChecklistSheet(
                        uiState = uiState,
                        onClose = { viewModel.closeSheet() }
                    )
                }
            }
        }
    }
}
