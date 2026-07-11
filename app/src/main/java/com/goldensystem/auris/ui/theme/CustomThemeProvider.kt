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
    val onPrimary = Color(config.onPrimaryColor)
    val secondary = Color(config.secondaryColor)
    val onSecondary = Color(config.onSecondaryColor)
    val tertiary = Color(config.tertiaryColor)
    val onTertiary = Color(config.onTertiaryColor)
    val background = Color(config.backgroundColor)
    val onBackground = Color(config.onBackgroundColor)
    val surface = Color(config.surfaceColor)
    val onSurface = Color(config.onSurfaceColor)
    val onSurfaceVariant = Color(config.onSurfaceVariantColor)
    
    // ===== CORES COM TRANSPARÊNCIA (VOLTARAM COMO ANTES) =====
    val primaryContainer = primary.copy(alpha = 0.2f)           // 👈 VOLTOU!
    val onPrimaryContainer = Color(config.onPrimaryContainerColor)
    val secondaryContainer = secondary.copy(alpha = 0.2f)       // 👈 VOLTOU!
    val onSecondaryContainer = Color(config.onSecondaryContainerColor)
    val tertiaryContainer = tertiary.copy(alpha = 0.2f)         // 👈 VOLTOU!
    val onTertiaryContainer = Color(config.onTertiaryContainerColor)
    val surfaceVariant = surface.copy(alpha = 0.8f)             // 👈 VOLTOU!
    val errorContainer = Color(config.errorColor).copy(alpha = 0.2f) // 👈 VOLTOU!
    val onErrorContainer = Color(config.onErrorContainerColor)
    
    val error = Color(config.errorColor)
    val onError = Color(config.onErrorColor)
    val outline = Color(config.outlineColor)
    val outlineVariant = Color(config.outlineVariantColor)
    val surfaceTint = Color(config.surfaceTintColor)
    val inversePrimary = Color(config.inversePrimaryColor)
    val inverseSurface = Color(config.inverseSurfaceColor)
    val inverseOnSurface = Color(config.inverseOnSurfaceColor)
    val scrim = Color(config.scrimColor)

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = tertiary,
            onTertiary = onTertiary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,        // 👈 COM TRANSPARÊNCIA
            onSurfaceVariant = onSurfaceVariant,
            primaryContainer = primaryContainer,    // 👈 COM TRANSPARÊNCIA
            onPrimaryContainer = onPrimaryContainer,
            secondaryContainer = secondaryContainer, // 👈 COM TRANSPARÊNCIA
            onSecondaryContainer = onSecondaryContainer,
            tertiaryContainer = tertiaryContainer,  // 👈 COM TRANSPARÊNCIA
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,        // 👈 COM TRANSPARÊNCIA
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            surfaceTint = surfaceTint,
            inversePrimary = inversePrimary,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            scrim = scrim
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            tertiary = tertiary,
            onTertiary = onTertiary,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,        // 👈 COM TRANSPARÊNCIA
            onSurfaceVariant = onSurfaceVariant,
            primaryContainer = primaryContainer,    // 👈 COM TRANSPARÊNCIA
            onPrimaryContainer = onPrimaryContainer,
            secondaryContainer = secondaryContainer, // 👈 COM TRANSPARÊNCIA
            onSecondaryContainer = onSecondaryContainer,
            tertiaryContainer = tertiaryContainer,  // 👈 COM TRANSPARÊNCIA
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            onError = onError,
            errorContainer = errorContainer,        // 👈 COM TRANSPARÊNCIA
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            surfaceTint = surfaceTint,
            inversePrimary = inversePrimary,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            scrim = scrim
        )
    }
}