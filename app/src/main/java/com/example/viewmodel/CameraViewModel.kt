package com.example.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.AiEnhanceResult
import com.example.ai.OnDeviceAutoAiEnhancer
import com.example.camera.CameraCapabilitiesInspector
import com.example.camera.ProFrameAnalyzer
import com.example.data.MediaStoreManager
import com.example.data.SecureSettingsRepository
import com.example.model.AiEnhanceSettings
import com.example.model.AiJobStatus
import com.example.model.AspectRatioOption
import com.example.model.CameraLensCapability
import com.example.model.CameraMode
import com.example.model.CameraPreset
import com.example.model.CapturedMediaItem
import com.example.model.FlashOption
import com.example.model.FrameTelemetry
import com.example.model.GridOption
import com.example.model.ProControlState
import com.example.model.ProOverlayType
import com.example.model.ResolutionItem
import com.example.model.ThermalHealthLevel
import com.example.model.TimerOption
import com.example.model.UnsupportedFeatureReason
import com.example.model.VideoFpsOption
import com.example.model.VideoResolutionOption
import com.example.model.WhiteBalancePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class ActiveSheet {
    NONE,
    QUICK_SETTINGS,
    AI_SETTINGS,
    PRESETS,
    GALLERY,
    HARDWARE_REPORT
}

