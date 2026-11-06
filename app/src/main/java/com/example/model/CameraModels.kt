package com.example.model

import android.hardware.camera2.CameraCharacteristics
import kotlin.math.roundToInt

enum class CameraMode(
    val displayName: String,
    val shortLabel: String,
    val isVideoMode: Boolean,
    val isProMode: Boolean
) {
    NIGHT("Night", "NIGHT", isVideoMode = false, isProMode = false),
    PORTRAIT("Portrait", "PORTRAIT", isVideoMode = false, isProMode = false),
    PHOTO("Photo", "PHOTO", isVideoMode = false, isProMode = false),
    VIDEO("Video", "VIDEO", isVideoMode = true, isProMode = false),
    PRO_PHOTO("Pro Photo", "PRO PHOTO", isVideoMode = false, isProMode = true),
    PRO_VIDEO("Pro Video", "PRO VIDEO", isVideoMode = true, isProMode = true)
}

enum class AspectRatioOption(
    val label: String,
    val widthRatio: Int,
    val heightRatio: Int
) {
    RATIO_16_9("16:9 (4K)", 16, 9),
    RATIO_4_3("4:3", 4, 3),
    RATIO_1_1("1:1", 1, 1),
    RATIO_FULL("FULL", 20, 9);

    val floatRatio: Float
        get() = widthRatio.toFloat() / heightRatio.toFloat()
}

enum class FlashOption(val label: String) {
    OFF("Off"),
    AUTO("Auto"),
    ON("On"),
    TORCH("Torch")
}

enum class TimerOption(val label: String, val seconds: Int) {
    OFF("Off", 0),
    SEC_3("3s", 3),
    SEC_5("5s", 5),
    SEC_10("10s", 10)
}

enum class GridOption(val label: String) {
    OFF("Off"),
    RULE_OF_THIRDS("3x3 Grid"),
    GOLDEN_RATIO("Phi Grid"),
    CROSSHAIR("Center Cross")
}

enum class StabilizationMode(
    val badge: String,
    val fullName: String,
    val useOis: Boolean,
    val useEis: Boolean
) {
    HYBRID_OIS_EIS("OIS+EIS", "Hybrid Optical (OIS) + Electronic (EIS) Stabilization", true, true),
    OIS_ONLY("OIS", "Optical Image Stabilization (Sensor/Lens Shift)", true, false),
    EIS_ONLY("EIS", "Electronic Gyro-Horizon Stabilization", false, true),
    OFF("STAB OFF", "Stabilization Off (Tripod Mode)", false, false);

    val shortBadge: String get() = badge
    val label: String get() = badge
}

enum class OptimalZoomStop(
    val ratio: Float,
    val label: String,
    val focalEquivalentMm: Int,
    val description: String
) {
    ULTRA_WIDE_0_6X(0.6f, "0.6x", 14, "Ultra-Wide 14mm Optic"),
    MAIN_1X(1.0f, "1x", 24, "Primary 24mm Wide Sensor"),
    LOSSLESS_2X(2.0f, "2x", 48, "Optimal 2x In-Sensor Lossless Zoom"),
    PORTRAIT_3_5X(3.5f, "3.5x", 85, "Optimal 85mm Telephoto Portrait"),
    HYBRID_5X(5.0f, "5x", 120, "5x Hybrid Optical + AI Super-Res"),
    AI_SUPER_10X(10.0f, "10x", 240, "10x AI Multi-Frame Super-Resolution");

    val focalEquivalent: String get() = "${focalEquivalentMm}mm $description"
}

enum class VideoResolutionOption(
    val label: String,
    val shortBadge: String,
    val width: Int,
    val height: Int
) {
    UHD_4K("UHD 4K (3840×2160)", "4K", 3840, 2160),
    FHD_1080P("FHD 1080p (1920×1080)", "FHD", 1920, 1080),
    HD_720P("HD 720p (1280×720)", "HD", 1280, 720)
}

enum class VideoFpsOption(val label: String, val fps: Int) {
    FPS_60("60 fps", 60),
    FPS_30("30 fps", 30),
    FPS_24("24 fps", 24)
}

