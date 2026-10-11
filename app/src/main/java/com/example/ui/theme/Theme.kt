package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Fallback palettes (phones older than Android 12) from Material Theme Builder.
private val M3LightScheme = lightColorScheme(
    primary = M3Light.primary,
    onPrimary = M3Light.onPrimary,
    primaryContainer = M3Light.primaryContainer,
    onPrimaryContainer = M3Light.onPrimaryContainer,
    secondary = M3Light.secondary,
    onSecondary = M3Light.onSecondary,
    secondaryContainer = M3Light.secondaryContainer,
    onSecondaryContainer = M3Light.onSecondaryContainer,
    tertiary = M3Light.tertiary,
    onTertiary = M3Light.onTertiary,
    tertiaryContainer = M3Light.tertiaryContainer,
    onTertiaryContainer = M3Light.onTertiaryContainer,
    error = M3Light.error,
    onError = M3Light.onError,
    errorContainer = M3Light.errorContainer,
    onErrorContainer = M3Light.onErrorContainer,
    background = M3Light.background,
    onBackground = M3Light.onBackground,
    surface = M3Light.surface,
    onSurface = M3Light.onSurface,
    surfaceVariant = M3Light.surfaceVariant,
    onSurfaceVariant = M3Light.onSurfaceVariant,
    outline = M3Light.outline,
    outlineVariant = M3Light.outlineVariant,
    scrim = M3Light.scrim,
    inverseSurface = M3Light.inverseSurface,
    inverseOnSurface = M3Light.inverseOnSurface,
    inversePrimary = M3Light.inversePrimary,
    surfaceDim = M3Light.surfaceDim,
    surfaceBright = M3Light.surfaceBright,
    surfaceContainerLowest = M3Light.surfaceContainerLowest,
    surfaceContainerLow = M3Light.surfaceContainerLow,
    surfaceContainer = M3Light.surfaceContainer,
    surfaceContainerHigh = M3Light.surfaceContainerHigh,
    surfaceContainerHighest = M3Light.surfaceContainerHighest
)

private val M3DarkScheme = darkColorScheme(
    primary = M3Dark.primary,
    onPrimary = M3Dark.onPrimary,
    primaryContainer = M3Dark.primaryContainer,
    onPrimaryContainer = M3Dark.onPrimaryContainer,
    secondary = M3Dark.secondary,
    onSecondary = M3Dark.onSecondary,
    secondaryContainer = M3Dark.secondaryContainer,
    onSecondaryContainer = M3Dark.onSecondaryContainer,
    tertiary = M3Dark.tertiary,
    onTertiary = M3Dark.onTertiary,
    tertiaryContainer = M3Dark.tertiaryContainer,
    onTertiaryContainer = M3Dark.onTertiaryContainer,
    error = M3Dark.error,
    onError = M3Dark.onError,
    errorContainer = M3Dark.errorContainer,
    onErrorContainer = M3Dark.onErrorContainer,
    background = M3Dark.background,
    onBackground = M3Dark.onBackground,
    surface = M3Dark.surface,
    onSurface = M3Dark.onSurface,
    surfaceVariant = M3Dark.surfaceVariant,
    onSurfaceVariant = M3Dark.onSurfaceVariant,
    outline = M3Dark.outline,
    outlineVariant = M3Dark.outlineVariant,
    scrim = M3Dark.scrim,
    inverseSurface = M3Dark.inverseSurface,
    inverseOnSurface = M3Dark.inverseOnSurface,
    inversePrimary = M3Dark.inversePrimary,
    surfaceDim = M3Dark.surfaceDim,
    surfaceBright = M3Dark.surfaceBright,
    surfaceContainerLowest = M3Dark.surfaceContainerLowest,
    surfaceContainerLow = M3Dark.surfaceContainerLow,
    surfaceContainer = M3Dark.surfaceContainer,
    surfaceContainerHigh = M3Dark.surfaceContainerHigh,
    surfaceContainerHighest = M3Dark.surfaceContainerHighest
)

// Private tabs keep their own purple look.
val WaterFoxPrivateColorScheme = darkColorScheme(
    primary = PrivateAccent,
    onPrimary = Color.White,
    primaryContainer = PrivateAccentDark,
    onPrimaryContainer = Color.White,
    secondary = PrivateAccent,
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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    isPrivateTab: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        isPrivateTab -> WaterFoxPrivateColorScheme
        // Dynamic color (Material You) on Android 12+
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        isDark -> M3DarkScheme
        else -> M3LightScheme
    }

    // After upgrading material3 (see the gradle note), you can switch this to
    // MaterialExpressiveTheme(colorScheme = colorScheme, typography = Typography, content = content)
    // with @OptIn(ExperimentalMaterial3ExpressiveApi::class) on this function.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
