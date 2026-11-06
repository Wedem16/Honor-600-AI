package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AiEnhanceSettings
import com.example.model.AiJobStatus
import com.example.model.AiSceneProfile
import com.example.ui.theme.AmberOpticPrimary
import com.example.ui.theme.CinemaCrimsonError
import com.example.ui.theme.CyanAiSecondary
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.IvoryTextPrimary
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverTextSecondary
import com.example.ui.theme.SlateSurfaceVariant
import com.example.viewmodel.CameraUiState

@Composable
fun AiEnhanceSettingsSheet(
    uiState: CameraUiState,
    onUpdateAiSettings: (AiEnhanceSettings) -> Unit,
    onResetAiSettings: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val settings = uiState.aiSettings

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_settings_sheet"),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanAiSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "HONOR Auto AI Imaging",
                            style = MaterialTheme.typography.headlineMedium,
                            color = IvoryTextPrimary
                        )
                        Text(
                            text = "100% On-Device ISP Engine • Zero API Keys Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldOptimal
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ai_settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Auto AI Settings",
                        tint = IvoryTextPrimary
                    )
                }
            }

            // Master Enable & Auto Mode
            Surface(
                color = GraphiteSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto AI Camera Photography",
                                style = MaterialTheme.typography.titleMedium,
                                color = IvoryTextPrimary
                            )
                            Text(
                                text = "Automatically recognizes scenes (Sky, Portrait, Night, HDR) and enhances 4K photos locally on your phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SilverTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = { onUpdateAiSettings(settings.copy(enabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAiSecondary),
                            modifier = Modifier.testTag("ai_enable_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Apply After Every 4K Shot",
                                style = MaterialTheme.typography.titleMedium,
                                color = IvoryTextPrimary
                            )
                            Text(
                                text = "Saves both the untouched 4K Original and the AI-enhanced 4K image instantly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanAiSecondary
                            )
                        }
                        Switch(
                            checked = settings.autoEnhanceAfterCapture,
                            onCheckedChange = {
                                onUpdateAiSettings(
                                    settings.copy(
                                        enabled = if (it) true else settings.enabled,
                                        autoEnhanceAfterCapture = it
                                    )
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAiSecondary),
                            modifier = Modifier.testTag("ai_auto_switch")
                        )
                    }
                }
            }

            // On-Device Scene Recognition Profiles
            Surface(
                color = GraphiteSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Imaging Scene Profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryTextPrimary
                        )
                        OutlinedButton(
                            onClick = onResetAiSettings,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ai_prompt_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Auto AI Imaging settings",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Default")
                        }
                    }

                    AiSceneProfile.entries.forEach { profile ->
                        val isSelected = settings.sceneProfile == profile
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) CyanAiSecondary.copy(alpha = 0.16f)
                                    else SlateSurfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) CyanAiSecondary else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onUpdateAiSettings(settings.copy(sceneProfile = profile)) }
                                .padding(12.dp)
                                .testTag("ai_profile_${profile.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) CyanAiSecondary else IvoryTextPrimary
                                )
                                Text(
                                    text = profile.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberOpticPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = SilverTextSecondary
                            )
                        }
                    }
                }
            }

            // AI Strength, Smart HDR Tone Mapping & 4K Detail Sharpening
            Surface(
                color = GraphiteSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AI Tuning Intensity",
                            style = MaterialTheme.typography.titleSmall,
                            color = IvoryTextPrimary
                        )
                        Text(
                            text = "${settings.intensityPercent}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanAiSecondary
                        )
                    }
                    Slider(
                        value = settings.intensityPercent.toFloat(),
                        onValueChange = { onUpdateAiSettings(settings.copy(intensityPercent = it.toInt())) },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAiSecondary,
                            activeTrackColor = CyanAiSecondary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart HDR Shadow & Highlight Recovery",
                                style = MaterialTheme.typography.titleSmall,
                                color = IvoryTextPrimary
                            )
                            Text(
                                text = "Lifts dark shadow regions while protecting bright sky highlights.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SilverTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.smartHdrToneMap,
                            onCheckedChange = { onUpdateAiSettings(settings.copy(smartHdrToneMap = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "4K Optical Micro-Contrast Sharpening",
                                style = MaterialTheme.typography.titleSmall,
                                color = IvoryTextPrimary
                            )
                            Text(
                                text = "Enhances fine edge definition on 3840×2160 4K captures.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SilverTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.detailSharpening,
                            onCheckedChange = { onUpdateAiSettings(settings.copy(detailSharpening = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Optimal Zoom AI Super-Resolution (2x–10x)",
                                style = MaterialTheme.typography.titleSmall,
                                color = IvoryTextPrimary
                            )
                            Text(
                                text = "Applies adaptive multi-scale edge reconstruction and chroma de-ringing at telephoto zoom stops.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanAiSecondary
                            )
                        }
                        Switch(
                            checked = settings.aiZoomSuperRes,
                            onCheckedChange = { onUpdateAiSettings(settings.copy(aiZoomSuperRes = it)) },
                            modifier = Modifier.testTag("ai_zoom_super_res_switch")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DualVersionGallerySheet(
    uiState: CameraUiState,
    onSelectMediaItem: (String) -> Unit,
    onToggleOriginalVsAi: (String, Boolean) -> Unit,
    onTriggerManualAiEnhance: (String) -> Unit,
    onCancelAiJob: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val selectedItem = uiState.selectedGalleryItem

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dual_gallery_sheet"),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "4K Original, RAW & Auto AI Gallery",
                        style = MaterialTheme.typography.headlineMedium,
                        color = IvoryTextPrimary
                    )
                    Text(
                        text = "Folder: Pictures/${uiState.customStorageFolder} • ${uiState.capturedItems.size} captures",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverTextSecondary
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("gallery_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Gallery",
                        tint = IvoryTextPrimary
                    )
                }
            }

            if (selectedItem == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.img_optics_studio_banner_1793975946933),
                            contentDescription = "No captures yet",
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No 4K captures saved yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryTextPrimary
                        )
                        Text(
                            text = "Tap the shutter to capture a 4K photo with Auto AI Imaging and 16-bit RAW DNG.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverTextSecondary
                        )
                    }
                }
            } else {
                val hasAiCopy = selectedItem.enhancedUriString != null
                val showingAi = hasAiCopy && selectedItem.showingEnhancedInGallery
                val activeUriString = if (showingAi) selectedItem.enhancedUriString else selectedItem.originalUriString

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(GraphiteSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!selectedItem.isVideo && !activeUriString.isNullOrBlank()) {
                        AsyncImage(
                            model = activeUriString,
                            contentDescription = if (showingAi) "Auto AI Enhanced 4K photo" else "Original 4K photo",
                            contentScale = ContentScale.Fit,
                            error = painterResource(id = R.drawable.img_optics_studio_banner_1793975946933),
                            placeholder = painterResource(id = R.drawable.img_optics_studio_banner_1793975946933),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_optics_studio_banner_1793975946933),
                            contentDescription = "Captured 4K media preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Top-Left Version, AI & RAW DNG Badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (showingAi) "VIEWING: 4K AUTO AI (_AI.jpg)" else "VIEWING: 4K ORIGINAL",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (showingAi) CyanAiSecondary else AmberOpticPrimary
                            )
                        }

                        if (hasAiCopy) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyanAiSecondary)
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("gallery_ai_badge")
                            ) {
                                Text(
                                    text = "AI",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }

                        if (!selectedItem.rawDngFileName.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberOpticPrimary)
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("gallery_raw_dng_badge")
                            ) {
                                Text(
                                    text = "RAW DNG",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Quick Original / AI Segmented Toggle
                    if (hasAiCopy) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.82f))
                                .border(1.dp, CyanAiSecondary, RoundedCornerShape(50))
                                .padding(4.dp)
                                .testTag("gallery_original_ai_toggle")
                        ) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(50))
                                    .background(if (!showingAi) AmberOpticPrimary else Color.Transparent)
                                    .clickable { onToggleOriginalVsAi(selectedItem.id, false) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ORIGINAL 4K",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (!showingAi) Color.Black else IvoryTextPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(50))
                                    .background(if (showingAi) CyanAiSecondary else Color.Transparent)
                                    .clickable { onToggleOriginalVsAi(selectedItem.id, true) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AUTO AI 4K",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (showingAi) Color.Black else IvoryTextPrimary
                                )
                            }
                        }
                    }
                }

                // Metadata & Instant On-Device Re-Enhance Action Card
                Surface(
                    color = GraphiteSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${selectedItem.displayName} • ${selectedItem.width}×${selectedItem.height}",
                            style = MaterialTheme.typography.titleSmall,
                            color = IvoryTextPrimary
                        )
                        Text(
                            text = "${selectedItem.lensLabel} • ${selectedItem.modeLabel} • ${selectedItem.exifSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SilverTextSecondary
                        )
                        if (!selectedItem.rawDngFileName.isNullOrBlank()) {
                            Text(
                                text = "16-Bit RAW File: ${selectedItem.rawDngFileName} (Saved to Pictures/${uiState.customStorageFolder})",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberOpticPrimary
                            )
                        }
                        if (!selectedItem.aiSceneSummary.isNullOrBlank()) {
                            Text(
                                text = "On-Device ISP: ${selectedItem.aiSceneSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanAiSecondary
                            )
                        }
                        if (!selectedItem.aiErrorMessage.isNullOrBlank()) {
                            Text(
                                text = selectedItem.aiErrorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = CinemaCrimsonError
                            )
                        }

                        if (!selectedItem.isVideo) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isProcessingThis = uiState.activeAiJobMediaId == selectedItem.id
                                if (isProcessingThis) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanAiSecondary
                                    )
                                    Text(
                                        text = uiState.activeAiProgressText ?: "Auto AI Tuning…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyanAiSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(onClick = onCancelAiJob) {
                                        Text("Cancel", color = CinemaCrimsonError)
                                    }
                                } else {
                                    Button(
                                        onClick = { onTriggerManualAiEnhance(selectedItem.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyanAiSecondary,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("gallery_manual_enhance_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (hasAiCopy) "Re-Run Auto AI" else "Apply Auto AI")
                                    }
                                    OutlinedButton(onClick = onOpenAiSettings) {
                                        Text("AI Scene")
                                    }
                                }
                            }
                        }
                    }
                }

                // Filmstrip Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.capturedItems.forEach { item ->
                        val isSelected = item.id == selectedItem.id
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AmberOpticPrimary.copy(alpha = 0.22f) else SlateSurfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AmberOpticPrimary else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectMediaItem(item.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.displayName.takeLast(14),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IvoryTextPrimary
                                )
                                if (item.enhancedUriString != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CyanAiSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