enum class ProOverlayType(val label: String, val shortTag: String) {
    NONE("Off", "OFF"),
    ZEBRA("Zebra Stripes", "ZEBRA"),
    FOCUS_PEAKING("Focus Peaking", "PEAK"),
    FALSE_COLOR("False Color IRE", "FALSE")
}

enum class WhiteBalancePreset(
    val label: String,
    val awbModeConstant: Int,
    val approxKelvin: Int
) {
    AUTO("Auto", CameraCharacteristics.CONTROL_AWB_MODE_AUTO, 5500),
    DAYLIGHT("Daylight", CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT, 5600),
    CLOUDY("Cloudy", CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT, 6500),
    TUNGSTEN("Incandescent", CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT, 3200),
    FLUORESCENT("Fluorescent", CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT, 4000),
    SHADE("Shade", CameraCharacteristics.CONTROL_AWB_MODE_SHADE, 7500),
    MANUAL_KELVIN("Custom K", CameraCharacteristics.CONTROL_AWB_MODE_OFF, 5200)
}

enum class AiSceneProfile(
    val displayName: String,
    val badge: String,
    val description: String
) {
    AUTO_SCENE(
        "Auto AI Scene Recognition",
        "AI AUTO",
        "Automatically detects scene lighting, boosts dynamic range, and optimizes color & 4K optical zoom clarity after every capture."
    ),
    VIVID_HDR(
        "AI Vivid & Sky HDR",
        "AI VIVID",
        "Rich saturation, deep sky blues, and lifted shadow detail for outdoor & landscape 4K shots."
    ),
    NATURAL_OPTIC(
        "AI Natural True-Tone",
        "AI NATURAL",
        "Balanced contrast and accurate white balance with subtle micro-contrast sharpening."
    ),
    PORTRAIT_GLOW(
        "AI Portrait Skin & Bokeh Tone",
        "AI PORTRAIT",
        "Warm skin-tone protection with gentle highlight roll-off and eye/edge clarity."
    ),
    NIGHT_VISION(
        "AI Night Vision & Denoise",
        "AI NIGHT",
        "Brightens low-light shadows, suppresses chroma noise, and enhances warm city lights."
    )
}

data class UnsupportedFeatureReason(
    val featureName: String,
    val reason: String,
    val characteristicKey: String
)

data class ResolutionItem(
    val width: Int,
    val height: Int
) {
    val is4kUhd: Boolean
        get() = (width == 3840 && height == 2160) || (width >= 3840 && height >= 2160)

    val megapixels: Float
        get() = ((width.toLong() * height.toLong()) / 100_000f).roundToInt() / 10f

    val label: String
        get() = if (width == 3840 && height == 2160) {
            "4K UHD (3840×2160 • 8.3MP)"
        } else {
            "${width}×${height} (${megapixels}MP)"
        }
}

data class CameraLensCapability(
    val cameraId: String,
    val displayName: String,
    val lensFacing: Int,
    val zoomBadge: String,
    val baseZoomRatio: Float,
    val maxDigitalZoom: Float,
    val focalLengthsMm: List<Float>,
    val apertures: List<Float>,
    val hardwareLevelDescription: String,
    val isFullOrBetterHardwareLevel: Boolean,
    val supportedPhotoSizes: List<ResolutionItem>,
    val supportedVideoResolutions: Set<VideoResolutionOption>,
    val supportedVideoFpsByResolution: Map<VideoResolutionOption, Set<VideoFpsOption>>,
    val supports4K: Boolean,
    val supports60Fps: Boolean,
    val supportsHdr10Video: Boolean,
    val supportsHdrSceneMode: Boolean,
    val supportsNightSceneOrExtension: Boolean,
    val supportsPortraitBokeh: Boolean,
    val supportsOpticalStabilization: Boolean,
    val supportsVideoStabilization: Boolean,
    val supportsFlashUnit: Boolean,
    val supportsManualSensor: Boolean,
    val supportsManualPostProcessing: Boolean,
    val supportsRawCapture: Boolean,
    val isoMin: Int?,
    val isoMax: Int?,
    val exposureTimeMinNs: Long?,
    val exposureTimeMaxNs: Long?,
    val minFocusDistanceDiopters: Float,
    val supportsVariableFocus: Boolean,
    val exposureCompMin: Int,
    val exposureCompMax: Int,
    val exposureCompStepEv: Float,
    val availableAwbModes: Set<Int>,
    val maxAfRegions: Int,
    val maxAeRegions: Int,
    val unsupportedFeatures: List<UnsupportedFeatureReason>
)

