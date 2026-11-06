package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AiEnhanceSettings
import com.example.model.AiJobStatus
import com.example.model.AiSceneProfile
import com.example.model.AspectRatioOption
import com.example.model.CameraMode
import com.example.model.CameraPreset
import com.example.model.CapturedMediaItem
import com.example.model.GridOption
import com.example.model.ProOverlayType
import com.example.model.StabilizationMode
import com.example.model.VideoFpsOption
import com.example.model.VideoResolutionOption
import com.example.model.WhiteBalancePreset
import org.json.JSONArray
import org.json.JSONObject

class SecureSettingsRepository(context: Context) {

    private val appPrefs: SharedPreferences =
        context.getSharedPreferences("lumina_600_camera_prefs", Context.MODE_PRIVATE)

    fun loadAiEnhanceSettings(): AiEnhanceSettings {
        val profileName = appPrefs.getString(KEY_AI_PROFILE, AiSceneProfile.AUTO_SCENE.name)
            ?: AiSceneProfile.AUTO_SCENE.name
        val profile = runCatching { AiSceneProfile.valueOf(profileName) }
            .getOrDefault(AiSceneProfile.AUTO_SCENE)

        return AiEnhanceSettings(
            enabled = appPrefs.getBoolean(KEY_AI_ENABLED, true),
            autoEnhanceAfterCapture = appPrefs.getBoolean(KEY_AI_AUTO, true),
            sceneProfile = profile,
            intensityPercent = appPrefs.getInt(KEY_AI_INTENSITY, 85),
            smartHdrToneMap = appPrefs.getBoolean(KEY_AI_HDR_TONEMAP, true),
            detailSharpening = appPrefs.getBoolean(KEY_AI_SHARPEN, true),
            aiZoomSuperRes = appPrefs.getBoolean(KEY_AI_ZOOM_SUPERRES, true)
        )
    }

    fun saveAiEnhanceSettings(settings: AiEnhanceSettings) {
        appPrefs.edit()
            .putBoolean(KEY_AI_ENABLED, settings.enabled)
            .putBoolean(KEY_AI_AUTO, settings.autoEnhanceAfterCapture)
            .putString(KEY_AI_PROFILE, settings.sceneProfile.name)
            .putInt(KEY_AI_INTENSITY, settings.intensityPercent)
            .putBoolean(KEY_AI_HDR_TONEMAP, settings.smartHdrToneMap)
            .putBoolean(KEY_AI_SHARPEN, settings.detailSharpening)
            .putBoolean(KEY_AI_ZOOM_SUPERRES, settings.aiZoomSuperRes)
            .apply()
    }

    fun isHalBypassEnabled(): Boolean = appPrefs.getBoolean(KEY_HAL_BYPASS, true)

    fun setHalBypassEnabled(enabled: Boolean) {
        appPrefs.edit().putBoolean(KEY_HAL_BYPASS, enabled).apply()
    }

    fun isRawCaptureEnabled(): Boolean = appPrefs.getBoolean(KEY_RAW_DNG_ENABLED, true)

    fun setRawCaptureEnabled(enabled: Boolean) {
        appPrefs.edit().putBoolean(KEY_RAW_DNG_ENABLED, enabled).apply()
    }

    fun getStabilizationMode(): StabilizationMode {
        val name = appPrefs.getString(KEY_STABILIZATION_MODE, StabilizationMode.HYBRID_OIS_EIS.name)
            ?: StabilizationMode.HYBRID_OIS_EIS.name
        return runCatching { StabilizationMode.valueOf(name) }
            .getOrDefault(StabilizationMode.HYBRID_OIS_EIS)
    }

    fun setStabilizationMode(mode: StabilizationMode) {
        appPrefs.edit().putString(KEY_STABILIZATION_MODE, mode.name).apply()
    }

