package com.example.miles.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

val NeonCyan = Color(0xFF00B0FF)
val CoralFlame = Color(0xFFFF5722)
val VividGreen = Color(0xFF00E676)
val ElectricAmber = Color(0xFFFFD600)
val DarkSurface = Color(0xFF121212)
val CardSurface = Color(0xFF1C1C1E)
val OLEDBlack = Color(0xFF000000)
val MutedGray = Color(0xFF90A4AE)

private val WearDarkColorScheme = ColorScheme(
    primary = NeonCyan,
    primaryContainer = Color(0xFF00364F),
    onPrimary = Color.Black,
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = VividGreen,
    secondaryContainer = Color(0xFF00391A),
    onSecondary = Color.Black,
    onSecondaryContainer = Color(0xFF6BFF9C),
    tertiary = CoralFlame,
    tertiaryContainer = Color(0xFF521500),
    onTertiary = Color.White,
    onTertiaryContainer = Color(0xFFFFDBD0),
    surfaceContainer = CardSurface,
    onSurface = Color.White,
    onSurfaceVariant = MutedGray,
    background = OLEDBlack,
    onBackground = Color.White,
    error = Color(0xFFFF5252),
    onError = Color.Black
)

@Composable
fun MilesWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearDarkColorScheme,
        typography = Typography(),
        content = content
    )
}
