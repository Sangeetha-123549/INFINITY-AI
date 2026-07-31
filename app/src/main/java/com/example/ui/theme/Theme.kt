package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ElegantDarkColorScheme = darkColorScheme(
    primary = ElegantDarkPrimary,
    onPrimary = ElegantDarkOnPrimary,
    primaryContainer = ElegantDarkPrimaryContainer,
    onPrimaryContainer = ElegantDarkOnPrimaryContainer,
    secondary = ElegantDarkSecondary,
    onSecondary = ElegantDarkOnSecondary,
    secondaryContainer = ElegantDarkSecondaryContainer,
    onSecondaryContainer = ElegantDarkOnSecondaryContainer,
    background = ElegantDarkBackground,
    onBackground = ElegantDarkOnBackground,
    surface = ElegantDarkSurface,
    onSurface = ElegantDarkOnSurface,
    surfaceVariant = ElegantDarkSurfaceContainerLow,
    onSurfaceVariant = ElegantDarkOnSurfaceVariant,
    outline = ElegantDarkOutline,
    outlineVariant = ElegantDarkOutlineVariant,
    surfaceContainerLowest = ElegantDarkSurfaceContainerLowest,
    surfaceContainerLow = ElegantDarkSurfaceContainerLow,
    surfaceContainer = ElegantDarkSurfaceContainer,
    surfaceContainerHigh = ElegantDarkSurfaceContainerHigh,
    surfaceContainerHighest = ElegantDarkSurfaceContainerHighest,
)

@Composable
fun InfinityAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ElegantDarkColorScheme,
        typography = Typography,
        content = content
    )
}

