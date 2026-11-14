package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StreetColorScheme = darkColorScheme(
    primary = StreetYellow,
    onPrimary = StreetCharcoal,
    primaryContainer = StreetYellowDark,
    onPrimaryContainer = StreetCharcoal,
    secondary = StreetCyan,
    onSecondary = StreetCharcoal,
    secondaryContainer = StreetSlateSurface,
    onSecondaryContainer = StreetOffWhite,
    tertiary = StreetCoral,
    onTertiary = StreetOffWhite,
    background = StreetCharcoal,
    onBackground = StreetOffWhite,
    surface = StreetSlate,
    onSurface = StreetOffWhite,
    surfaceVariant = StreetSlateSurface,
    onSurfaceVariant = StreetMutedText,
    error = StreetCoral,
    onError = StreetOffWhite
)

@Composable
fun StreetSurferTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StreetColorScheme,
        typography = StreetSurferTypography,
        content = content
    )
}
