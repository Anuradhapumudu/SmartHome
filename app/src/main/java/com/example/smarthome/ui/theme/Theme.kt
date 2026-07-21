package com.example.smarthome.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.Black,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealPrimaryLight,
    // Secondary = teal variant so NavigationBar indicator stays on-brand
    secondary = TealPrimaryLight,
    onSecondary = Color.Black,
    secondaryContainer = TealContainer,
    onSecondaryContainer = TealPrimary,
    // Tertiary = amber accent (for iron warnings, etc.)
    tertiary = AmberAccent,
    onTertiary = Color.Black,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberAccent,
    background = BackgroundDark,
    onBackground = OnSurface,
    surface = SurfaceDark,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariant,
    error = StatusError,
    onError = Color.White,
    outline = TealPrimaryDark,
)

@Composable
fun SmartHomeTheme(
    content: @Composable () -> Unit
) {
    // Force dark mode — smart home dashboards always look best dark
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SmartHomeTypography,
        content = content
    )
}
