package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AspectRatioOption
import com.example.model.CameraPreset
import com.example.model.GridOption
import com.example.model.VideoFpsOption
import com.example.model.VideoResolutionOption
import com.example.ui.theme.AmberOpticPrimary
import com.example.ui.theme.CinemaCrimsonError
import com.example.ui.theme.CyanAiSecondary
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.IvoryTextPrimary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverTextSecondary
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.ThermalWarningOrange
import com.example.viewmodel.ActiveSheet
import com.example.viewmodel.CameraUiState

@Composable
fun FourDotQuickSettingsSheet(
    uiState: CameraUiState,
    onSetAspectRatio: (AspectRatioOption) -> Unit,
    onSetPhotoResolutionIndex: (Int) -> Unit,
    onSetVideoResolution: (VideoResolutionOption) -> Unit,
    onSetVideoFps: (VideoFpsOption) -> Unit,
    onToggleHdr: (Boolean) -> Unit,
    onToggleStabilization: (Boolean) -> Unit,
    onSetGridOption: (GridOption) -> Unit,
    onToggleHorizonLevel: (Boolean) -> Unit,
    onToggleAudio: (Boolean) -> Unit,
    onSaveCustomFolder: (String) -> Unit,
    onNavigateSheet: (ActiveSheet) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val lens = uiState.activeLens
    var folderDraft by remember(uiState.customStorageFolder) {
        mutableStateOf(uiState.customStorageFolder)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("four_dot_quick_settings_sheet"),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Camera Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = IvoryTextPrimary
                    )
                    Text(
                        text = "Active Optic: ${lens?.displayName ?: "Primary Sensor"} • ${lens?.hardwareLevelDescription ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberOpticPrimary
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("quick_settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Quick Settings",
                        tint = IvoryTextPrimary
                    )
                }
            }

            // Aspect Ratio & Photo Resolution
            Surface(color = GraphiteSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aspect Ratio & Photo Output", style = MaterialTheme.typography.titleMedium, color = IvoryTextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AspectRatioOption.entries.forEach { ratio ->
                            val selected = uiState.aspectRatio == ratio
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) AmberOpticPrimary else SlateSurfaceVariant)
                                    .clickable { onSetAspectRatio(ratio) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ratio.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.Black else IvoryTextPrimary
                                )
                            }
                        }
                    }

                    val photoSizes = lens?.supportedPhotoSizes.orEmpty()
                    if (photoSizes.isNotEmpty()) {
                        Text("Hardware JPEG Output Sizes (${lens?.zoomBadge}):", style = MaterialTheme.typography.labelSmall, color = SilverTextSecondary)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            photoSizes.take(5).forEachIndexed { idx, resItem ->
                                val selected = uiState.selectedPhotoResolutionIndex == idx
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) AmberOpticPrimary.copy(alpha = 0.25f) else SlateSurfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) AmberOpticPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSetPhotoResolutionIndex(idx) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = resItem.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) AmberOpticPrimary else IvoryTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Video Resolution & FPS (Hardware-Gated)
            Surface(color = GraphiteSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Video Resolution & Frame Rate", style = MaterialTheme.typography.titleMedium, color = IvoryTextPrimary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        VideoResolutionOption.entries.forEach { res ->
                            val supported = lens?.supportedVideoResolutions?.contains(res) != false
                            val selected = uiState.videoResolution == res
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            selected -> AmberOpticPrimary
                                            supported -> SlateSurfaceVariant
                                            else -> Color(0xFF1A1518)
                                        }
                                    )
                                    .clickable { onSetVideoResolution(res) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("video_res_${res.shortBadge.lowercase()}")
                            ) {
                                Text(
                                    text = if (supported) res.label else "${res.shortBadge} (Unsupported)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        selected -> Color.Black
                                        supported -> IvoryTextPrimary
                                        else -> CinemaCrimsonError
                                    }
                                )
                            }
                        }
                    }

                    val allowedFps = lens?.supportedVideoFpsByResolution?.get(uiState.videoResolution)
                        ?: setOf(VideoFpsOption.FPS_30)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoFpsOption.entries.forEach { fps ->
                            val supported = allowedFps.contains(fps)
                            val selected = uiState.videoFps == fps
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            selected -> AmberOpticPrimary
                                            supported -> SlateSurfaceVariant
                                            else -> Color(0xFF1A1518)
                                        }
                                    )
                                    .clickable { onSetVideoFps(fps) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("video_fps_${fps.fps}")
                            ) {
                                Text(
                                    text = if (supported) fps.label else "${fps.label} (N/A)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        selected -> Color.Black
                                        supported -> IvoryTextPrimary
                                        else -> CinemaCrimsonError
                                    }
                                )
                            }
                        }
                    }

                    if (!allowedFps.contains(VideoFpsOption.FPS_60)) {
                        Text(
                            text = "Note: 60 FPS at ${uiState.videoResolution.shortBadge} is disabled because sensor minFrameDuration exceeds 16.6ms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThermalWarningOrange
                        )
                    }
                }
            }

            // Hardware Stabilization, HDR, Grid, Level & Audio
            Surface(color = GraphiteSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val hdrSupported = lens?.supportsHdr10Video == true || lens?.supportsHdrSceneMode == true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HDR Capture / 10-Bit Profile", style = MaterialTheme.typography.titleSmall, color = IvoryTextPrimary)
                            Text(
                                text = if (hdrSupported) "Supported by active lens" else "Disabled: No HDR10 / HDR scene profile exposed on this lens",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hdrSupported) SilverTextSecondary else ThermalWarningOrange
                            )
                        }
                        Switch(
                            checked = uiState.hdrEnabled,
                            onCheckedChange = onToggleHdr
                        )
                    }

                    val stabSupported = lens?.supportsOpticalStabilization == true || lens?.supportsVideoStabilization == true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Optical / Electronic Stabilization (OIS + EIS)", style = MaterialTheme.typography.titleSmall, color = IvoryTextPrimary)
                            Text(
                                text = if (stabSupported)
                                    "OIS: ${if (lens?.supportsOpticalStabilization == true) "Yes" else "No"} • EIS: ${if (lens?.supportsVideoStabilization == true) "Yes" else "No"}"
                                else
                                    "Disabled: Neither OIS nor EIS exposed by CameraCharacteristics",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (stabSupported) SilverTextSecondary else ThermalWarningOrange
                            )
                        }
                        Switch(
                            checked = uiState.stabilizationEnabled,
                            onCheckedChange = onToggleStabilization
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dual-Axis Horizon Level", style = MaterialTheme.typography.titleSmall, color = IvoryTextPrimary)
                        Switch(
                            checked = uiState.horizonLevelEnabled,
                            onCheckedChange = onToggleHorizonLevel
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Video Stereo Microphone Audio", style = MaterialTheme.typography.titleSmall, color = IvoryTextPrimary)
                        Switch(
                            checked = uiState.audioRecordingEnabled,
                            onCheckedChange = onToggleAudio
                        )
                    }

                    Text("Composition Grid Overlay", style = MaterialTheme.typography.labelMedium, color = SilverTextSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        GridOption.entries.forEach { grid ->
                            val selected = uiState.gridOption == grid
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) AmberOpticPrimary else SlateSurfaceVariant)
                                    .clickable { onSetGridOption(grid) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = grid.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) Color.Black else IvoryTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Custom Storage Folder
            Surface(color = GraphiteSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Custom MediaStore Folder", style = MaterialTheme.typography.titleMedium, color = IvoryTextPrimary)
                    Text(
                        text = "Photos save to Pictures/$folderDraft with full EXIF metadata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverTextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = folderDraft,
                            onValueChange = { folderDraft = it },
                            label = { Text("Subfolder Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onSaveCustomFolder(folderDraft) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberOpticPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }

            // Navigation Shortcuts to AI Settings, Presets & Hardware Checklist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateSheet(ActiveSheet.AI_SETTINGS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("AI Studio", color = CyanAiSecondary)
                }
                OutlinedButton(
                    onClick = { onNavigateSheet(ActiveSheet.PRESETS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Presets", color = AmberOpticPrimary)
                }
                OutlinedButton(
                    onClick = { onNavigateSheet(ActiveSheet.HARDWARE_REPORT) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("HONOR 600 Info", color = EmeraldOptimal)
                }
            }
        }
    }
}

@Composable
fun PresetsManagerSheet(
    uiState: CameraUiState,
    onSavePreset: (String) -> Unit,
    onLoadPreset: (CameraPreset) -> Unit,
    onRenamePreset: (String, String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    var newPresetName by remember { mutableStateOf("") }
    var editingPresetId by remember { mutableStateOf<String?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("presets_manager_sheet"),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Shooting Presets", style = MaterialTheme.typography.headlineMedium, color = IvoryTextPrimary)
                    Text(
                        text = "Validated against active lens (${uiState.activeLens?.displayName ?: "Primary"}) on load",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverTextSecondary
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("presets_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Presets", tint = IvoryTextPrimary)
                }
            }

            // Save Current Configuration Card
            Surface(color = GraphiteSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save Current Camera Configuration", style = MaterialTheme.typography.titleMedium, color = IvoryTextPrimary)
                    Text(
                        text = "Captures Mode (${uiState.currentMode.shortLabel}), Aspect (${uiState.aspectRatio.label}), Video (${uiState.videoResolution.shortBadge}/${uiState.videoFps.fps}), Pro ISO/Shutter/WB, and Overlays.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverTextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPresetName,
                            onValueChange = { newPresetName = it },
                            placeholder = { Text("e.g., Astro Tripod 8s") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preset_name_input")
                        )
                        Button(
                            onClick = {
                                onSavePreset(newPresetName)
                                newPresetName = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberOpticPrimary,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.testTag("preset_save_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }

            // Saved Presets List
            uiState.presets.forEach { preset ->
                Surface(
                    color = GraphiteSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (editingPresetId == preset.id) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = renameDraft,
                                    onValueChange = { renameDraft = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        onRenamePreset(preset.id, renameDraft)
                                        editingPresetId = null
                                    }
                                ) {
                                    Text("Save Name")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AmberOpticPrimary
                                    )
                                    Text(
                                        text = "${preset.mode.shortLabel} • ${preset.aspectRatio.label} • ${preset.videoResolution.shortBadge}@${preset.videoFps.fps} • ISO ${preset.proIso ?: "Auto"} • WB ${preset.proWbPreset.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SilverTextSecondary
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingPresetId = preset.id
                                            renameDraft = preset.name
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename ${preset.name}", tint = SilverTextSecondary)
                                    }
                                    IconButton(
                                        onClick = { onDeletePreset(preset.id) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete ${preset.name}", tint = CinemaCrimsonError)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { onLoadPreset(preset) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateSurfaceVariant,
                                contentColor = IvoryTextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_preset_${preset.id}")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldOptimal)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Validate & Load Preset")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareAndChecklistSheet(
    uiState: CameraUiState,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val checklistItems = remember {
        listOf(
            "M1: Grant Camera & Audio permissions and verify 4K UHD (3840×2160) photo capture saved to MediaStore (Pictures/Lumina600).",
            "M2: Swipe horizontally on viewfinder to switch modes (Night/Portrait/Photo/Video/Pro) and swipe vertically to flip front/rear lens.",
            "M3: Switch to Video mode and tap the top 4K • 60FPS / 4K • 30FPS badge (or 4-Dot Quick Settings) to switch between 4K 60fps and 4K 30fps.",
            "M4: Verify On-Device Auto AI Imaging ('AI AUTO' badge, zero API keys) automatically saves both 4K Original and 4K Auto AI (_AI.jpg) versions with Gallery toggle.",
            "M5: Switch to Pro Photo/Video, verify docked top-corner RGB/Luma Histogram HUD and toggle single active overlay (Zebra / Focus Peaking / False Color IRE).",
            "M6: Save & load a custom 4K preset and check thermal status indicator in the top bar."
        )
    }
    val checkedStates = remember { mutableStateListOf(false, false, false, false, false, false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("hardware_report_sheet"),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HONOR 600 Diagnostics & Checklist",
                        style = MaterialTheme.typography.headlineMedium,
                        color = IvoryTextPrimary
                    )
                    Text(
                        text = "Thermal: ${uiState.thermalHealth.label} (${uiState.batteryTempCelsius}°C) • ${uiState.detectedLenses.size} Camera(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldOptimal
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("hardware_report_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Hardware Diagnostics", tint = IvoryTextPrimary)
                }
            }

            // Runtime CameraCharacteristics Per Lens
            Surface(color = GraphiteSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Runtime Detected CameraCharacteristics",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberOpticPrimary
                    )
                    uiState.detectedLenses.forEach { lens ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurfaceVariant)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${lens.displayName} [${lens.zoomBadge}] — ${lens.hardwareLevelDescription}",
                                style = MaterialTheme.typography.titleSmall,
                                color = IvoryTextPrimary
                            )
                            Text(
                                text = "Focal: ${lens.focalLengthsMm.joinToString()}mm • Aperture: f/${lens.apertures.joinToString()} • 4K: ${lens.supports4K} • 60fps: ${lens.supports60Fps} • OIS: ${lens.supportsOpticalStabilization} • EIS: ${lens.supportsVideoStabilization}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverTextSecondary
                            )
                            Text(
                                text = "Manual Sensor: ${lens.supportsManualSensor} (ISO ${lens.isoMin ?: "N/A"}..${lens.isoMax ?: "N/A"}) • RAW: ${lens.supportsRawCapture} • MinFocus: ${lens.minFocusDistanceDiopters}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAiSecondary
                            )
                            if (lens.unsupportedFeatures.isNotEmpty()) {
                                lens.unsupportedFeatures.forEach { unsup ->
                                    Text(
                                        text = "✗ ${unsup.featureName}: ${unsup.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThermalWarningOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Documented HONOR 600 OEM Hardware / HAL Limitations
            Surface(color = GraphiteSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CinemaCrimsonError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Features the HONOR 600 Cannot Support (Camera2 HAL)",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryTextPrimary
                        )
                    }
                    uiState.honor600Limitations.forEach { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = item.featureName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = AmberOpticPrimary
                            )
                            Text(
                                text = item.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = IvoryTextPrimary
                            )
                            Text(
                                text = "Inspected Key: ${item.characteristicKey}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverTextSecondary
                            )
                        }
                    }
                }
            }

            // Interactive On-Device Manual Test Checklist
            Surface(color = GraphiteSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "On-Device Manual Verification Checklist (M1–M6)",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldOptimal
                    )
                    checklistItems.forEachIndexed { idx, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { checkedStates[idx] = !checkedStates[idx] }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = checkedStates[idx],
                                onCheckedChange = { checkedStates[idx] = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = IvoryTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
