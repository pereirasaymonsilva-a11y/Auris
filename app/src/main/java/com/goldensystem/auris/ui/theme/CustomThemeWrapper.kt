// ui/theme/CustomThemeWrapper.kt
package com.goldensystem.auris.ui.theme

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.goldensystem.auris.data.preferences.WallpaperType
import com.goldensystem.auris.presentation.viewmodel.CustomThemeViewModel

@Composable
fun CustomThemeWrapper(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val viewModel: CustomThemeViewModel = hiltViewModel()
    val config by viewModel.customThemeConfig.collectAsStateWithLifecycle()

    if (config.isEnabled) {
        val colorScheme = customColorScheme(config, isDark)
        
        // Substitui as cores do MaterialTheme
        CompositionLocalProvider(
            LocalColorScheme provides colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Wallpaper
                when (config.wallpaperType) {
                    WallpaperType.SOLID -> {
                        Box(modifier = Modifier.fillMaxSize().background(Color(config.wallpaperColor)))
                    }
                    WallpaperType.GALLERY -> {
                        config.wallpaperUri?.let { uri ->
                            AsyncImage(
                                model = Uri.parse(uri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    WallpaperType.SERVER -> {
                        config.wallpaperUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Dim
                if (config.wallpaperType != WallpaperType.SOLID) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = config.wallpaperDim))
                    )
                }

                content()
            }
        }
    } else {
        content()
    }
}