// data/preferences/ThemePreferences.kt
package com.goldensystem.auris.data.preferences

import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class CustomThemeConfig(
    val isEnabled: Boolean = false,
    
    // ===== CORES PRINCIPAIS (APENAS 5) =====
    val primaryColor: Int = 0xFF6750A4.toInt(),
    val secondaryColor: Int = 0xFFF06292.toInt(),
    val backgroundColor: Int = 0xFF1E1234.toInt(),
    val onPrimaryColor: Int = 0xFFFFFFFF.toInt(),
    val onSurfaceColor: Int = 0xFFE1BEE7.toInt(),
    
    // ===== COR ÚNICA PARA DETALHES =====
    val accentColor: Int = 0xFFFF8A65.toInt(),  // 👈 UMA COR PARA TUDO!
    
    // ===== WALLPAPER =====
    val wallpaperType: WallpaperType = WallpaperType.SOLID,
    val wallpaperColor: Int = 0xFF1E1234.toInt(),
    val wallpaperUri: String? = null,
    val wallpaperUrl: String? = null,
    val wallpaperBlur: Float = 0.5f,
    val wallpaperDim: Float = 0.3f
)

enum class WallpaperType {
    SOLID,
    GALLERY,
    SERVER
}

@Singleton
class ThemePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val THEME_ENABLED = booleanPreferencesKey("custom_theme_enabled")
        private val PRIMARY_COLOR = intPreferencesKey("custom_primary_color")
        private val SECONDARY_COLOR = intPreferencesKey("custom_secondary_color")
        private val BACKGROUND_COLOR = intPreferencesKey("custom_background_color")
        private val ON_PRIMARY_COLOR = intPreferencesKey("custom_on_primary_color")
        private val ON_SURFACE_COLOR = intPreferencesKey("custom_on_surface_color")
        private val ACCENT_COLOR = intPreferencesKey("custom_accent_color")  // 👈 NOVO
        
        // WALLPAPER
        private val WALLPAPER_TYPE = stringPreferencesKey("wallpaper_type")
        private val WALLPAPER_COLOR = intPreferencesKey("wallpaper_color")
        private val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        private val WALLPAPER_URL = stringPreferencesKey("wallpaper_url")
        private val WALLPAPER_BLUR = floatPreferencesKey("wallpaper_blur")
        private val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
    }

    val customThemeConfig: Flow<CustomThemeConfig> = dataStore.data.map { prefs ->
        CustomThemeConfig(
            isEnabled = prefs[THEME_ENABLED] ?: false,
            primaryColor = prefs[PRIMARY_COLOR] ?: 0xFF6750A4.toInt(),
            secondaryColor = prefs[SECONDARY_COLOR] ?: 0xFFF06292.toInt(),
            backgroundColor = prefs[BACKGROUND_COLOR] ?: 0xFF1E1234.toInt(),
            onPrimaryColor = prefs[ON_PRIMARY_COLOR] ?: 0xFFFFFFFF.toInt(),
            onSurfaceColor = prefs[ON_SURFACE_COLOR] ?: 0xFFE1BEE7.toInt(),
            accentColor = prefs[ACCENT_COLOR] ?: 0xFFFF8A65.toInt(),
            wallpaperType = try {
                val typeName = prefs[WALLPAPER_TYPE] ?: WallpaperType.SOLID.name
                WallpaperType.valueOf(typeName)
            } catch (_: Exception) {
                WallpaperType.SOLID
            },
            wallpaperColor = prefs[WALLPAPER_COLOR] ?: 0xFF1E1234.toInt(),
            wallpaperUri = prefs[WALLPAPER_URI],
            wallpaperUrl = prefs[WALLPAPER_URL],
            wallpaperBlur = prefs[WALLPAPER_BLUR] ?: 0.5f,
            wallpaperDim = prefs[WALLPAPER_DIM] ?: 0.3f
        )
    }

    suspend fun setCustomTheme(config: CustomThemeConfig) {
        dataStore.edit { prefs ->
            prefs[THEME_ENABLED] = config.isEnabled
            prefs[PRIMARY_COLOR] = config.primaryColor
            prefs[SECONDARY_COLOR] = config.secondaryColor
            prefs[BACKGROUND_COLOR] = config.backgroundColor
            prefs[ON_PRIMARY_COLOR] = config.onPrimaryColor
            prefs[ON_SURFACE_COLOR] = config.onSurfaceColor
            prefs[ACCENT_COLOR] = config.accentColor
            prefs[WALLPAPER_TYPE] = config.wallpaperType.name
            prefs[WALLPAPER_COLOR] = config.wallpaperColor
            prefs[WALLPAPER_URI] = config.wallpaperUri ?: ""
            prefs[WALLPAPER_URL] = config.wallpaperUrl ?: ""
            prefs[WALLPAPER_BLUR] = config.wallpaperBlur
            prefs[WALLPAPER_DIM] = config.wallpaperDim
        }
    }

    suspend fun resetCustomTheme() {
        dataStore.edit { prefs ->
            prefs.remove(THEME_ENABLED)
            prefs.remove(PRIMARY_COLOR)
            prefs.remove(SECONDARY_COLOR)
            prefs.remove(BACKGROUND_COLOR)
            prefs.remove(ON_PRIMARY_COLOR)
            prefs.remove(ON_SURFACE_COLOR)
            prefs.remove(ACCENT_COLOR)
            prefs.remove(WALLPAPER_TYPE)
            prefs.remove(WALLPAPER_COLOR)
            prefs.remove(WALLPAPER_URI)
            prefs.remove(WALLPAPER_URL)
            prefs.remove(WALLPAPER_BLUR)
            prefs.remove(WALLPAPER_DIM)
        }
    }
}