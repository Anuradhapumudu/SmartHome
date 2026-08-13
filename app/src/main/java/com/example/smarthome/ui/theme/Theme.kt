package com.example.smarthome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Slate200,
    onPrimary = Slate900,
    primaryContainer = Slate700,
    onPrimaryContainer = White,
    secondary = SoftBlue,
    onSecondary = Slate900,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = White,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    error = SoftRed,
    onError = White,
    outline = Slate600,
    outlineVariant = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = Slate800,
    onPrimary = White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate800,
    secondary = Slate600,
    onSecondary = White,
    background = SlateBgLight,
    onBackground = Slate800,
    surface = White,
    onSurface = Slate800,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate600,
    error = SoftRed,
    onError = White,
    outline = Slate200,
    outlineVariant = Slate100
)

@Composable
fun SmartHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartHomeTypography,
        content = content
    )
}
