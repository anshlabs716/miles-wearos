package com.example.miles.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.miles.wear.MilesWearApplication
import com.example.miles.wear.data.model.ThemeAccent

val NeonCyan = Color(0xFF00B0FF)
val CoralFlame = Color(0xFFFF5722)
val VividGreen = Color(0xFF00E676)
val ElectricAmber = Color(0xFFFFD600)
val NeonPurple = Color(0xFFE040FB)
val DarkSurface = Color(0xFF121212)
val CardSurface = Color(0xFF1C1C1E)
val OLEDBlack = Color(0xFF000000)
val MutedGray = Color(0xFF90A4AE)

@Composable
fun MilesWearTheme(
    content: @Composable () -> Unit
) {
    val repository = MilesWearApplication.instance.repository
    val settings by repository.settings.collectAsStateWithLifecycle()

    val accentColor = when (settings.themeAccent) {
        ThemeAccent.CYAN -> NeonCyan
        ThemeAccent.CORAL -> CoralFlame
        ThemeAccent.GREEN -> VividGreen
        ThemeAccent.AMBER -> ElectricAmber
        ThemeAccent.PURPLE -> NeonPurple
    }

    val dynamicColorScheme = ColorScheme(
        primary = accentColor,
        primaryContainer = accentColor.copy(alpha = 0.25f),
        onPrimary = Color.Black,
        onPrimaryContainer = Color.White,
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

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography(),
        content = content
    )
}
