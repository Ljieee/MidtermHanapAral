package com.example.examhanaparal.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary            = AccentBlue,
    onPrimary          = Color.White,
    primaryContainer   = AccentBluePale,
    onPrimaryContainer = AccentBlue,
    secondary          = AccentGreen,
    onSecondary        = Color.White,
    secondaryContainer = AccentGreenDim,
    tertiary           = AdminGold,
    background         = DarkBackground,
    onBackground       = TextPrimary,
    surface            = DarkSurface,
    onSurface          = TextPrimary,
    surfaceVariant     = DarkCard,
    onSurfaceVariant   = TextSecondary,
    outline            = DividerColor,
    error              = DangerRed,
    onError            = Color.White,
    errorContainer     = DangerRedDim
)

@Composable
fun HanapAralTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}