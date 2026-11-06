package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiJobStatus
import com.example.model.CameraMode
import com.example.model.FlashOption
import com.example.model.ThermalHealthLevel
import com.example.model.WhiteBalancePreset
import com.example.ui.theme.AmberOpticPrimary
import com.example.ui.theme.CinemaCrimsonError
import com.example.ui.theme.CyanAiSecondary
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.IvoryTextPrimary
import com.example.ui.theme.SilverTextSecondary
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.ThermalWarningOrange
import com.example.viewmodel.ActiveSheet
import com.example.viewmodel.CameraUiState
import java.util.Locale

private enum class ProParameterTab(val label: String) {
    ISO("ISO"),
    SHUTTER("SHUTTER"),
    WB("WB"),
    FOCUS("FOCUS"),
    EV("EV"),
    ZEBRA("ZEBRA %")
}

@Composable
fun TopTelemetryUtilityBar(
    uiState: CameraUiState,
    onCycleFlash: () -> Unit,
    onCycleTimer: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLens = uiState.activeLens
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = GraphiteSurface.copy(alpha = 0.86f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Button
            IconButton(
                onClick = onCycleFlash,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_flash_button")
                    .semantics {
                        contentDescription = "Flash mode: ${uiState.flashOption.label}"
                    }
            ) {
                val flashIcon = when (uiState.flashOption) {
                    FlashOption.OFF -> Icons.Default.FlashOff
                    FlashOption.AUTO -> Icons.Default.FlashAuto
                    FlashOption.ON -> Icons.Default.FlashOn
                    FlashOption.TORCH -> Icons.Default.Highlight
                }
                Icon(
                    imageVector = flashIcon,
                    contentDescription = stringResource(R.string.cd_flash_toggle),
                    tint = if (uiState.flashOption == FlashOption.OFF) IvoryTextPrimary else AmberOpticPrimary
                )
            }

            // Timer Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onCycleTimer)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("top_timer_button")
                    .semantics {
                        contentDescription = "Countdown timer: ${uiState.timerOption.label}"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = stringResource(R.string.cd_timer_toggle),
                    tint = if (uiState.timerOption.seconds > 0) AmberOpticPrimary else IvoryTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = uiState.timerOption.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.timerOption.seconds > 0) AmberOpticPrimary else IvoryTextPrimary
                )
            }

            // 4K Photo / 4K 60fps-30fps Video Quick Toggle Pill
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateSurfaceVariant)
                    .border(
                        width = 1.dp,
                        color = AmberOpticPrimary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onCycleAspectRatio)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("top_aspect_or_video_badge")
                    .semantics {
                        contentDescription = "Toggle 4K resolution and 60fps or 30fps frame rate"
                    },
                contentAlignment = Alignment.Center
            ) {
                val badgeText = if (uiState.currentMode.isVideoMode) {
                    "${uiState.videoResolution.shortBadge} • ${uiState.videoFps.fps}FPS"
                } else {
                    "4K • ${uiState.aspectRatio.label}"
                }
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberOpticPrimary
                )
            }

            // On-Device Auto AI Imaging Pill (HONOR AI Camera style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (uiState.aiSettings.enabled) CyanAiSecondary.copy(alpha = 0.2f)
                        else SlateSurfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (uiState.aiSettings.enabled) CyanAiSecondary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onOpenSheet(ActiveSheet.AI_SETTINGS) }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("top_ai_enhance_button")
                    .semantics {
                        contentDescription = "Open On-Device Auto AI Imaging settings. Status: ${if (uiState.aiSettings.enabled) uiState.aiSettings.sceneProfile.badge else "Off"}"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = stringResource(R.string.cd_ai_enhance_settings),
                    tint = if (uiState.aiSettings.enabled) CyanAiSecondary else SilverTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (uiState.aiSettings.enabled) uiState.aiSettings.sceneProfile.badge else "AI OFF",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.aiSettings.enabled) CyanAiSecondary else SilverTextSecondary
                )
            }

            // Presets Button
            IconButton(
                onClick = { onOpenSheet(ActiveSheet.PRESETS) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_presets_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmarks,
                    contentDescription = stringResource(R.string.cd_presets_sheet),
                    tint = IvoryTextPrimary
                )
            }

            // Hardware & Thermal Diagnostics Button
            IconButton(
                onClick = { onOpenSheet(ActiveSheet.HARDWARE_REPORT) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_hardware_inspector_button")
            ) {
                val thermalTint = when (uiState.thermalHealth) {
                    ThermalHealthLevel.NOMINAL, ThermalHealthLevel.LIGHT -> EmeraldOptimal
                    ThermalHealthLevel.MODERATE -> AmberOpticPrimary
                    ThermalHealthLevel.SEVERE, ThermalHealthLevel.CRITICAL -> CinemaCrimsonError
                }
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "${stringResource(R.string.cd_hardware_inspector)}. Lens ${activeLens?.zoomBadge ?: "1x"}, Thermal ${uiState.thermalHealth.label}",
                    tint = thermalTint
                )
            }
        }
    }
}

