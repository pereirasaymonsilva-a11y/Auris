// ui/theme/CustomThemeWrapper.kt
package com.goldensystem.auris.ui.theme

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.goldensystem.auris.data.preferences.WallpaperType
import com.goldensystem.auris.presentation.viewmodel.CustomThemeViewModel

@Composable
fun CustomThemeWrapper(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val viewModel: CustomThemeViewModel = hiltViewModel()
    val config by viewModel.customThemeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (config.isEnabled) {
        val baseScheme = customColorScheme(config, isDark)
        val colorScheme = baseScheme.copy(
            surfaceContainer = Color(config.containerColor)
        )
        
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (config.wallpaperType) {
                    WallpaperType.SOLID -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(config.backgroundColor))
                        )
                    }
                    
                    WallpaperType.GALLERY -> {
                        config.wallpaperUri?.let { uri ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(Uri.parse(uri))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Wallpaper da galeria",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: run {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(config.backgroundColor))
                            )
                        }
                    }
                    
                    WallpaperType.SERVER -> {
                        config.wallpaperUrl?.let { url ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Wallpaper do servidor",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: run {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(config.backgroundColor))
                            )
                        }
                    }
                }

                if (config.wallpaperType != WallpaperType.SOLID && config.wallpaperDim > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = config.wallpaperDim))
                    )
                }

                if (config.wallpaperType != WallpaperType.SOLID && config.wallpaperBlur > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.White.copy(
                                    alpha = config.wallpaperBlur * 0.3f
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    content()
                }
            }
        }
    } else {
        content()
    }
}