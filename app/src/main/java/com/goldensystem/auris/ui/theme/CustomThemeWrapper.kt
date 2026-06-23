// ui/theme/CustomThemeWrapper.kt
package com.goldensystem.auris.ui.theme

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.goldensystem.auris.data.preferences.CustomThemeConfig
import com.goldensystem.auris.data.preferences.WallpaperType
import com.goldensystem.auris.presentation.viewmodel.CustomThemeViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CustomThemeWrapper(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val viewModel: CustomThemeViewModel = hiltViewModel()
    val config by viewModel.customThemeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (config.isEnabled) {
        val colorScheme = customColorScheme(config, isDark)
        
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Wallpaper Background
                when (config.wallpaperType) {
                    WallpaperType.SOLID -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(config.wallpaperColor))
                        )
                    }
                    
                    WallpaperType.GALLERY -> {
                        config.wallpaperUri?.let { uri ->
                            AsyncImage(
                                model = Uri.parse(uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        // Aplicar blur e dim
                                        // (implementar com RenderEffect se disponível)
                                    },
                                contentScale = ContentScale.Crop
                            )
                        } ?: Box(modifier = Modifier.fillMaxSize().background(Color(config.backgroundColor)))
                    }
                    
                    WallpaperType.SERVER -> {
                        config.wallpaperUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        // Aplicar blur e dim
                                    },
                                contentScale = ContentScale.Crop
                            )
                        } ?: Box(modifier = Modifier.fillMaxSize().background(Color(config.backgroundColor)))
                    }
                }

                // Dim overlay
                if (config.wallpaperType != WallpaperType.SOLID) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = config.wallpaperDim))
                    )
                }

                // Conteúdo
                content()
            }
        }
    } else {
        // Tema padrão
        content()
    }
}