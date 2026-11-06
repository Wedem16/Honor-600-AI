package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuminaDarkColorScheme = darkColorScheme(
    primary = AmberOpticPrimary,
    onPrimary = Color(0xFF1A1200),
    primaryContainer = AmberOpticDim,
    onPrimaryContainer = AmberOpticPrimary,
    secondary = CyanAiSecondary,
    onSecondary = Color(0xFF001F24),
    secondaryContainer = CyanAiDim,
    onSecondaryContainer = CyanAiSecondary,
    tertiary = EmeraldOptimal,
    onTertiary = Color(0xFF002107),
    background = ObsidianBlack,
    onBackground = IvoryTextPrimary,
    surface = GraphiteSurface,
    onSurface = IvoryTextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SilverTextSecondary,
    error = CinemaCrimsonError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuminaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