data class ProControlState(
    val iso: Int? = null,
    val shutterSpeedNs: Long? = null,
    val whiteBalancePreset: WhiteBalancePreset = WhiteBalancePreset.AUTO,
    val customKelvin: Int = 5500,
    val focusDistanceDiopters: Float? = null,
    val exposureCompensationIndex: Int = 0,
    val aeLocked: Boolean = false,
    val afLocked: Boolean = false,
    val showHistogram: Boolean = true,
    val activeOverlay: ProOverlayType = ProOverlayType.NONE,
    val zebraThresholdPercent: Int = 92
)

data class FrameTelemetry(
    val lumaBins: IntArray = IntArray(64),
    val redBins: IntArray = IntArray(64),
    val greenBins: IntArray = IntArray(64),
    val blueBins: IntArray = IntArray(64),
    val averageLuma: Int = 128,
    val highlightClipPercent: Float = 0f,
    val shadowClipPercent: Float = 0f,
    val dynamicRangeStopsEstimate: Float = 11.2f,
    val gridWidth: Int = 48,
    val gridHeight: Int = 36,
    val overlayCells: IntArray = IntArray(48 * 36)
)

data class AiEnhanceSettings(
    val enabled: Boolean = true,
    val autoEnhanceAfterCapture: Boolean = true,
    val sceneProfile: AiSceneProfile = AiSceneProfile.AUTO_SCENE,
    val intensityPercent: Int = 85,
    val smartHdrToneMap: Boolean = true,
    val detailSharpening: Boolean = true,
    val aiZoomSuperRes: Boolean = true
)

enum class AiJobStatus(val badgeText: String) {
    NONE("Original 4K"),
    ENHANCING("Auto AI ISP…"),
    COMPLETED("AI + RAW Ready"),
    FAILED("AI Failed"),
    CANCELLED("Cancelled")
}

data class CapturedMediaItem(
    val id: String,
    val originalUriString: String,
    val enhancedUriString: String? = null,
    val rawDngUriString: String? = null,
    val rawDngFileName: String? = null,
    val displayName: String,
    val isVideo: Boolean = false,
    val timestampMs: Long,
    val width: Int,
    val height: Int,
    val lensLabel: String,
    val modeLabel: String,
    val exifSummary: String,
    val customFolder: String,
    val aiStatus: AiJobStatus = AiJobStatus.NONE,
    val aiSceneSummary: String? = null,
    val aiErrorMessage: String? = null,
    val showingEnhancedInGallery: Boolean = true
)

data class CameraPreset(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val mode: CameraMode,
    val aspectRatio: AspectRatioOption,
    val videoResolution: VideoResolutionOption,
    val videoFps: VideoFpsOption,
    val hdrEnabled: Boolean,
    val stabilizationEnabled: Boolean,
    val gridOption: GridOption,
    val horizonLevelEnabled: Boolean,
    val customFolder: String,
    val proIso: Int?,
    val proShutterNs: Long?,
    val proWbPreset: WhiteBalancePreset,
    val proFocusDiopters: Float?,
    val proExposureComp: Int,
    val showHistogram: Boolean,
    val activeOverlay: ProOverlayType
)

enum class ThermalHealthLevel(
    val label: String,
    val detail: String,
    val throttlesHeavyFeatures: Boolean
) {
    NOMINAL("Nominal (Cool)", "Camera2 HAL Bypass, 4K/60, RAW DNG, OIS+EIS & Auto AI active.", false),
    LIGHT("Light Warmth", "Normal operating temperature.", false),
    MODERATE("Warm", "Monitoring sensor heat; 4K/60, RAW & OIS+EIS active.", false),
    SEVERE("Hot — Throttling", "Frame analysis throttled to cool device.", true),
    CRITICAL("Critical Heat", "High-power live overlays paused to protect hardware.", true)
}