    fun getCustomStorageFolder(): String {
        val raw = appPrefs.getString(KEY_CUSTOM_FOLDER, "Lumina600")?.trim().orEmpty()
        return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "").ifEmpty { "Lumina600" }
    }

    fun setCustomStorageFolder(folder: String) {
        val sanitized = folder.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "").ifEmpty { "Lumina600" }
        appPrefs.edit().putString(KEY_CUSTOM_FOLDER, sanitized).apply()
    }

    fun loadPresets(): List<CameraPreset> {
        val raw = appPrefs.getString(KEY_PRESETS_JSON, null)
        if (raw.isNullOrBlank()) {
            val defaults = defaultPresets()
            savePresets(defaults)
            return defaults
        }
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CameraPreset>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CameraPreset(
                        id = obj.optString("id", "preset_$i"),
                        name = obj.optString("name", "Preset ${i + 1}"),
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                        mode = runCatching { CameraMode.valueOf(obj.optString("mode")) }
                            .getOrDefault(CameraMode.PRO_PHOTO),
                        aspectRatio = runCatching { AspectRatioOption.valueOf(obj.optString("aspectRatio")) }
                            .getOrDefault(AspectRatioOption.RATIO_16_9),
                        videoResolution = runCatching { VideoResolutionOption.valueOf(obj.optString("videoResolution")) }
                            .getOrDefault(VideoResolutionOption.UHD_4K),
                        videoFps = runCatching { VideoFpsOption.valueOf(obj.optString("videoFps")) }
                            .getOrDefault(VideoFpsOption.FPS_60),
                        hdrEnabled = obj.optBoolean("hdrEnabled", true),
                        stabilizationEnabled = obj.optBoolean("stabilizationEnabled", true),
                        gridOption = runCatching { GridOption.valueOf(obj.optString("gridOption")) }
                            .getOrDefault(GridOption.RULE_OF_THIRDS),
                        horizonLevelEnabled = obj.optBoolean("horizonLevelEnabled", true),
                        customFolder = obj.optString("customFolder", "Lumina600"),
                        proIso = if (obj.has("proIso") && !obj.isNull("proIso")) obj.getInt("proIso") else null,
                        proShutterNs = if (obj.has("proShutterNs") && !obj.isNull("proShutterNs")) obj.getLong("proShutterNs") else null,
                        proWbPreset = runCatching { WhiteBalancePreset.valueOf(obj.optString("proWbPreset")) }
                            .getOrDefault(WhiteBalancePreset.AUTO),
                        proFocusDiopters = if (obj.has("proFocusDiopters") && !obj.isNull("proFocusDiopters")) obj.getDouble("proFocusDiopters").toFloat() else null,
                        proExposureComp = obj.optInt("proExposureComp", 0),
                        showHistogram = obj.optBoolean("showHistogram", true),
                        activeOverlay = runCatching { ProOverlayType.valueOf(obj.optString("activeOverlay")) }
                            .getOrDefault(ProOverlayType.NONE)
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultPresets()
        }
    }

    fun savePresets(presets: List<CameraPreset>) {
        val arr = JSONArray()
        presets.forEach { preset ->
            val obj = JSONObject().apply {
                put("id", preset.id)
                put("name", preset.name)
                put("createdAtMs", preset.createdAtMs)
                put("mode", preset.mode.name)
                put("aspectRatio", preset.aspectRatio.name)
                put("videoResolution", preset.videoResolution.name)
                put("videoFps", preset.videoFps.name)
                put("hdrEnabled", preset.hdrEnabled)
                put("stabilizationEnabled", preset.stabilizationEnabled)
                put("gridOption", preset.gridOption.name)
                put("horizonLevelEnabled", preset.horizonLevelEnabled)
                put("customFolder", preset.customFolder)
                if (preset.proIso != null) put("proIso", preset.proIso) else put("proIso", JSONObject.NULL)
                if (preset.proShutterNs != null) put("proShutterNs", preset.proShutterNs) else put("proShutterNs", JSONObject.NULL)
                put("proWbPreset", preset.proWbPreset.name)
                if (preset.proFocusDiopters != null) put("proFocusDiopters", preset.proFocusDiopters.toDouble()) else put("proFocusDiopters", JSONObject.NULL)
                put("proExposureComp", preset.proExposureComp)
                put("showHistogram", preset.showHistogram)
                put("activeOverlay", preset.activeOverlay.name)
            }
            arr.put(obj)
        }
        appPrefs.edit().putString(KEY_PRESETS_JSON, arr.toString()).apply()
    }

    fun loadCapturedMediaHistory(): List<CapturedMediaItem> {
        val raw = appPrefs.getString(KEY_MEDIA_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CapturedMediaItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CapturedMediaItem(
                        id = obj.getString("id"),
                        originalUriString = obj.getString("originalUriString"),
                        enhancedUriString = obj.optString("enhancedUriString", "").ifBlank { null },
                        rawDngUriString = obj.optString("rawDngUriString", "").ifBlank { null },
                        rawDngFileName = obj.optString("rawDngFileName", "").ifBlank { null },
                        displayName = obj.optString("displayName", "IMG_${i + 1}.jpg"),
                        isVideo = obj.optBoolean("isVideo", false),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
                        width = obj.optInt("width", 3840),
                        height = obj.optInt("height", 2160),
                        lensLabel = obj.optString("lensLabel", "1x Wide"),
                        modeLabel = obj.optString("modeLabel", "PHOTO 4K"),
                        exifSummary = obj.optString("exifSummary", "RAW DNG + 4K UHD • ISO 100"),
                        customFolder = obj.optString("customFolder", "Lumina600"),
                        aiStatus = runCatching { AiJobStatus.valueOf(obj.optString("aiStatus")) }
                            .getOrDefault(AiJobStatus.NONE),
                        aiSceneSummary = obj.optString("aiSceneSummary", "").ifBlank { null },
                        aiErrorMessage = obj.optString("aiErrorMessage", "").ifBlank { null },
                        showingEnhancedInGallery = obj.optBoolean("showingEnhancedInGallery", true)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCapturedMediaHistory(items: List<CapturedMediaItem>) {
        val arr = JSONArray()
        items.take(60).forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalUriString", item.originalUriString)
                put("enhancedUriString", item.enhancedUriString ?: "")
                put("rawDngUriString", item.rawDngUriString ?: "")
                put("rawDngFileName", item.rawDngFileName ?: "")
                put("displayName", item.displayName)
                put("isVideo", item.isVideo)
                put("timestampMs", item.timestampMs)
                put("width", item.width)
                put("height", item.height)
                put("lensLabel", item.lensLabel)
                put("modeLabel", item.modeLabel)
                put("exifSummary", item.exifSummary)
                put("customFolder", item.customFolder)
                put("aiStatus", item.aiStatus.name)
                put("aiSceneSummary", item.aiSceneSummary ?: "")
                put("aiErrorMessage", item.aiErrorMessage ?: "")
                put("showingEnhancedInGallery", item.showingEnhancedInGallery)
            }
            arr.put(obj)
        }
        appPrefs.edit().putString(KEY_MEDIA_HISTORY_JSON, arr.toString()).apply()
    }

    private fun defaultPresets(): List<CameraPreset> {
        val now = System.currentTimeMillis()
        return listOf(
            CameraPreset(
                id = "preset_4k60_action",
                name = "4K 60fps Hybrid OIS+EIS",
                createdAtMs = now - 86_400_000L,
                mode = CameraMode.VIDEO,
                aspectRatio = AspectRatioOption.RATIO_16_9,
                videoResolution = VideoResolutionOption.UHD_4K,
                videoFps = VideoFpsOption.FPS_60,
                hdrEnabled = true,
                stabilizationEnabled = true,
                gridOption = GridOption.RULE_OF_THIRDS,
                horizonLevelEnabled = true,
                customFolder = "Lumina600",
                proIso = null,
                proShutterNs = null,
                proWbPreset = WhiteBalancePreset.AUTO,
                proFocusDiopters = null,
                proExposureComp = 0,
                showHistogram = true,
                activeOverlay = ProOverlayType.NONE
            ),
            CameraPreset(
                id = "preset_raw_4k_pro",
                name = "RAW DNG + 4K Auto AI Master",
                createdAtMs = now - 43_200_000L,
                mode = CameraMode.PRO_PHOTO,
                aspectRatio = AspectRatioOption.RATIO_16_9,
                videoResolution = VideoResolutionOption.UHD_4K,
                videoFps = VideoFpsOption.FPS_60,
                hdrEnabled = true,
                stabilizationEnabled = true,
                gridOption = GridOption.RULE_OF_THIRDS,
                horizonLevelEnabled = true,
                customFolder = "LuminaRAW",
                proIso = 100,
                proShutterNs = 4_000_000L,
                proWbPreset = WhiteBalancePreset.DAYLIGHT,
                proFocusDiopters = null,
                proExposureComp = 0,
                showHistogram = true,
                activeOverlay = ProOverlayType.FOCUS_PEAKING
            )
        )
    }

    companion object {
        private const val KEY_AI_ENABLED = "ai_ondevice_enabled"
        private const val KEY_AI_AUTO = "ai_ondevice_auto"
        private const val KEY_AI_PROFILE = "ai_ondevice_profile"
        private const val KEY_AI_INTENSITY = "ai_ondevice_intensity"
        private const val KEY_AI_HDR_TONEMAP = "ai_ondevice_hdr"
        private const val KEY_AI_SHARPEN = "ai_ondevice_sharpen"
        private const val KEY_AI_ZOOM_SUPERRES = "ai_ondevice_zoom_superres"
        private const val KEY_HAL_BYPASS = "camera2_hal_bypass_enabled"
        private const val KEY_RAW_DNG_ENABLED = "raw_dng_capture_enabled"
        private const val KEY_STABILIZATION_MODE = "stabilization_mode_v2"
        private const val KEY_CUSTOM_FOLDER = "custom_storage_folder"
        private const val KEY_PRESETS_JSON = "camera_presets_v3_json"
        private const val KEY_MEDIA_HISTORY_JSON = "captured_media_history_v3_json"
    }
}
