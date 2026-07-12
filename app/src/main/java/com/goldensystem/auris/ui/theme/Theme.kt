// ui/theme/Theme.kt
package com.goldensystem.auris.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.goldensystem.auris.data.preferences.WallpaperType
import com.goldensystem.auris.presentation.viewmodel.ColorSchemePair
import com.goldensystem.auris.presentation.viewmodel.CustomThemeViewModel
import java.io.File

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

    // ... status bar ...

    CompositionLocalProvider(LocalAurisDarkTheme provides darkTheme) {
        val viewModel: CustomThemeViewModel = remember { hiltViewModel() }
        val config by viewModel.customThemeConfig.collectAsStateWithLifecycle()

        LaunchedEffect(config.wallpaperUri) {
            if (config.wallpaperType == WallpaperType.GALLERY && config.wallpaperUri != null) {
                val file = File(config.wallpaperUri!!)
                if (!file.exists()) {
                    viewModel.resetWallpaper()
                    Log.d("Theme", "Wallpaper não encontrado, resetando para cor sólida")
                }
            }
        }

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
                            val uri = config.wallpaperUri
                            if (uri != null) {
                                val file = File(uri)
                                if (file.exists()) {
                                    // 🔥 BLUR NO BOX QUE ENVOLVE O ASYNCIMAGE
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(radius = (config.wallpaperBlur * 18f).dp)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(file)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Wallpaper da galeria",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color(config.backgroundColor))
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(config.backgroundColor))
                                )
                            }
                        }
                        WallpaperType.SERVER -> {
                            config.wallpaperUrl?.let {
                                // 🔥 BLUR NO BOX QUE ENVOLVE O ASYNCIMAGE
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(radius = (config.wallpaperBlur * 18f).dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(it)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Wallpaper do servidor",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } ?: run {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(config.backgroundColor))
                                )
                            }
                        }
                    }

                    // DIM (escurecimento) - isso está funcionando!
                    if (config.wallpaperType != WallpaperType.SOLID && config.wallpaperDim > 0f) {
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