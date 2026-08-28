package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// The app is permanently light-themed: the system dark mode is deliberately
// ignored so the off-white palette and dark status-bar icons stay consistent.
private val ForcedLightColorScheme = lightColorScheme(
    primary = VibrantPrimary,
    onPrimary = VibrantOnPrimary,
    primaryContainer = VibrantPrimaryContainer,
    onPrimaryContainer = VibrantOnPrimaryContainer,
    secondary = VibrantSecondary,
    onSecondary = VibrantOnSecondary,
    secondaryContainer = VibrantSecondaryContainer,
    onSecondaryContainer = VibrantOnSecondaryContainer,
    tertiary = VibrantTertiary,
    onTertiary = VibrantOnTertiary,
    tertiaryContainer = VibrantTertiaryContainer,
    onTertiaryContainer = VibrantOnTertiaryContainer,
    background = VibrantBackground,
    onBackground = VibrantOnBackground,
    surface = VibrantSurface,
    onSurface = VibrantOnSurface,
    surfaceVariant = VibrantSurfaceVariant,
    onSurfaceVariant = VibrantOnSurfaceVariant,
    outline = VibrantOutline
)

@Composable
fun ClassFlowTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ForcedLightColorScheme,
        typography = Typography,
        content = content
    )
}
