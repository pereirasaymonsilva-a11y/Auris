package com.goldensystem.auris.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.goldensystem.auris.presentation.viewmodel.ColorSchemePair
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.unit.dp

val LocalAurisDarkTheme = staticCompositionLocalOf { false }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AurisStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val colorArgb = color.toArgb()
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        @Suppress("DEPRECATION")
        window.statusBarColor = colorArgb
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
    }
}

val DarkColorScheme = darkColorScheme(
    primary = AurisPurplePrimary,
    secondary = AurisPink,
    tertiary = AurisOrange,
    background = AurisPurpleDark,
    surface = AurisSurface,
    onPrimary = AurisWhite,
    onSecondary = AurisWhite,
    onTertiary = AurisWhite,
    onBackground = AurisWhite,
    onSurface = AurisLightPurple, // Texto sobre superficies
    error = Color(0xFFFF5252),
    onError = AurisWhite
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color(0xFF000000),              // Preto sobre elementos primários
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Color(0xFF000000),              // Preto para elementos secundários
    onSecondary = Color(0xFFFFC107),            // Dourado sobre eles
    secondaryContainer = Color(0xFF000000),     // Container secundário preto
    onSecondaryContainer = Color(0xFFFFC107),   // Dourado no container secundário
    tertiary = Color(0xFF4D3E00),               // Tom escuro para detalhes
    onTertiary = Color(0xFFFFC107),             // Dourado sobre detalhes
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    surfaceTint = LightPrimary,
    error = Color(0xFFD32F2F),                 // Vermelho para erros
    onError = Color(0xFFFFFFFF)                 // Texto branco no erro
)

@Composable
fun AurisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val finalColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val statusBarElevation = if (darkTheme) 4.dp else 12.dp
    val elevatedSurface = finalColorScheme.surfaceColorAtElevation(statusBarElevation)
    val defaultStatusBarColor = Color(
        ColorUtils.blendARGB(
            finalColorScheme.background.toArgb(),
            elevatedSurface.toArgb(),
            0.35f
        )
    )

    AurisStatusBarStyle(color = defaultStatusBarColor)

    CompositionLocalProvider(LocalAurisDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
