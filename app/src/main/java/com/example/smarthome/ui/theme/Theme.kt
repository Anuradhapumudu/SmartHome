package com.example.smarthome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Slate200,
    onPrimary = DarkCharcoal,
    primaryContainer = Slate700,
    onPrimaryContainer = Slate50,
    secondary = SoftBlue,
    onSecondary = DarkCharcoal,
    secondaryContainer = Slate800,
    onSecondaryContainer = SoftBlueDim,
    tertiary = SoftAmber,
    onTertiary = DarkCharcoal,
    background = DarkCharcoal,
    onBackground = Slate50,
    surface = NearBlack,
    onSurface = Slate50,
    surfaceVariant = CardDark,
    onSurfaceVariant = Slate200,
    error = SoftRed,
    onError = Color.White,
    outline = Slate600,
    outlineVariant = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = Slate800,
    onPrimary = White,
    primaryContainer = Slate50,
    onPrimaryContainer = Slate800,
    secondary = Slate600,
    onSecondary = White,
    secondaryContainer = OffWhite,
    onSecondaryContainer = Slate700,
    tertiary = SoftBlue,
    onTertiary = White,
    background = OffWhite,
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
