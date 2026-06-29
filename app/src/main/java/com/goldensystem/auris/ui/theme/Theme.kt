// ui/theme/Theme.kt
package com.goldensystem.auris.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
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
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.goldensystem.auris.data.preferences.WallpaperType
import com.goldensystem.auris.presentation.viewmodel.CustomThemeViewModel

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
    onSurface = AurisLightPurple,
    error = Color(0xFFFF5252),
    onError = AurisWhite
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightPrimary,
    onSecondary = Color(0xFF000000),
    secondaryContainer = LightPrimary.copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF4D3E00),
    onTertiary = Color(0xFFFFC107),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    surfaceTint = LightPrimary,
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
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
        val viewModel: CustomThemeViewModel = hiltViewModel()
        val config by viewModel.customThemeConfig.collectAsStateWithLifecycle()

        if (config.isEnabled) {
            val customColorScheme = customColorScheme(config, darkTheme)

            MaterialTheme(
                colorScheme = customColorScheme,
                typography = Typography,
                shapes = Shapes
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (config.wallpaperType) {
                        WallpaperType.SOLID -> {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color(config.backgroundColor))
                            )
                        }
                        WallpaperType.GALLERY -> {
                            config.wallpaperUri?.let {
                                AsyncImage(
                                    model = Uri.parse(it),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        WallpaperType.SERVER -> {
                            config.wallpaperUrl?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    if (config.wallpaperType != WallpaperType.SOLID) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = config.wallpaperDim))
                        )
                    }

                    content()
                }
            }
        } else {
            MaterialTheme(
                colorScheme = finalColorScheme,
                typography = Typography,
                shapes = Shapes
            ) {
                content()
            }
        }
    }
}
