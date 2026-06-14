package com.aigenerator.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary          = Color(0xFF9C89FF),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF2D1B69),
    secondary        = Color(0xFF03DAC5),
    background       = Color(0xFF1A1A2E),
    surface          = Color(0xFF16213E),
    surfaceVariant   = Color(0xFF252542),
    onSurface        = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFB0B0CC),
    error            = Color(0xFFFF6B6B),
    errorContainer   = Color(0xFF4D1F1F),
    onErrorContainer = Color(0xFFFFB3B3)
)
private val Light = lightColorScheme(
    primary          = Color(0xFF6750A4),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    secondary        = Color(0xFF018786),
    background       = Color(0xFFF8F5FF),
    surface          = Color.White,
    surfaceVariant   = Color(0xFFF2EEFF),
    onSurface        = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun AIGeneratorTheme(dark: Boolean = isSystemInDarkTheme(),
                     content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
