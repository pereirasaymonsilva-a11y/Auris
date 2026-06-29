// ui/theme/CustomThemeWrapper.kt
package com.goldensystem.auris.ui.theme

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.io.File

@Composable
fun CustomThemeWrapper(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val viewModel: CustomThemeViewModel = hiltViewModel()
    val config by viewModel.customThemeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 👇 VERIFICA SE O ARQUIVO DE WALLPAPER EXISTE (GALLERY)
    LaunchedEffect(config.wallpaperUri) {
        if (config.wallpaperType == WallpaperType.GALLERY && config.wallpaperUri != null) {
            val file = File(config.wallpaperUri!!)
            if (!file.exists()) {
                viewModel.resetWallpaper()
                Log.d("CustomThemeWrapper", "Wallpaper não encontrado, resetando para cor sólida")
            }
        }
    }

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
                        val uri = config.wallpaperUri
                        if (uri != null) {
                            val file = File(uri)
                            if (file.exists()) {
                                // Carrega do arquivo local
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(file)  // 👈 Passa o File diretamente
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Wallpaper da galeria",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Fallback se o arquivo não existir
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(config.backgroundColor))
                                )
                            }
                        } else {
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
        AurisTheme(darkTheme = isDark) {
            content()
        }
    }
}