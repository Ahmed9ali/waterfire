package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WaterFoxDarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = BrandOrangeLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
    error = DangerRed,
    onError = Color.White
)

val WaterFoxLightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBE5),
    onPrimaryContainer = BrandOrangeDark,
    secondary = BrandOrangeDark,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightDivider,
    error = DangerRed,
    onError = Color.White
)

val WaterFoxPrivateColorScheme = darkColorScheme(
    primary = PrivateAccent,
    onPrimary = Color.White,
    primaryContainer = PrivateAccentDark,
    onPrimaryContainer = Color.White,
    secondary = BrandOrange,
    onSecondary = Color.White,
    background = PrivateBackground,
    onBackground = DarkTextPrimary,
    surface = PrivateSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = PrivateSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF4A3B5C),
    error = DangerRed,
    onError = Color.White
)

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

@Composable
fun WaterFoxTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    isPrivateTab: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        isPrivateTab -> WaterFoxPrivateColorScheme
        isDark -> WaterFoxDarkColorScheme
        else -> WaterFoxLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
