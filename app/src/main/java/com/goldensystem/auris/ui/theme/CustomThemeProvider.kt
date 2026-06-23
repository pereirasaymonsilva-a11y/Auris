// ui/theme/CustomThemeProvider.kt
package com.goldensystem.auris.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.goldensystem.auris.data.preferences.CustomThemeConfig

fun customColorScheme(
    config: CustomThemeConfig,
    isDark: Boolean
): ColorScheme {
    val primary = Color(config.primaryColor)
    val secondary = Color(config.secondaryColor)
    val background = Color(config.backgroundColor)
    val surface = Color(config.surfaceColor)

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = secondary,
            onSecondary = Color.White,
            background = background,
            surface = surface,
            onSurface = Color.White.copy(alpha = 0.87f),
            onBackground = Color.White.copy(alpha = 0.87f),
            surfaceVariant = surface.copy(alpha = 0.8f),
            onSurfaceVariant = Color.White.copy(alpha = 0.6f),
            primaryContainer = primary.copy(alpha = 0.2f),
            onPrimaryContainer = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.2f),
            onSecondaryContainer = Color.White,
            tertiary = Color(0xFFFF8A65),
            onTertiary = Color.White,
            error = Color(0xFFFF5252),
            onError = Color.White,
            outline = Color.White.copy(alpha = 0.12f),
            outlineVariant = Color.White.copy(alpha = 0.08f)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = secondary,
            onSecondary = Color.White,
            background = background,
            surface = surface,
            onSurface = Color.Black.copy(alpha = 0.87f),
            onBackground = Color.Black.copy(alpha = 0.87f),
            surfaceVariant = surface.copy(alpha = 0.8f),
            onSurfaceVariant = Color.Black.copy(alpha = 0.6f),
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = Color.Black.copy(alpha = 0.87f),
            secondaryContainer = secondary.copy(alpha = 0.15f),
            onSecondaryContainer = Color.Black.copy(alpha = 0.87f),
            tertiary = Color(0xFFFF8A65),
            onTertiary = Color.White,
            error = Color(0xFFD32F2F),
            onError = Color.White,
            outline = Color.Black.copy(alpha = 0.12f),
            outlineVariant = Color.Black.copy(alpha = 0.08f)
        )
    }
}