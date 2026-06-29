// presentation/viewmodel/CustomThemeViewModel.kt
package com.goldensystem.auris.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldensystem.auris.data.preferences.CustomThemeConfig
import com.goldensystem.auris.data.preferences.ThemePreferences
import com.goldensystem.auris.data.preferences.WallpaperType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
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

    fun updateContainerColor(color: Int) {
    _config.update { it.copy(containerColor = color) }
    }

    fun updateSecondaryColor(color: Int) {
        _config.update { it.copy(secondaryColor = color) }
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

    suspend fun saveCustomTheme() {
        themePreferences.setCustomTheme(_config.value.copy(isEnabled = true))
    }

    suspend fun resetToDefault() {
        themePreferences.resetCustomTheme()
        // Recarregar o config do SharedPreferences
        _config.value = CustomThemeConfig() // Isso já tem o backgroundColor padrão: 0xFF1E1234
        // E também recarregamos do SharedPreferences para garantir consistência
        themePreferences.customThemeConfig.collect { config ->
            _config.value = config
        }
    }
}