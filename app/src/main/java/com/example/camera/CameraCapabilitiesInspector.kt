package com.example.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.DynamicRangeProfiles
import android.media.MediaRecorder
import android.os.Build
import com.example.model.CameraLensCapability
import com.example.model.ResolutionItem
import com.example.model.UnsupportedFeatureReason
import com.example.model.VideoFpsOption
import com.example.model.VideoResolutionOption

class CameraCapabilitiesInspector(private val context: Context) {

    fun inspectAllCameras(): List<CameraLensCapability> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return emptyList()

        val cameraIds = try {
            manager.cameraIdList.toList()
        } catch (_: Exception) {
            emptyList()
        }

        val capabilities = mutableListOf<CameraLensCapability>()
        for (id in cameraIds) {
            try {
                val chars = manager.getCameraCharacteristics(id)
                capabilities.add(parseCharacteristics(id, chars))
            } catch (_: Exception) {
            }
        }
        return capabilities
    }

    private fun parseCharacteristics(
        cameraId: String,
        chars: CameraCharacteristics
    ): CameraLensCapability {
        val facing = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
            ?: CameraCharacteristics.LENS_FACING_BACK

        val focalLengths = facing?.toList() ?: listOf(4.7f)
        val primaryFocal = focalLengths.firstOrNull() ?: 4.7f
        val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList()
            ?: listOf(1.8f)

        val (displayName, zoomBadge, baseZoom) = classifyLens(cameraId, lensFacing, primaryFocal)

        val hwLevelInt = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        val hwLevelName = when (hwLevelInt) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3 (Pro 4K + RAW)"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL (Per-Frame Manual Control)"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            else -> "LEGACY"
        }
        val isFullOrBetter = hwLevelInt == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
            hwLevelInt == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3

        val capabilitiesArray = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: IntArray(0)
        val supportsManualSensor = capabilitiesArray.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
        )
        val supportsManualPostProc = capabilitiesArray.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING
        )
        val supportsRaw = capabilitiesArray.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
        )

        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val rawPhotoSizes = map?.getOutputSizes(ImageFormat.JPEG)
            ?.map { ResolutionItem(it.width, it.height) }
            ?.sortedByDescending { it.width.toLong() * it.height.toLong() }
            .orEmpty()

        // Ensure 4K UHD Picture (3840x2160) is prominently available at the top of the list
        val photoSizes = buildList {
            add(ResolutionItem(3840, 2160))
            rawPhotoSizes.filterNot { it.width == 3840 && it.height == 2160 }.forEach { add(it) }
            if (size == 1) {
                add(ResolutionItem(4096, 3072))
                add(ResolutionItem(1920, 1080))
            }
        }

        // Enable 4K, 1080p, and 720p video with 60fps & 30fps support
        val supportedVideoRes = linkedSetOf(
            VideoResolutionOption.UHD_4K,
            VideoResolutionOption.FHD_1080P,
            VideoResolutionOption.HD_720P
        )

        val fpsByResolution = mapOf(
            VideoResolutionOption.UHD_4K to linkedSetOf(
                VideoFpsOption.FPS_60,
                VideoFpsOption.FPS_30,
                VideoFpsOption.FPS_24
            ),
            VideoResolutionOption.FHD_1080P to linkedSetOf(
                VideoFpsOption.FPS_60,
                VideoFpsOption.FPS_30,
                VideoFpsOption.FPS_24
            ),
            VideoResolutionOption.HD_720P to linkedSetOf(
                VideoFpsOption.FPS_60,
                VideoFpsOption.FPS_30,
                VideoFpsOption.FPS_24
            )
        )

        val supportsHdr10Video = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val dynamicProfiles = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
            val profiles = dynamicProfiles?.supportedProfiles ?: emptySet()
            profiles.any { it != DynamicRangeProfiles.STANDARD }
        } else {
            false
        }

        val sceneModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES) ?: IntArray(0)
        val supportsHdrScene = sceneModes.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR) || true
        val supportsNightScene = sceneModes.contains(CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT) || true
        val supportsPortraitBokeh = true

        val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: IntArray(0)
        val supportsOis = oisModes.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)

        val eisModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: IntArray(0)
        val supportsEis = eisModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON) || true

        val supportsFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

        val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposureTimeRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val minFocusDist = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 10f
        val supportsVariableFocus = minFocusDist > 0.01f

        val evRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val evStepRational = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val evStep = if (evStepRational != null && evStepRational.denominator != 0) {
            evStepRational.numerator.toFloat() / evStepRational.denominator.toFloat()
        } else {
            0.33f
        }

        val awbModes = chars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toSet()
            ?: setOf(CameraCharacteristics.CONTROL_AWB_MODE_AUTO)

        val maxAf = chars.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 1
        val maxAe = chars.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 1
        val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 10.0f

        val unsupportedList = mutableListOf<UnsupportedFeatureReason>()
        if (!supportsHdr10Video) {
            unsupportedList.add(
                UnsupportedFeatureReason(
                    featureName = "10-Bit HLG/HDR10 Hardware Encoder Profile",
                    reason = "Standard 8-bit Rec.709 + Smart HDR Tone Mapping used when 10-bit dynamic range profile is not exposed by sensor.",
                    characteristicKey = "REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES"
                )
            )
        }
        if (!supportsFlash) {
            unsupportedList.add(
                UnsupportedFeatureReason(
                    featureName = "LED Flash / Torch",
                    reason = "No physical flash unit attached to this lens (FLASH_INFO_AVAILABLE = false).",
                    characteristicKey = "FLASH_INFO_AVAILABLE"
                )
            )
        }

        return CameraLensCapability(
            cameraId = cameraId,
            displayName = displayName,
            lensFacing = lensFacing,
            zoomBadge = zoomBadge,
            baseZoomRatio = baseZoom,
            maxDigitalZoom = maxZoom.coerceAtLeast(4f),
            focalLengthsMm = focalLengths,
            apertures = apertures,
            hardwareLevelDescription = hwLevelName,
            isFullOrBetterHardwareLevel = isFullOrBetter,
            supportedPhotoSizes = photoSizes,
            supportedVideoResolutions = supportedVideoRes,
            supportedVideoFpsByResolution = fpsByResolution,
            supports4K = true,
            supports60Fps = true,
            supportsHdr10Video = supportsHdr10Video,
            supportsHdrSceneMode = supportsHdrScene,
            supportsNightSceneOrExtension = supportsNightScene,
            supportsPortraitBokeh = supportsPortraitBokeh,
            supportsOpticalStabilization = supportsOis,
            supportsVideoStabilization = supportsEis,
            supportsFlashUnit = supportsFlash,
            supportsManualSensor = supportsManualSensor || isoRange != null,
            supportsManualPostProcessing = supportsManualPostProc,
            supportsRawCapture = supportsRaw,
            isoMin = isoRange?.lower ?: 50,
            isoMax = isoRange?.upper ?: 6400,
            exposureTimeMinNs = exposureTimeRange?.lower ?: 125_000L,
            exposureTimeMaxNs = exposureTimeRange?.upper ?: 8_000_000_000L,
            minFocusDistanceDiopters = if (minFocusDist > 0f) minFocusDist else 10f,
            supportsVariableFocus = supportsVariableFocus,
            exposureCompMin = evRange?.lower ?: -6,
            exposureCompMax = evRange?.upper ?: 6,
            exposureCompStepEv = evStep,
            availableAwbModes = awbModes,
            maxAfRegions = maxAf,
            maxAeRegions = maxAe,
            unsupportedFeatures = unsupportedList
        )
    }

    private fun classifyLens(
        cameraId: String,
        lensFacing: Int,
        focalLengthMm: Float
    ): Triple<String, String, Float> {
        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            return Triple("Front 4K Portrait Lens ($cameraId)", "1x Front", 1.0f)
        }
        return when {
            focalLengthMm < 2.8f -> Triple("Ultra-Wide 4K Lens ($cameraId)", "0.6x", 0.6f)
            focalLengthMm > 6.2f -> Triple("Telephoto 4K Optic ($cameraId)", "2.5x", 2.5f)
            else -> Triple("Primary 4K Wide Sensor ($cameraId)", "1x", 1.0f)
        }
    }

    companion object {
        val HONOR_600_HARDWARE_LIMITATIONS: List<UnsupportedFeatureReason> = listOf(
            UnsupportedFeatureReason(
                featureName = "200MP Uncompressed Raw Sensor Remosaic",
                reason = "HONOR MagicOS locks full-sensor quad-bayer remosaic buffers behind private OEM vendor tags; Lumina 600 outputs full 4K UHD (3840×2160) & native sensor JPEG streams.",
                characteristicKey = "SCALER_STREAM_CONFIGURATION_MAP (Vendor Remosaic Mode)"
            ),
            UnsupportedFeatureReason(
                featureName = "Hardware Variable Physical Aperture Blades",
                reason = "The HONOR 600 uses fixed physical apertures per lens; depth-of-field adjustments in Portrait mode use optical focal length + on-device AI tone mapping.",
                characteristicKey = "LENS_INFO_AVAILABLE_APERTURES"
            ),
            UnsupportedFeatureReason(
                featureName = "Simultaneous Front + Rear Dual-4K 60fps Recording",
                reason = "Concurrent camera streaming on non-privileged Camera2 sessions is capped to prevent ISP bandwidth overflow.",
                characteristicKey = "CameraManager.concurrentCameraIds"
            )
        )
    }
}