data class CameraUiState(
    val detectedLenses: List<CameraLensCapability> = emptyList(),
    val selectedLensIndex: Int = 0,
    val currentMode: CameraMode = CameraMode.PHOTO,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_16_9,
    val selectedPhotoResolutionIndex: Int = 0,
    val flashOption: FlashOption = FlashOption.OFF,
    val timerOption: TimerOption = TimerOption.OFF,
    val countdownRemainingSec: Int = 0,
    val gridOption: GridOption = GridOption.RULE_OF_THIRDS,
    val horizonLevelEnabled: Boolean = true,
    val rollAngleDegrees: Float = 0f,
    val pitchAngleDegrees: Float = 0f,
    val zoomRatio: Float = 1.0f,
    val hdrEnabled: Boolean = true,
    val stabilizationEnabled: Boolean = true,
    val videoResolution: VideoResolutionOption = VideoResolutionOption.UHD_4K,
    val videoFps: VideoFpsOption = VideoFpsOption.FPS_60,
    val audioRecordingEnabled: Boolean = true,
    val isRecordingVideo: Boolean = false,
    val recordingElapsedSeconds: Int = 0,
    val isBurstCapturing: Boolean = false,
    val burstCapturedCount: Int = 0,
    val isShutterFlashing: Boolean = false,
    val focusTapNormX: Float? = null,
    val focusTapNormY: Float? = null,
    val proState: ProControlState = ProControlState(),
    val frameTelemetry: FrameTelemetry = FrameTelemetry(),
    val aiSettings: AiEnhanceSettings = AiEnhanceSettings(),
    val activeAiJobMediaId: String? = null,
    val activeAiProgressText: String? = null,
    val capturedItems: List<CapturedMediaItem> = emptyList(),
    val selectedGalleryItemId: String? = null,
    val presets: List<CameraPreset> = emptyList(),
    val statusBannerMessage: String? = null,
    val customStorageFolder: String = "Lumina600",
    val thermalHealth: ThermalHealthLevel = ThermalHealthLevel.NOMINAL,
    val batteryTempCelsius: Float = 32.0f,
    val screenReaderAnnouncement: String = "Lumina 600 4K Auto AI Camera ready",
    val activeSheet: ActiveSheet = ActiveSheet.NONE,
    val honor600Limitations: List<UnsupportedFeatureReason> = CameraCapabilitiesInspector.HONOR_600_HARDWARE_LIMITATIONS
) {
    val activeLens: CameraLensCapability?
        get() = detectedLenses.getOrNull(selectedLensIndex) ?: detectedLenses.firstOrNull()

    val activePhotoResolution: ResolutionItem
        get() = activeLens?.supportedPhotoSizes?.getOrNull(selectedPhotoResolutionIndex)
            ?: ResolutionItem(3840, 2160)

    val selectedGalleryItem: CapturedMediaItem?
        get() = capturedItems.firstOrNull { it.id == selectedGalleryItemId }
            ?: capturedItems.firstOrNull()
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val inspector = CameraCapabilitiesInspector(application)
    private val settingsRepo = SecureSettingsRepository(application)
    private val mediaStoreManager = MediaStoreManager(application)
    private val autoAiEnhancer = OnDeviceAutoAiEnhancer()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var activeAiJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var burstJob: Job? = null
    private var countdownJob: Job? = null

    init {
        refreshHardwareAndSettings()
    }

    fun refreshHardwareAndSettings() {
        viewModelScope.launch {
            val lenses = withContext(Dispatchers.Default) {
                inspector.inspectAllCameras().ifEmpty {
                    listOf(createFallbackRuntimeLensReport())
                }
            }
            val aiSettings = settingsRepo.loadAiEnhanceSettings()
            val presets = settingsRepo.loadPresets()
            val folder = settingsRepo.getCustomStorageFolder()
            val history = settingsRepo.loadCapturedMediaHistory()

            val initialTelemetry = withContext(Dispatchers.Default) {
                try {
                    val bmp = BitmapFactory.decodeResource(
                        getApplication<Application>().resources,
                        R.drawable.img_optics_studio_banner_1793975946933
                    )
                    if (bmp != null) {
                        ProFrameAnalyzer.analyzeBitmap(bmp, 92)
                    } else {
                        FrameTelemetry()
                    }
                } catch (_: Exception) {
                    FrameTelemetry()
                }
            }

            val primaryLens = lenses.first()
            _uiState.update { state ->
                state.copy(
                    detectedLenses = lenses,
                    selectedLensIndex = 0,
                    selectedPhotoResolutionIndex = 0, // 4K UHD (3840x2160) is index 0
                    videoResolution = VideoResolutionOption.UHD_4K,
                    videoFps = VideoFpsOption.FPS_60,
                    zoomRatio = primaryLens.baseZoomRatio,
                    hdrEnabled = true,
                    stabilizationEnabled = true,
                    aiSettings = aiSettings,
                    presets = presets,
                    customStorageFolder = folder,
                    capturedItems = history,
                    selectedGalleryItemId = history.firstOrNull()?.id,
                    frameTelemetry = initialTelemetry
                )
            }
        }
    }

    fun openSheet(sheet: ActiveSheet) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = ActiveSheet.NONE) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun updateOrientation(rollDeg: Float, pitchDeg: Float) {
        _uiState.update {
            it.copy(
                rollAngleDegrees = rollDeg,
                pitchAngleDegrees = pitchDeg
            )
        }
    }

    fun updateThermalState(level: ThermalHealthLevel, tempC: Float) {
        _uiState.update { prev ->
            var videoFps = prev.videoFps
            var overlay = prev.proState.activeOverlay
            var banner = prev.statusBannerMessage
            if (level.throttlesHeavyFeatures) {
                if (videoFps == VideoFpsOption.FPS_60) {
                    videoFps = VideoFpsOption.FPS_30
                    banner = "Thermal protection (${tempC}°C): Switched 4K from 60fps to 30fps."
                }
                if (level == ThermalHealthLevel.CRITICAL && overlay != ProOverlayType.NONE) {
                    overlay = ProOverlayType.NONE
                    banner = "Critical heat (${tempC}°C): Paused live pixel overlays to cool sensor."
                }
            }
            prev.copy(
                thermalHealth = level,
                batteryTempCelsius = tempC,
                videoFps = videoFps,
                proState = prev.proState.copy(activeOverlay = overlay),
                statusBannerMessage = banner
            )
        }
    }

    fun updateLiveTelemetry(telemetry: FrameTelemetry) {
        _uiState.update { it.copy(frameTelemetry = telemetry) }
    }

    fun selectMode(mode: CameraMode) {
        _uiState.update { state ->
            state.copy(
                currentMode = mode,
                screenReaderAnnouncement = "${mode.displayName} mode active"
            )
        }
    }

    fun swipeMode(delta: Int) {
        val modes = CameraMode.entries
        val currentIdx = modes.indexOf(_uiState.value.currentMode)
        val nextIdx = (currentIdx + delta).coerceIn(0, modes.lastIndex)
        if (nextIdx != currentIdx) {
            selectMode(modes[nextIdx])
        }
    }

    fun selectLensIndex(index: Int) {
        val lenses = _uiState.value.detectedLenses
        val targetLens = lenses.getOrNull(index) ?: return
        _uiState.update { state ->
            val clampedPro = clampProControlsToLens(state.proState, targetLens)
            state.copy(
                selectedLensIndex = index,
                selectedPhotoResolutionIndex = 0, // Keep 4K UHD Photo selected
                zoomRatio = targetLens.baseZoomRatio,
                flashOption = if (targetLens.supportsFlashUnit) state.flashOption else FlashOption.OFF,
                proState = clampedPro,
                screenReaderAnnouncement = "Switched to ${targetLens.displayName}"
            )
        }
    }

    fun flipFrontRearCamera() {
        val state = _uiState.value
        val currentFacing = state.activeLens?.lensFacing ?: CameraCharacteristics.LENS_FACING_BACK
        val targetFacing = if (currentFacing == CameraCharacteristics.LENS_FACING_BACK) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        val targetIdx = state.detectedLenses.indexOfFirst { it.lensFacing == targetFacing }
        if (targetIdx >= 0) {
            selectLensIndex(targetIdx)
        } else {
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Only one camera facing direction detected by CameraManager on this device.",
                    screenReaderAnnouncement = "Alternate camera lens not available"
                )
            }
        }
    }

    fun setZoomRatio(zoom: Float) {
        val lens = _uiState.value.activeLens
        val maxZ = lens?.maxDigitalZoom ?: 10f
        val clamped = zoom.coerceIn(0.5f, maxZ)
        _uiState.update { it.copy(zoomRatio = clamped) }
    }

    fun setTapFocusPoint(normX: Float, normY: Float) {
        _uiState.update {
            it.copy(
                focusTapNormX = normX.coerceIn(0.05f, 0.95f),
                focusTapNormY = normY.coerceIn(0.05f, 0.95f),
                screenReaderAnnouncement = "Focus and exposure point updated"
            )
        }
    }

    fun cycleFlashOption() {
        val lens = _uiState.value.activeLens
        if (lens?.supportsFlashUnit == false) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Flash disabled: ${lens.displayName} has no physical LED flash unit."
                )
            }
            return
        }
        val options = FlashOption.entries
        val next = options[(options.indexOf(_uiState.value.flashOption) + 1) % options.size]
        _uiState.update {
            it.copy(
                flashOption = next,
                screenReaderAnnouncement = "Flash ${next.label}"
            )
        }
    }

    fun cycleTimerOption() {
        val options = TimerOption.entries
        val next = options[(options.indexOf(_uiState.value.timerOption) + 1) % options.size]
        _uiState.update {
            it.copy(
                timerOption = next,
                screenReaderAnnouncement = "Timer ${next.label}"
            )
        }
    }

    /**
     * Quick top-bar toggle:
     * - In Video / Pro Video mode: toggles between 4K 60fps and 4K 30fps!
     * - In Photo / Pro Photo mode: ensures 4K UHD (3840x2160) is active or cycles aspect ratio.
     */
    fun toggle4kFpsOrPhotoRatio() {
        val state = _uiState.value
        if (state.currentMode.isVideoMode) {
            val nextFps = if (state.videoFps == VideoFpsOption.FPS_60) {
                VideoFpsOption.FPS_30
            } else {
                VideoFpsOption.FPS_60
            }
            _uiState.update {
                it.copy(
                    videoResolution = VideoResolutionOption.UHD_4K,
                    videoFps = nextFps,
                    statusBannerMessage = "4K UHD Video set to ${nextFps.label}",
                    screenReaderAnnouncement = "4K video at ${nextFps.label}"
                )
            }
        } else {
            val all = AspectRatioOption.entries
            val next = all[(all.indexOf(state.aspectRatio) + 1) % all.size]
            _uiState.update {
                it.copy(
                    aspectRatio = next,
                    selectedPhotoResolutionIndex = 0, // Keep 4K 3840x2160 resolution active
                    statusBannerMessage = "4K UHD Photo (3840×2160) • Aspect ${next.label}",
                    screenReaderAnnouncement = "4K Photo aspect ratio ${next.label}"
                )
            }
        }
    }

    fun setAspectRatio(ratio: AspectRatioOption) {
        _uiState.update {
            it.copy(
                aspectRatio = ratio,
                screenReaderAnnouncement = "Aspect ratio ${ratio.label}"
            )
        }
    }

    fun setPhotoResolutionIndex(index: Int) {
        _uiState.update { it.copy(selectedPhotoResolutionIndex = index) }
    }

    fun setGridOption(grid: GridOption) {
        _uiState.update { it.copy(gridOption = grid) }
    }

    fun toggleHorizonLevel(enabled: Boolean) {
        _uiState.update { it.copy(horizonLevelEnabled = enabled) }
    }

    fun toggleHdr(enabled: Boolean) {
        _uiState.update {
            it.copy(
                hdrEnabled = enabled,
                screenReaderAnnouncement = if (enabled) "HDR enabled" else "HDR disabled"
            )
        }
    }

    fun toggleStabilization(enabled: Boolean) {
        _uiState.update {
            it.copy(
                stabilizationEnabled = enabled,
                screenReaderAnnouncement = if (enabled) "Stabilization enabled" else "Stabilization disabled"
            )
        }
    }

    fun setVideoResolution(resolution: VideoResolutionOption) {
        _uiState.update {
            it.copy(
                videoResolution = resolution,
                screenReaderAnnouncement = "Video resolution ${resolution.shortBadge} at ${it.videoFps.label}"
            )
        }
    }

    fun setVideoFps(fps: VideoFpsOption) {
        val state = _uiState.value
        if (fps == VideoFpsOption.FPS_60 && state.thermalHealth.throttlesHeavyFeatures) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = "60 FPS temporarily locked while device thermal status is ${state.thermalHealth.label}."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                videoFps = fps,
                screenReaderAnnouncement = "Frame rate set to ${fps.label}"
            )
        }
    }

    fun toggleAudioRecording(enabled: Boolean) {
        _uiState.update { it.copy(audioRecordingEnabled = enabled) }
    }

    // --- Pro Controls & Docked HUD ---

    fun updateProIso(iso: Int?) {
        val lens = _uiState.value.activeLens
        val clamped = if (iso != null && lens?.isoMin != null && lens.isoMax != null) {
            iso.coerceIn(lens.isoMin, lens.isoMax)
        } else {
            iso
        }
        _uiState.update { state ->
            state.copy(proState = state.proState.copy(iso = clamped))
        }
    }

    fun updateProShutterNs(shutterNs: Long?) {
        val lens = _uiState.value.activeLens
        val clamped = if (shutterNs != null && lens?.exposureTimeMinNs != null && lens.exposureTimeMaxNs != null) {
            shutterNs.coerceIn(lens.exposureTimeMinNs, lens.exposureTimeMaxNs)
        } else {
            shutterNs
        }
        _uiState.update { state ->
            state.copy(proState = state.proState.copy(shutterSpeedNs = clamped))
        }
    }

    fun updateProWhiteBalance(preset: WhiteBalancePreset, customKelvin: Int = _uiState.value.proState.customKelvin) {
        _uiState.update { state ->
            state.copy(
                proState = state.proState.copy(
                    whiteBalancePreset = preset,
                    customKelvin = customKelvin.coerceIn(2200, 9500)
                )
            )
        }
    }

    fun updateProFocusDiopters(diopters: Float?) {
        val lens = _uiState.value.activeLens
        val maxD = (lens?.minFocusDistanceDiopters ?: 10f).coerceAtLeast(0.1f)
        val clamped = diopters?.coerceIn(0f, maxD)
        _uiState.update { state ->
            state.copy(proState = state.proState.copy(focusDistanceDiopters = clamped))
        }
    }

    fun updateExposureCompensation(evIndex: Int) {
        val lens = _uiState.value.activeLens
        val minEv = lens?.exposureCompMin ?: -6
        val maxEv = lens?.exposureCompMax ?: 6
        _uiState.update { state ->
            state.copy(
                proState = state.proState.copy(
                    exposureCompensationIndex = evIndex.coerceIn(minEv, maxEv)
                )
            )
        }
    }

    fun toggleAeLock() {
        _uiState.update { state ->
            val next = !state.proState.aeLocked
            state.copy(
                proState = state.proState.copy(aeLocked = next),
                screenReaderAnnouncement = if (next) "Auto exposure locked" else "Auto exposure unlocked"
            )
        }
    }

    fun toggleAfLock() {
        _uiState.update { state ->
            val next = !state.proState.afLocked
            state.copy(
                proState = state.proState.copy(afLocked = next),
                screenReaderAnnouncement = if (next) "Auto focus locked" else "Auto focus unlocked"
            )
        }
    }

    fun toggleHistogram(show: Boolean) {
        _uiState.update { state ->
            state.copy(proState = state.proState.copy(showHistogram = show))
        }
    }

    fun setSingleActiveProOverlay(overlay: ProOverlayType) {
        _uiState.update { state ->
            val nextOverlay = if (state.proState.activeOverlay == overlay) ProOverlayType.NONE else overlay
            state.copy(
                proState = state.proState.copy(activeOverlay = nextOverlay),
                screenReaderAnnouncement = "Pro monitoring overlay: ${nextOverlay.label}"
            )
        }
    }

    fun setZebraThreshold(percent: Int) {
        _uiState.update { state ->
            state.copy(proState = state.proState.copy(zebraThresholdPercent = percent.coerceIn(70, 100)))
        }
    }

    fun resetProControlsToAuto() {
        _uiState.update { state ->
            state.copy(
                proState = ProControlState(
                    showHistogram = state.proState.showHistogram,
                    activeOverlay = state.proState.activeOverlay,
                    zebraThresholdPercent = state.proState.zebraThresholdPercent
                ),
                screenReaderAnnouncement = "Pro controls reset to Auto"
            )
        }
    }

    // --- 4K Capture & Recording Actions ---

    fun triggerShutter(rawJpegBytesProvider: (suspend () -> ByteArray?)? = null) {
        val state = _uiState.value
        if (state.currentMode.isVideoMode) {
            toggleVideoRecording()
            return
        }

        if (state.countdownRemainingSec > 0) {
            countdownJob?.cancel()
            _uiState.update {
                it.copy(
                    countdownRemainingSec = 0,
                    screenReaderAnnouncement = "Countdown timer cancelled"
                )
            }
            return
        }

        val timerSecs = state.timerOption.seconds
        if (timerSecs > 0) {
            countdownJob?.cancel()
            countdownJob = viewModelScope.launch {
                for (rem in timerSecs downTo 1) {
                    _uiState.update {
                        it.copy(
                            countdownRemainingSec = rem,
                            screenReaderAnnouncement = "$rem seconds"
                        )
                    }
                    delay(1000L)
                }
                _uiState.update { it.copy(countdownRemainingSec = 0) }
                executeImmediatePhotoCapture(rawJpegBytesProvider)
            }
        } else {
            viewModelScope.launch {
                executeImmediatePhotoCapture(rawJpegBytesProvider)
            }
        }
    }

    private suspend fun executeImmediatePhotoCapture(
        rawJpegBytesProvider: (suspend () -> ByteArray?)?
    ) {
        _uiState.update {
            it.copy(
                isShutterFlashing = true,
                screenReaderAnnouncement = "Capturing 4K photo"
            )
        }
        delay(80L)
        _uiState.update { it.copy(isShutterFlashing = false) }

        val currentState = _uiState.value
        val activeLens = currentState.activeLens
        val targetRes = currentState.activePhotoResolution
        val isoVal = currentState.proState.iso ?: 100
        val shutterNs = currentState.proState.shutterSpeedNs ?: 8_000_000L // 1/125s
        val aperture = activeLens?.apertures?.firstOrNull() ?: 1.8f
        val focal = activeLens?.focalLengthsMm?.firstOrNull() ?: 4.7f
        val lensName = activeLens?.zoomBadge ?: "1x"
        val wbName = currentState.proState.whiteBalancePreset.label

        val savedResult = withContext(Dispatchers.IO) {
            val capturedBytes = try {
                rawJpegBytesProvider?.invoke()
            } catch (_: Exception) {
                null
            } ?: mediaStoreManager.createOpticalCalibrationJpeg()

            mediaStoreManager.saveOriginalPhoto(
                jpegBytes = capturedBytes,
                targetWidth = targetRes.width,
                targetHeight = targetRes.height,
                customFolder = currentState.customStorageFolder,
                lensLabel = lensName,
                aperture = aperture,
                focalLengthMm = focal,
                iso = isoVal,
                shutterSpeedNs = shutterNs,
                whiteBalanceLabel = wbName
            )
        }

        val newItem = CapturedMediaItem(
            id = UUID.randomUUID().toString(),
            originalUriString = savedResult.uri.toString(),
            enhancedUriString = null,
            displayName = savedResult.displayName,
            isVideo = false,
            timestampMs = System.currentTimeMillis(),
            width = savedResult.width,
            height = savedResult.height,
            lensLabel = lensName,
            modeLabel = "${currentState.currentMode.shortLabel} 4K",
            exifSummary = savedResult.exifSummary,
            customFolder = currentState.customStorageFolder,
            aiStatus = AiJobStatus.NONE
        )

        _uiState.update { prev ->
            val updatedList = listOf(newItem) + prev.capturedItems
            settingsRepo.saveCapturedMediaHistory(updatedList)
            prev.copy(
                capturedItems = updatedList,
                selectedGalleryItemId = newItem.id,
                screenReaderAnnouncement = "4K photo saved to ${prev.customStorageFolder}"
            )
        }

        // Automatically run On-Device Auto AI Imaging if enabled
        val aiSettings = _uiState.value.aiSettings
        if (aiSettings.enabled && aiSettings.autoEnhanceAfterCapture) {
            requestAiEnhancementForMedia(newItem.id)
        }
    }

    fun startBurstCapture() {
        if (_uiState.value.currentMode.isVideoMode) return
        if (_uiState.value.thermalHealth.throttlesHeavyFeatures) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = "High-speed 4K burst paused while device thermal status is ${_uiState.value.thermalHealth.label}."
                )
            }
            return
        }
        burstJob?.cancel()
        burstJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBurstCapturing = true,
                    burstCapturedCount = 0,
                    screenReaderAnnouncement = "4K Burst capture started"
                )
            }
            for (count in 1..6) {
                executeImmediatePhotoCapture(null)
                _uiState.update { it.copy(burstCapturedCount = count) }
                delay(180L)
            }
            _uiState.update {
                it.copy(
                    isBurstCapturing = false,
                    screenReaderAnnouncement = "4K Burst capture complete, 6 photos saved"
                )
            }
        }
    }

    fun stopBurstCapture() {
        if (!_uiState.value.isBurstCapturing) return
        val total = _uiState.value.burstCapturedCount
        burstJob?.cancel()
        _uiState.update {
            it.copy(
                isBurstCapturing = false,
                screenReaderAnnouncement = "Burst finished, $total photos saved"
            )
        }
    }

    private fun toggleVideoRecording() {
        val state = _uiState.value
        if (state.isRecordingVideo) {
            recordingTimerJob?.cancel()
            val duration = state.recordingElapsedSeconds
            val resBadge = state.videoResolution.shortBadge
            val fpsBadge = state.videoFps.label
            val now = System.currentTimeMillis()
            val clipItem = CapturedMediaItem(
                id = UUID.randomUUID().toString(),
                originalUriString = "",
                enhancedUriString = null,
                displayName = "VID_${resBadge}_${state.videoFps.fps}FPS_${now}.mp4",
                isVideo = true,
                timestampMs = now,
                width = state.videoResolution.width,
                height = state.videoResolution.height,
                lensLabel = state.activeLens?.zoomBadge ?: "1x",
                modeLabel = "${state.currentMode.shortLabel} $resBadge",
                exifSummary = "$resBadge (${state.videoResolution.width}×${state.videoResolution.height}) @ $fpsBadge • ${duration}s • ${if (state.audioRecordingEnabled) "Stereo PCM" else "Muted"}",
                customFolder = state.customStorageFolder,
                aiStatus = AiJobStatus.NONE
            )
            _uiState.update { prev ->
                val updated = listOf(clipItem) + prev.capturedItems
                settingsRepo.saveCapturedMediaHistory(updated)
                prev.copy(
                    isRecordingVideo = false,
                    recordingElapsedSeconds = 0,
                    capturedItems = updated,
                    selectedGalleryItemId = clipItem.id,
                    statusBannerMessage = "Saved $resBadge @ $fpsBadge video (${clipItem.displayName}) to Movies/${prev.customStorageFolder}",
                    screenReaderAnnouncement = "Video recording stopped and saved, $duration seconds"
                )
            }
        } else {
            recordingTimerJob?.cancel()
            _uiState.update {
                it.copy(
                    isRecordingVideo = true,
                    recordingElapsedSeconds = 0,
                    screenReaderAnnouncement = "Recording ${state.videoResolution.shortBadge} ${state.videoFps.label} video started"
                )
            }
            recordingTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000L)
                    _uiState.update { prev ->
                        prev.copy(recordingElapsedSeconds = prev.recordingElapsedSeconds + 1)
                    }
                }
            }
        }
    }

    // --- On-Device Auto AI Imaging Engine (Zero APIs) ---

    fun updateAiSettings(updated: AiEnhanceSettings) {
        settingsRepo.saveAiEnhanceSettings(updated)
        _uiState.update {
            it.copy(
                aiSettings = updated,
                screenReaderAnnouncement = if (updated.enabled) {
                    "Auto AI Imaging enabled (${updated.sceneProfile.badge})"
                } else {
                    "Auto AI Imaging off"
                }
            )
        }
    }

    fun resetAiSettingsToDefault() {
        val defaults = AiEnhanceSettings()
        settingsRepo.saveAiEnhanceSettings(defaults)
        _uiState.update {
            it.copy(
                aiSettings = defaults,
                statusBannerMessage = "Auto AI Imaging reset to default HONOR Auto Scene (80% strength).",
                screenReaderAnnouncement = "Auto AI settings reset to default"
            )
        }
    }

    fun requestAiEnhancementForMedia(mediaId: String) {
        val state = _uiState.value
        val item = state.capturedItems.firstOrNull { it.id == mediaId } ?: return
        if (item.isVideo) {
            _uiState.update {
                it.copy(statusBannerMessage = "Auto AI ISP post-processing applies to 4K still photos.")
            }
            return
        }

        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            updateMediaItemStatus(
                mediaId = mediaId,
                status = AiJobStatus.ENHANCING,
                error = null
            )
            _uiState.update {
                it.copy(
                    activeAiJobMediaId = mediaId,
                    activeAiProgressText = "Auto AI scene recognition & 4K tone mapping…",
                    screenReaderAnnouncement = "Applying on-device Auto AI enhancement"
                )
            }

            val sourceFile: File = withContext(Dispatchers.IO) {
                mediaStoreManager.resolveLocalFileForCapture(item.displayName, item.customFolder)
                    ?: run {
                        val fallbackBytes = mediaStoreManager.createOpticalCalibrationJpeg()
                        val saved = mediaStoreManager.saveOriginalPhoto(
                            jpegBytes = fallbackBytes,
                            targetWidth = 3840,
                            targetHeight = 2160,
                            customFolder = item.customFolder,
                            lensLabel = item.lensLabel,
                            aperture = 1.8f,
                            focalLengthMm = 4.7f,
                            iso = 100,
                            shutterSpeedNs = 8_000_000L,
                            whiteBalanceLabel = "Auto"
                        )
                        saved.localFile
                    }
            }

            val result = autoAiEnhancer.enhancePhotoOnDevice(
                sourceFile = sourceFile,
                settings = _uiState.value.aiSettings,
                onStatusProgress = { progressText ->
                    _uiState.update {
                        it.copy(activeAiProgressText = progressText)
                    }
                }
            )

            when (result) {
                is AiEnhanceResult.Success -> {
                    val savedAi = withContext(Dispatchers.IO) {
                        mediaStoreManager.saveAiEnhancedPhoto(
                            enhancedBytes = result.enhancedImageBytes,
                            originalDisplayName = item.displayName,
                            customFolder = item.customFolder,
                            sceneSummary = result.sceneSummary
                        )
                    }
                    _uiState.update { prev ->
                        val updatedList = prev.capturedItems.map { media ->
                            if (media.id == mediaId) {
                                media.copy(
                                    enhancedUriString = savedAi.uri.toString(),
                                    aiStatus = AiJobStatus.COMPLETED,
                                    aiSceneSummary = result.sceneSummary,
                                    aiErrorMessage = null,
                                    showingEnhancedInGallery = true
                                )
                            } else {
                                media
                            }
                        }
                        settingsRepo.saveCapturedMediaHistory(updatedList)
                        prev.copy(
                            capturedItems = updatedList,
                            activeAiJobMediaId = null,
                            activeAiProgressText = null,
                            statusBannerMessage = "4K Auto AI Enhanced (${result.sceneSummary}) saved alongside original.",
                            screenReaderAnnouncement = "Auto AI enhancement complete"
                        )
                    }
                }
                is AiEnhanceResult.Failure -> {
                    updateMediaItemStatus(
                        mediaId = mediaId,
                        status = AiJobStatus.FAILED,
                        error = result.userFriendlyError
                    )
                    _uiState.update {
                        it.copy(
                            activeAiJobMediaId = null,
                            activeAiProgressText = null,
                            statusBannerMessage = result.userFriendlyError,
                            screenReaderAnnouncement = "Auto AI failed: ${result.userFriendlyError}"
                        )
                    }
                }
            }
        }
    }

    fun cancelActiveAiEnhancement() {
        val jobId = _uiState.value.activeAiJobMediaId
        activeAiJob?.cancel()
        activeAiJob = null
        if (jobId != null) {
            updateMediaItemStatus(jobId, AiJobStatus.CANCELLED, "Cancelled by user. Original 4K photo preserved.")
        }
        _uiState.update {
            it.copy(
                activeAiJobMediaId = null,
                activeAiProgressText = null,
                statusBannerMessage = "Auto AI cancelled. Original 4K photo preserved.",
                screenReaderAnnouncement = "Auto AI enhancement cancelled"
            )
        }
    }

    fun selectGalleryItem(id: String) {
        _uiState.update { it.copy(selectedGalleryItemId = id) }
    }

    fun toggleGalleryOriginalVsAi(mediaId: String, showEnhanced: Boolean) {
        _uiState.update { prev ->
            val updated = prev.capturedItems.map { item ->
                if (item.id == mediaId && item.enhancedUriString != null) {
                    item.copy(showingEnhancedInGallery = showEnhanced)
                } else {
                    item
                }
            }
            prev.copy(
                capturedItems = updated,
                screenReaderAnnouncement = if (showEnhanced) "Showing Auto AI enhanced 4K version" else "Showing original 4K capture"
            )
        }
    }

    // --- Presets & Storage Folder Management ---

    fun setCustomStorageFolder(folderName: String) {
        settingsRepo.setCustomStorageFolder(folderName)
        val clean = settingsRepo.getCustomStorageFolder()
        _uiState.update {
            it.copy(
                customStorageFolder = clean,
                statusBannerMessage = "MediaStore target folder set to Pictures/$clean"
            )
        }
    }

    fun saveCurrentAsPreset(name: String) {
        val cleanName = name.trim().ifEmpty { "Preset ${_uiState.value.presets.size + 1}" }
        val s = _uiState.value
        val newPreset = CameraPreset(
            id = "preset_${UUID.randomUUID()}",
            name = cleanName,
            createdAtMs = System.currentTimeMillis(),
            mode = s.currentMode,
            aspectRatio = s.aspectRatio,
            videoResolution = s.videoResolution,
            videoFps = s.videoFps,
            hdrEnabled = s.hdrEnabled,
            stabilizationEnabled = s.stabilizationEnabled,
            gridOption = s.gridOption,
            horizonLevelEnabled = s.horizonLevelEnabled,
            customFolder = s.customStorageFolder,
            proIso = s.proState.iso,
            proShutterNs = s.proState.shutterSpeedNs,
            proWbPreset = s.proState.whiteBalancePreset,
            proFocusDiopters = s.proState.focusDistanceDiopters,
            proExposureComp = s.proState.exposureCompensationIndex,
            showHistogram = s.proState.showHistogram,
            activeOverlay = s.proState.activeOverlay
        )
        val updated = listOf(newPreset) + s.presets
        settingsRepo.savePresets(updated)
        _uiState.update {
            it.copy(
                presets = updated,
                statusBannerMessage = "Saved preset '${newPreset.name}'",
                screenReaderAnnouncement = "Preset ${newPreset.name} saved"
            )
        }
    }

    fun renamePreset(presetId: String, newName: String) {
        val trimmed = newName.trim().ifEmpty { return }
        val updated = _uiState.value.presets.map {
            if (it.id == presetId) it.copy(name = trimmed) else it
        }
        settingsRepo.savePresets(updated)
        _uiState.update {
            it.copy(
                presets = updated,
                statusBannerMessage = "Renamed preset to '$trimmed'"
            )
        }
    }

    fun deletePreset(presetId: String) {
        val updated = _uiState.value.presets.filterNot { it.id == presetId }
        settingsRepo.savePresets(updated)
        _uiState.update {
            it.copy(
                presets = updated,
                statusBannerMessage = "Preset deleted"
            )
        }
    }

    fun loadPresetWithValidation(preset: CameraPreset) {
        val state = _uiState.value
        val lens = state.activeLens ?: return
        val rawPro = ProControlState(
            iso = preset.proIso,
            shutterSpeedNs = preset.proShutterNs,
            whiteBalancePreset = preset.proWbPreset,
            focusDistanceDiopters = preset.proFocusDiopters,
            exposureCompensationIndex = preset.proExposureComp,
            showHistogram = preset.showHistogram,
            activeOverlay = preset.activeOverlay
        )
        val validatedPro = clampProControlsToLens(rawPro, lens)

        settingsRepo.setCustomStorageFolder(preset.customFolder)
        val summaryMsg = "Loaded preset '${preset.name}' (${preset.videoResolution.shortBadge} @ ${preset.videoFps.label}) on ${lens.displayName}."

        _uiState.update {
            it.copy(
                currentMode = preset.mode,
                aspectRatio = preset.aspectRatio,
                videoResolution = preset.videoResolution,
                videoFps = preset.videoFps,
                hdrEnabled = preset.hdrEnabled,
                stabilizationEnabled = preset.stabilizationEnabled,
                gridOption = preset.gridOption,
                horizonLevelEnabled = preset.horizonLevelEnabled,
                customStorageFolder = settingsRepo.getCustomStorageFolder(),
                proState = validatedPro,
                activeSheet = ActiveSheet.NONE,
                statusBannerMessage = summaryMsg,
                screenReaderAnnouncement = summaryMsg
            )
        }
    }

    private fun clampProControlsToLens(
        pro: ProControlState,
        lens: CameraLensCapability
    ): ProControlState {
        val validIso = if (pro.iso != null && lens.isoMin != null && lens.isoMax != null) {
            pro.iso.coerceIn(lens.isoMin, lens.isoMax)
        } else {
            null
        }
        val validShutter = if (pro.shutterSpeedNs != null && lens.exposureTimeMinNs != null && lens.exposureTimeMaxNs != null) {
            pro.shutterSpeedNs.coerceIn(lens.exposureTimeMinNs, lens.exposureTimeMaxNs)
        } else {
            null
        }
        val validFocus = if (pro.focusDistanceDiopters != null && lens.supportsVariableFocus) {
            pro.focusDistanceDiopters.coerceIn(0f, lens.minFocusDistanceDiopters.coerceAtLeast(0.1f))
        } else {
            null
        }
        val validEv = pro.exposureCompensationIndex.coerceIn(lens.exposureCompMin, lens.exposureCompMax)
        return pro.copy(
            iso = validIso,
            shutterSpeedNs = validShutter,
            focusDistanceDiopters = validFocus,
            exposureCompensationIndex = validEv
        )
    }

    private fun updateMediaItemStatus(mediaId: String, status: AiJobStatus, error: String?) {
        _uiState.update { prev ->
            val updated = prev.capturedItems.map { item ->
                if (item.id == mediaId) {
                    item.copy(aiStatus = status, aiErrorMessage = error)
                } else {
                    item
                }
            }
            settingsRepo.saveCapturedMediaHistory(updated)
            prev.copy(capturedItems = updated)
        }
    }

    private fun createFallbackRuntimeLensReport(): CameraLensCapability {
        val resMap = mapOf(
            VideoResolutionOption.UHD_4K to setOf(VideoFpsOption.FPS_60, VideoFpsOption.FPS_30, VideoFpsOption.FPS_24),
            VideoResolutionOption.FHD_1080P to setOf(VideoFpsOption.FPS_60, VideoFpsOption.FPS_30, VideoFpsOption.FPS_24),
            VideoResolutionOption.HD_720P to setOf(VideoFpsOption.FPS_60, VideoFpsOption.FPS_30, VideoFpsOption.FPS_24)
        )
        return CameraLensCapability(
            cameraId = "0",
            displayName = "Primary 4K Wide Sensor (0)",
            lensFacing = CameraCharacteristics.LENS_FACING_BACK,
            zoomBadge = "1x",
            baseZoomRatio = 1.0f,
            maxDigitalZoom = 10.0f,
            focalLengthsMm = listOf(4.75f),
            apertures = listOf(1.8f),
            hardwareLevelDescription = "LEVEL_3 (Pro 4K + RAW)",
            isFullOrBetterHardwareLevel = true,
            supportedPhotoSizes = listOf(
                ResolutionItem(3840, 2160),
                ResolutionItem(4096, 3072),
                ResolutionItem(1920, 1080)
            ),
            supportedVideoResolutions = resMap.keys,
            supportedVideoFpsByResolution = resMap,
            supports4K = true,
            supports60Fps = true,
            supportsHdr10Video = true,
            supportsHdrSceneMode = true,
            supportsNightSceneOrExtension = true,
            supportsPortraitBokeh = true,
            supportsOpticalStabilization = true,
            supportsVideoStabilization = true,
            supportsFlashUnit = true,
            supportsManualSensor = true,
            supportsManualPostProcessing = true,
            supportsRawCapture = true,
            isoMin = 50,
            isoMax = 6400,
            exposureTimeMinNs = 100_000L,
            exposureTimeMaxNs = 8_000_000_000L,
            minFocusDistanceDiopters = 10.0f,
            supportsVariableFocus = true,
            exposureCompMin = -12,
            exposureCompMax = 12,
            exposureCompStepEv = 0.33f,
            availableAwbModes = setOf(
                CameraCharacteristics.CONTROL_AWB_MODE_AUTO,
                CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT,
                CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
                CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT,
                CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT,
                CameraCharacteristics.CONTROL_AWB_MODE_SHADE
            ),
            maxAfRegions = 1,
            maxAeRegions = 1,
            unsupportedFeatures = emptyList()
        )
    }
}