@Composable
fun BottomCameraControlDeck(
    uiState: CameraUiState,
    onSelectLensIndex: (Int) -> Unit,
    onSetZoomRatio: (Float) -> Unit,
    onSelectMode: (CameraMode) -> Unit,
    onShutterTap: () -> Unit,
    onShutterLongPress: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenSheet: (ActiveSheet) -> Unit,
    onCancelAiJob: () -> Unit,
    onUpdateProIso: (Int?) -> Unit,
    onUpdateProShutterNs: (Long?) -> Unit,
    onUpdateProWb: (WhiteBalancePreset, Int) -> Unit,
    onUpdateProFocus: (Float?) -> Unit,
    onUpdateExposureComp: (Int) -> Unit,
    onToggleAeLock: () -> Unit,
    onToggleAfLock: () -> Unit,
    onUpdateZebraThreshold: (Int) -> Unit,
    onResetProAuto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = GraphiteSurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Non-blocking AI Enhance Background Progress Pill (with Cancel button)
            AnimatedVisibility(visible = uiState.activeAiProgressText != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("ai_background_progress_banner"),
                    color = Color(0xFF002B33),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAiSecondary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CyanAiSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.activeAiProgressText ?: "AI Enhancing…",
                                style = MaterialTheme.typography.bodySmall,
                                color = IvoryTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onCancelAiJob,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cancel_ai_job_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel background OpenAI image enhancement",
                                tint = CyanAiSecondary
                            )
                        }
                    }
                }
            }

            // Video Recording Timecode Banner
            AnimatedVisibility(visible = uiState.isRecordingVideo) {
                val mins = uiState.recordingElapsedSeconds / 60
                val secs = uiState.recordingElapsedSeconds % 60
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CinemaCrimsonError.copy(alpha = 0.22f))
                        .border(1.dp, CinemaCrimsonError, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("recording_timecode_pill")
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CinemaCrimsonError)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "REC %02d:%02d • %s %dfps", mins, secs, uiState.videoResolution.shortBadge, uiState.videoFps.fps),
                        style = MaterialTheme.typography.labelLarge,
                        color = IvoryTextPrimary
                    )
                }
            }

            // Pro Manual Controls Scrubber Deck (Visible in PRO PHOTO & PRO VIDEO)
            AnimatedVisibility(visible = uiState.currentMode.isProMode) {
                ProControlsScrubberPanel(
                    uiState = uiState,
                    onUpdateProIso = onUpdateProIso,
                    onUpdateProShutterNs = onUpdateProShutterNs,
                    onUpdateProWb = onUpdateProWb,
                    onUpdateProFocus = onUpdateProFocus,
                    onUpdateExposureComp = onUpdateExposureComp,
                    onToggleAeLock = onToggleAeLock,
                    onToggleAfLock = onToggleAfLock,
                    onUpdateZebraThreshold = onUpdateZebraThreshold,
                    onResetProAuto = onResetProAuto,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Lens & Optical Zoom Selector Pills
            LensAndZoomPillRow(
                uiState = uiState,
                onSelectLensIndex = onSelectLensIndex,
                onSetZoomRatio = onSetZoomRatio
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Mode Ribbon (One UI Bottom-Weighted Carousel)
            ModeCarouselRibbon(
                currentMode = uiState.currentMode,
                onSelectMode = onSelectMode
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Shutter Row: Gallery Thumbnail | Shutter Ring | Flip Camera | 4-Dot Quick Settings
            PrimaryShutterActionRow(
                uiState = uiState,
                onOpenGallery = { onOpenSheet(ActiveSheet.GALLERY) },
                onShutterTap = onShutterTap,
                onShutterLongPress = onShutterLongPress,
                onFlipCamera = onFlipCamera,
                onOpenQuickSettings = { onOpenSheet(ActiveSheet.QUICK_SETTINGS) }
            )
        }
    }
}

@Composable
private fun LensAndZoomPillRow(
    uiState: CameraUiState,
    onSelectLensIndex: (Int) -> Unit,
    onSetZoomRatio: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SlateSurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Hardware detected lenses
        uiState.detectedLenses.forEachIndexed { index, lens ->
            val isSelected = index == uiState.selectedLensIndex
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) AmberOpticPrimary else Color.Transparent)
                    .clickable { onSelectLensIndex(index) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("lens_pill_$index")
                    .semantics {
                        contentDescription = "Select lens ${lens.displayName} (${lens.zoomBadge})"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lens.zoomBadge,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color(0xFF1A1200) else IvoryTextPrimary
                )
            }
        }

        // Quick digital zoom presets (2x, 5x) within active lens maxDigitalZoom
        val maxZoom = uiState.activeLens?.maxDigitalZoom ?: 8f
        for (presetZoom in listOf(2.0f, 5.0f)) {
            if (presetZoom <= maxZoom) {
                val isZoomSelected = kotlin.math.abs(uiState.zoomRatio - presetZoom) < 0.15f
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .background(if (isZoomSelected) AmberOpticPrimary.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isZoomSelected) AmberOpticPrimary else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onSetZoomRatio(presetZoom) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("zoom_preset_${presetZoom.toInt()}x")
                        .semantics {
                            contentDescription = "Set zoom ratio to ${presetZoom.toInt()}x"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${presetZoom.toInt()}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isZoomSelected) AmberOpticPrimary else SilverTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProControlsScrubberPanel(
    uiState: CameraUiState,
    onUpdateProIso: (Int?) -> Unit,
    onUpdateProShutterNs: (Long?) -> Unit,
    onUpdateProWb: (WhiteBalancePreset, Int) -> Unit,
    onUpdateProFocus: (Float?) -> Unit,
    onUpdateExposureComp: (Int) -> Unit,
    onToggleAeLock: () -> Unit,
    onToggleAfLock: () -> Unit,
    onUpdateZebraThreshold: (Int) -> Unit,
    onResetProAuto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ProParameterTab.ISO) }
    val pro = uiState.proState
    val lens = uiState.activeLens

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pro_controls_panel"),
        color = SlateSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Top Row: Parameter readout pills + AE/AF Lock + Reset Auto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shutterReadout = pro.shutterSpeedNs?.let { ns ->
                    val sec = ns / 1_000_000_000.0
                    if (sec >= 1.0) String.format(Locale.US, "%.1fs", sec)
                    else "1/${(1.0 / sec).toInt().coerceAtLeast(1)}s"
                } ?: "AUTO"

                ProTabChip(
                    title = "ISO",
                    value = pro.iso?.toString() ?: "AUTO",
                    selected = activeTab == ProParameterTab.ISO,
                    enabled = lens?.supportsManualSensor != false,
                    tag = "pro_tab_iso",
                    onClick = { activeTab = ProParameterTab.ISO }
                )
                ProTabChip(
                    title = "SHUTTER",
                    value = shutterReadout,
                    selected = activeTab == ProParameterTab.SHUTTER,
                    enabled = lens?.supportsManualSensor != false,
                    tag = "pro_tab_shutter",
                    onClick = { activeTab = ProParameterTab.SHUTTER }
                )
                ProTabChip(
                    title = "WB",
                    value = if (pro.whiteBalancePreset == WhiteBalancePreset.MANUAL_KELVIN) "${pro.customKelvin}K" else pro.whiteBalancePreset.label,
                    selected = activeTab == ProParameterTab.WB,
                    enabled = true,
                    tag = "pro_tab_wb",
                    onClick = { activeTab = ProParameterTab.WB }
                )
                ProTabChip(
                    title = "FOCUS",
                    value = pro.focusDistanceDiopters?.let { String.format(Locale.US, "%.1fd", it) } ?: "AF-C",
                    selected = activeTab == ProParameterTab.FOCUS,
                    enabled = lens?.supportsVariableFocus != false,
                    tag = "pro_tab_focus",
                    onClick = { activeTab = ProParameterTab.FOCUS }
                )
                ProTabChip(
                    title = "EV",
                    value = String.format(Locale.US, "%+.1f", pro.exposureCompensationIndex * (lens?.exposureCompStepEv ?: 0.33f)),
                    selected = activeTab == ProParameterTab.EV,
                    enabled = true,
                    tag = "pro_tab_ev",
                    onClick = { activeTab = ProParameterTab.EV }
                )
                ProTabChip(
                    title = "ZEBRA",
                    value = "${pro.zebraThresholdPercent}%",
                    selected = activeTab == ProParameterTab.ZEBRA,
                    enabled = true,
                    tag = "pro_tab_zebra",
                    onClick = { activeTab = ProParameterTab.ZEBRA }
                )

                // AE Lock
                IconButton(
                    onClick = onToggleAeLock,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pro_ae_lock_button")
                ) {
                    Icon(
                        imageVector = if (pro.aeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (pro.aeLocked) "Unlock Auto Exposure" else "Lock Auto Exposure",
                        tint = if (pro.aeLocked) AmberOpticPrimary else SilverTextSecondary
                    )
                }

                // Reset All Pro Controls to Auto
                IconButton(
                    onClick = onResetProAuto,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pro_reset_auto_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset all Pro controls to Auto",
                        tint = SilverTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Active Parameter Scrubber
            when (activeTab) {
                ProParameterTab.ISO -> {
                    if (lens?.supportsManualSensor == false) {
                        UnsupportedControlNotice(
                            reason = "Manual ISO unavailable on ${lens.displayName}: CameraCharacteristics lacks MANUAL_SENSOR."
                        )
                    } else {
                        val minIso = (lens?.isoMin ?: 50).toFloat()
                        val maxIso = (lens?.isoMax ?: 6400).toFloat()
                        val currentIso = (pro.iso ?: 200).toFloat().coerceIn(minIso, maxIso)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AutoTogglePill(
                                isAuto = pro.iso == null,
                                onToggle = { onUpdateProIso(if (pro.iso == null) 200 else null) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = currentIso,
                                onValueChange = { onUpdateProIso(it.toInt()) },
                                valueRange = minIso..maxIso,
                                colors = SliderDefaults.colors(
                                    thumbColor = AmberOpticPrimary,
                                    activeTrackColor = AmberOpticPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pro_iso_slider")
                            )
                        }
                    }
                }
                ProParameterTab.SHUTTER -> {
                    if (lens?.supportsManualSensor == false) {
                        UnsupportedControlNotice(
                            reason = "Manual Shutter Speed unavailable on ${lens.displayName}: SENSOR_INFO_EXPOSURE_TIME_RANGE not exposed."
                        )
                    } else {
                        val shutterStops = listOf(
                            125_000L to "1/8000s",
                            500_000L to "1/2000s",
                            1_000_000L to "1/1000s",
                            2_000_000L to "1/500s",
                            4_000_000L to "1/250s",
                            8_000_000L to "1/125s",
                            16_666_666L to "1/60s",
                            33_333_333L to "1/30s",
                            66_666_666L to "1/15s",
                            250_000_000L to "1/4s",
                            1_000_000_000L to "1.0s"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            AutoTogglePill(
                                isAuto = pro.shutterSpeedNs == null,
                                onToggle = { onUpdateProShutterNs(if (pro.shutterSpeedNs == null) 8_000_000L else null) }
                            )
                            shutterStops.forEach { (ns, label) ->
                                val selected = pro.shutterSpeedNs == ns
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) AmberOpticPrimary else GraphiteSurface)
                                        .clickable { onUpdateProShutterNs(ns) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.Black else IvoryTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                ProParameterTab.WB -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        WhiteBalancePreset.entries.forEach { preset ->
                            val isSelected = pro.whiteBalancePreset == preset
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AmberOpticPrimary else GraphiteSurface)
                                    .clickable { onUpdateProWb(preset, preset.approxKelvin) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${preset.label} (${preset.approxKelvin}K)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Color.Black else IvoryTextPrimary
                                )
                            }
                        }
                    }
                }
                ProParameterTab.FOCUS -> {
                    if (lens?.supportsVariableFocus == false) {
                        UnsupportedControlNotice(
                            reason = "Fixed-Focus Lens (${lens.displayName}): LENS_INFO_MINIMUM_FOCUS_DISTANCE is 0.0 diopters."
                        )
                    } else {
                        val maxD = (lens?.minFocusDistanceDiopters ?: 10f).coerceAtLeast(1f)
                        val curD = (pro.focusDistanceDiopters ?: 0f).coerceIn(0f, maxD)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AutoTogglePill(
                                isAuto = pro.focusDistanceDiopters == null,
                                onToggle = { onUpdateProFocus(if (pro.focusDistanceDiopters == null) 1.5f else null) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = curD,
                                onValueChange = { onUpdateProFocus(it) },
                                valueRange = 0f..maxD,
                                colors = SliderDefaults.colors(
                                    thumbColor = AmberOpticPrimary,
                                    activeTrackColor = AmberOpticPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pro_focus_slider")
                            )
                        }
                    }
                }
                ProParameterTab.EV -> {
                    val minEv = (lens?.exposureCompMin ?: -6).toFloat()
                    val maxEv = (lens?.exposureCompMax ?: 6).toFloat()
                    Slider(
                        value = pro.exposureCompensationIndex.toFloat().coerceIn(minEv, maxEv),
                        onValueChange = { onUpdateExposureComp(it.toInt()) },
                        valueRange = minEv..maxEv,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberOpticPrimary,
                            activeTrackColor = AmberOpticPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pro_ev_slider")
                    )
                }
                ProParameterTab.ZEBRA -> {
                    Slider(
                        value = pro.zebraThresholdPercent.toFloat(),
                        onValueChange = { onUpdateZebraThreshold(it.toInt()) },
                        valueRange = 70f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CinemaCrimsonError,
                            activeTrackColor = CinemaCrimsonError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pro_zebra_threshold_slider")
                    )
                }
            }
        }
    }
}

@Composable
private fun ProTabChip(
    title: String,
    value: String,
    selected: Boolean,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AmberOpticPrimary.copy(alpha = 0.22f) else GraphiteSurface)
            .border(
                width = 1.dp,
                color = if (selected) AmberOpticPrimary else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag)
            .semantics { contentDescription = "$title control: $value" }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) SilverTextSecondary else CinemaCrimsonError
        )
        Text(
            text = if (enabled) value else "N/A",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AmberOpticPrimary else IvoryTextPrimary
        )
    }
}

