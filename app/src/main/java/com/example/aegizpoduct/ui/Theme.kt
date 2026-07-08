package com.example.aegizpoduct.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


object AegizPoductTheme {
    val Background = Color(0xFF0B0F10)
    val Panel = Color(0xFF202425)
    val PanelLow = Color(0xFF111516)
    val Outline = Color(0xFF313A3C)
    val OutlineSoft = Color(0xFF1B2426)
    val Text = Color(0xFFE9F2F2)
    val Muted = Color(0xFF8C999B)
    val Cyan = Color(0xFF00CED1)
    val CyanDim = Color(0xFF0B5759)
    val RescueRed = Color(0xFFD32F2F)
    val RescueRedDark = Color(0xFF7E0813)
    val RescueRedSoft = Color(0xFFFFB0AD)
    val Amber = Color(0xFFFFC107)
}


object OpsColors {
    val Background = Color(0xFF0B0F10)
    val Panel = Color(0xFF202425)
    val PanelLow = Color(0xFF111516)
    val Outline = Color(0xFF313A3C)
    val OutlineSoft = Color(0xFF1B2426)
    val Text = Color(0xFFE9F2F2)
    val Muted = Color(0xFF8C999B)
    val Cyan = Color(0xFF00CED1)
    val CyanDim = Color(0xFF0B5759)
    val RescueRed = Color(0xFFD32F2F)
    val RescueRedDark = Color(0xFF7E0813)
    val RescueRedSoft = Color(0xFFFFB0AD)
    val Amber = Color(0xFFFFC107)
}

private val MonitorColorScheme = darkColorScheme(
    primary = OpsColors.Cyan,
    onPrimary = Color(0xFF001010),
    primaryContainer = OpsColors.CyanDim,
    onPrimaryContainer = OpsColors.Cyan,
    secondary = OpsColors.Amber,
    background = OpsColors.Background,
    onBackground = OpsColors.Text,
    surface = OpsColors.Background,
    onSurface = OpsColors.Text,
    surfaceVariant = OpsColors.Panel,
    onSurfaceVariant = OpsColors.Muted,
    outline = OpsColors.Outline,
    outlineVariant = OpsColors.OutlineSoft,
    error = OpsColors.RescueRed,
    onError = Color.White,
    errorContainer = OpsColors.RescueRedDark,
    onErrorContainer = OpsColors.RescueRedSoft,
)

@Composable
fun MonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MonitorColorScheme, content = content)
}


