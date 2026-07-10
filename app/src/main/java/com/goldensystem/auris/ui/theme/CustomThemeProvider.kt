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
    return if (isDark) {
        darkColorScheme(
            primary = Color(config.primaryColor),
            onPrimary = Color(config.onPrimaryColor),
            secondary = Color(config.secondaryColor),
            onSecondary = Color(config.onSecondaryColor),
            tertiary = Color(config.tertiaryColor),
            onTertiary = Color(config.onTertiaryColor),
            background = Color(config.backgroundColor),
            onBackground = Color(config.onBackgroundColor),
            surface = Color(config.surfaceColor),
            onSurface = Color(config.onSurfaceColor),
            surfaceVariant = Color(config.surfaceVariantColor),
            onSurfaceVariant = Color(config.onSurfaceVariantColor),
            primaryContainer = Color(config.primaryContainerColor),
            onPrimaryContainer = Color(config.onPrimaryContainerColor),
            secondaryContainer = Color(config.secondaryContainerColor),
            onSecondaryContainer = Color(config.onSecondaryContainerColor),
            error = Color(config.errorColor),
            onError = Color(config.onErrorColor),
            outline = Color(config.outlineColor),
            outlineVariant = Color(config.outlineVariantColor),
            surfaceTint = Color(config.surfaceTintColor)
        )
    } else {
        lightColorScheme(
            primary = Color(config.primaryColor),
            onPrimary = Color(config.onPrimaryColor),
            secondary = Color(config.secondaryColor),
            onSecondary = Color(config.onSecondaryColor),
            tertiary = Color(config.tertiaryColor),
            onTertiary = Color(config.onTertiaryColor),
            background = Color(config.backgroundColor),
            onBackground = Color(config.onBackgroundColor),
            surface = Color(config.surfaceColor),
            onSurface = Color(config.onSurfaceColor),
            surfaceVariant = Color(config.surfaceVariantColor),
            onSurfaceVariant = Color(config.onSurfaceVariantColor),
            primaryContainer = Color(config.primaryContainerColor),
            onPrimaryContainer = Color(config.onPrimaryContainerColor),
            secondaryContainer = Color(config.secondaryContainerColor),
            onSecondaryContainer = Color(config.onSecondaryContainerColor),
            error = Color(config.errorColor),
            onError = Color(config.onErrorColor),
            outline = Color(config.outlineColor),
            outlineVariant = Color(config.outlineVariantColor),
            surfaceTint = Color(config.surfaceTintColor)
        )
    }
}