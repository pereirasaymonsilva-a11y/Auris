// presentation/viewmodel/CustomThemeViewModel.kt
package com.goldensystem.auris.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldensystem.auris.data.preferences.CustomThemeConfig
import com.goldensystem.auris.data.preferences.ThemePreferences
import com.goldensystem.auris.data.preferences.WallpaperType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class CustomThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _config = MutableStateFlow(CustomThemeConfig())
    val customThemeConfig: StateFlow<CustomThemeConfig> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            themePreferences.customThemeConfig.collect { config ->
                _config.value = config
            }
        }
    }

    fun updatePrimaryColor(color: Int) {
        _config.update { it.copy(primaryColor = color) }
    }

    fun updateSecondaryColor(color: Int) {
        _config.update { it.copy(secondaryColor = color) }
    }

    fun updateContainerColor(color: Int) {
        _config.update { it.copy(containerColor = color) }
    }

    fun updateBackgroundColor(color: Int) {
        _config.update { it.copy(backgroundColor = color) }
    }

    fun updateSurfaceColor(color: Int) {
        _config.update { it.copy(surfaceColor = color) }
    }

    fun setWallpaperType(type: WallpaperType) {
        _config.update { it.copy(wallpaperType = type) }
    }

    fun setWallpaperColor(color: Int) {
        _config.update { it.copy(wallpaperColor = color) }
    }

    fun setWallpaperFromGallery(uri: String) {
        _config.update { 
            it.copy(
                wallpaperType = WallpaperType.GALLERY,
                wallpaperUri = uri
            )
        }
    }

    fun setWallpaperFromServer(url: String) {
        _config.update { 
            it.copy(
                wallpaperType = WallpaperType.SERVER,
                wallpaperUrl = url
            )
        }
    }

    fun setWallpaperBlur(blur: Float) {
        _config.update { it.copy(wallpaperBlur = blur) }
    }

    fun setWallpaperDim(dim: Float) {
        _config.update { it.copy(wallpaperDim = dim) }
    }
    
    suspend fun disableCustomTheme() {
        themePreferences.setCustomTheme(_config.value.copy(isEnabled = false))
        _config.value = _config.value.copy(isEnabled = false)
    }

    suspend fun saveCustomTheme() {
        themePreferences.setCustomTheme(_config.value.copy(isEnabled = true))
    }

    suspend fun resetToDefault() {
        themePreferences.resetCustomTheme()
        _config.value = CustomThemeConfig()
        themePreferences.customThemeConfig.collect { config ->
            _config.value = config
        }
    }

    // 👇 FUNÇÃO CORRIGIDA DENTRO DA CLASSE
    fun saveWallpaperFromGallery(uriString: String) {
        viewModelScope.launch {
            try {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                
                if (inputStream != null) {
                    val cacheDir = File(context.cacheDir, "wallpapers")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    
                    val outputFile = File(cacheDir, "wallpaper_${System.currentTimeMillis()}.jpg")
                    
                    FileOutputStream(outputFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    
                    _config.update { currentConfig ->
                        currentConfig.copy(
                            wallpaperUri = outputFile.absolutePath,
                            wallpaperType = WallpaperType.GALLERY
                        )
                    }
                    
                    // Salva no preferences
                    themePreferences.setCustomTheme(_config.value)
                    
                    Log.d("CustomTheme", "Wallpaper salvo em: ${outputFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e("CustomTheme", "Erro ao salvar wallpaper: ${e.message}")
            }
        }
    }

    // 👇 FUNÇÃO PARA RESETAR O WALLPAPER (quando o arquivo for deletado)
    fun resetWallpaper() {
        _config.update { currentConfig ->
            currentConfig.copy(
                wallpaperUri = null,
                wallpaperType = WallpaperType.SOLID
            )
        }
        viewModelScope.launch {
            themePreferences.setCustomTheme(_config.value)
        }
    }
}