@Composable
private fun AutoTogglePill(
    isAuto: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAuto) AmberOpticPrimary else GraphiteSurface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isAuto) "AUTO" else "MANUAL",
            style = MaterialTheme.typography.labelMedium,
            color = if (isAuto) Color.Black else IvoryTextPrimary
        )
    }
}

@Composable
private fun UnsupportedControlNotice(reason: String) {
    Text(
        text = reason,
        style = MaterialTheme.typography.bodySmall,
        color = ThermalWarningOrange,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ModeCarouselRibbon(
    currentMode: CameraMode,
    onSelectMode: (CameraMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraMode.entries.forEach { mode ->
            val selected = mode == currentMode
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) AmberOpticPrimary else Color.Transparent
                    )
                    .clickable { onSelectMode(mode) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("mode_tab_${mode.name.lowercase()}")
                    .semantics {
                        contentDescription = "${mode.displayName} mode ${if (selected) "selected" else ""}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.shortLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) Color(0xFF1A1200) else IvoryTextPrimary
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PrimaryShutterActionRow(
    uiState: CameraUiState,
    onOpenGallery: () -> Unit,
    onShutterTap: () -> Unit,
    onShutterLongPress: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenQuickSettings: () -> Unit
) {
    val latestMedia = uiState.capturedItems.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Gallery Thumbnail Button with "AI" Badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SlateSurfaceVariant)
                .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenGallery)
                .testTag("gallery_thumbnail_button")
                .semantics {
                    contentDescription = "Open Gallery. ${uiState.capturedItems.size} items captured."
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = stringResource(R.string.cd_open_gallery),
                tint = IvoryTextPrimary
            )
            if (latestMedia != null) {
                val badgeText = if (latestMedia.enhancedUriString != null || latestMedia.aiStatus == AiJobStatus.COMPLETED) {
                    "AI"
                } else {
                    "${uiState.capturedItems.size}"
                }
                val badgeColor = if (badgeText == "AI") CyanAiSecondary else AmberOpticPrimary
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black
                    )
                }
            }
        }

        // 2. Center Tactile Shutter Button (Tap for Photo/Video, Long-Press for Burst)
        val isVideo = uiState.currentMode.isVideoMode
        val shutterDesc = when {
            isVideo && uiState.isRecordingVideo -> stringResource(R.string.cd_shutter_video_stop)
            isVideo -> stringResource(R.string.cd_shutter_video_start)
            else -> stringResource(R.string.cd_shutter_photo)
        }

        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    color = if (isVideo) CinemaCrimsonError else AmberOpticPrimary,
                    shape = CircleShape
                )
                .combinedClickable(
                    onClick = onShutterTap,
                    onLongClick = onShutterLongPress
                )
                .padding(7.dp)
                .testTag("shutter_button")
                .semantics {
                    contentDescription = shutterDesc
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (uiState.isRecordingVideo) 32.dp else 60.dp)
                    .clip(if (uiState.isRecordingVideo) RoundedCornerShape(8.dp) else CircleShape)
                    .background(if (isVideo) CinemaCrimsonError else IvoryTextPrimary)
            )
        }

        // 3. Right Action Pair: Camera Flip + Four-Dot Quick Settings
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onFlipCamera,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(SlateSurfaceVariant)
                    .testTag("flip_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = stringResource(R.string.cd_flip_camera),
                    tint = IvoryTextPrimary
                )
            }

            IconButton(
                onClick = onOpenQuickSettings,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(SlateSurfaceVariant)
                    .testTag("four_dot_quick_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = stringResource(R.string.cd_quick_settings),
                    tint = AmberOpticPrimary
                )
            }
        }
    }
}
