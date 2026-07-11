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
    
    // ===== CORES PRINCIPAIS =====
    val primaryColor: Int = 0xFF6750A4.toInt(),
    val secondaryColor: Int = 0xFFF06292.toInt(),
    val tertiaryColor: Int = 0xFFFF8A65.toInt(),
    val backgroundColor: Int = 0xFF1E1234.toInt(),
    val surfaceColor: Int = 0xFF2A1F40.toInt(),
    val containerColor: Int = 0xFF2A1F40.toInt(),
    
    // ===== CORES ON (TEXTO) =====
    val onPrimaryColor: Int = 0xFFFFFFFF.toInt(),
    val onSecondaryColor: Int = 0xFFFFFFFF.toInt(),
    val onTertiaryColor: Int = 0xFFFFFFFF.toInt(),
    val onBackgroundColor: Int = 0xFFFFFFFF.toInt(),
    val onSurfaceColor: Int = 0xFFE1BEE7.toInt(),
    val onSurfaceVariantColor: Int = 0xFFE1BEE7.toInt(),
    
    // ===== CONTAINERS =====
    val primaryContainerColor: Int = 0xFF6750A4.toInt(),
    val onPrimaryContainerColor: Int = 0xFFFFFFFF.toInt(),
    val secondaryContainerColor: Int = 0xFFF06292.toInt(),
    val onSecondaryContainerColor: Int = 0xFFFFFFFF.toInt(),
    val surfaceVariantColor: Int = 0xFF2A1F40.toInt(),
    val tertiaryContainerColor: Int = 0xFFFF8A65.toInt(),
    val onTertiaryContainerColor: Int = 0xFFFFFFFF.toInt(),
    val errorContainerColor: Int = 0xFFFF5252.toInt(),
    val onErrorContainerColor: Int = 0xFFFFFFFF.toInt(),
    
    // ===== OUTROS =====
    val errorColor: Int = 0xFFFF5252.toInt(),
    val onErrorColor: Int = 0xFFFFFFFF.toInt(),
    val outlineColor: Int = 0xFFB8860B.toInt(),
    val outlineVariantColor: Int = 0xFFB8860B.toInt(),
    val surfaceTintColor: Int = 0xFFFFC107.toInt(),
    val inversePrimaryColor: Int = 0xFF6750A4.toInt(),
    val inverseSurfaceColor: Int = 0xFF2A1F40.toInt(),
    val inverseOnSurfaceColor: Int = 0xFFFFFFFF.toInt(),
    val scrimColor: Int = 0x66000000.toInt(),
    
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
        // CORES PRINCIPAIS
        private val THEME_ENABLED = booleanPreferencesKey("custom_theme_enabled")
        private val PRIMARY_COLOR = intPreferencesKey("custom_primary_color")
        private val SECONDARY_COLOR = intPreferencesKey("custom_secondary_color")
        private val TERTIARY_COLOR = intPreferencesKey("custom_tertiary_color")
        private val BACKGROUND_COLOR = intPreferencesKey("custom_background_color")
        private val SURFACE_COLOR = intPreferencesKey("custom_surface_color")
        private val CONTAINER_COLOR = intPreferencesKey("custom_container_color")
        
        // CORES ON (TEXTO)
        private val ON_PRIMARY_COLOR = intPreferencesKey("custom_on_primary_color")
        private val ON_SECONDARY_COLOR = intPreferencesKey("custom_on_secondary_color")
        private val ON_TERTIARY_COLOR = intPreferencesKey("custom_on_tertiary_color")
        private val ON_BACKGROUND_COLOR = intPreferencesKey("custom_on_background_color")
        private val ON_SURFACE_COLOR = intPreferencesKey("custom_on_surface_color")
        private val ON_SURFACE_VARIANT_COLOR = intPreferencesKey("custom_on_surface_variant_color")
        
        // CONTAINERS
        private val PRIMARY_CONTAINER_COLOR = intPreferencesKey("custom_primary_container_color")
        private val ON_PRIMARY_CONTAINER_COLOR = intPreferencesKey("custom_on_primary_container_color")
        private val SECONDARY_CONTAINER_COLOR = intPreferencesKey("custom_secondary_container_color")
        private val ON_SECONDARY_CONTAINER_COLOR = intPreferencesKey("custom_on_secondary_container_color")
        private val SURFACE_VARIANT_COLOR = intPreferencesKey("custom_surface_variant_color")
        private val TERTIARY_CONTAINER_COLOR = intPreferencesKey("custom_tertiary_container_color")
        private val ON_TERTIARY_CONTAINER_COLOR = intPreferencesKey("custom_on_tertiary_container_color")
        private val ERROR_CONTAINER_COLOR = intPreferencesKey("custom_error_container_color")
        private val ON_ERROR_CONTAINER_COLOR = intPreferencesKey("custom_on_error_container_color")
        
        // OUTROS
        private val ERROR_COLOR = intPreferencesKey("custom_error_color")
        private val ON_ERROR_COLOR = intPreferencesKey("custom_on_error_color")
        private val OUTLINE_COLOR = intPreferencesKey("custom_outline_color")
        private val OUTLINE_VARIANT_COLOR = intPreferencesKey("custom_outline_variant_color")
        private val SURFACE_TINT_COLOR = intPreferencesKey("custom_surface_tint_color")
        private val INVERSE_PRIMARY_COLOR = intPreferencesKey("custom_inverse_primary_color")
        private val INVERSE_SURFACE_COLOR = intPreferencesKey("custom_inverse_surface_color")
        private val INVERSE_ON_SURFACE_COLOR = intPreferencesKey("custom_inverse_on_surface_color")
        private val SCRIM_COLOR = intPreferencesKey("custom_scrim_color")
        
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
            
            // CORES PRINCIPAIS
            primaryColor = prefs[PRIMARY_COLOR] ?: 0xFF6750A4.toInt(),
            secondaryColor = prefs[SECONDARY_COLOR] ?: 0xFFF06292.toInt(),
            tertiaryColor = prefs[TERTIARY_COLOR] ?: 0xFFFF8A65.toInt(),
            backgroundColor = prefs[BACKGROUND_COLOR] ?: 0xFF1E1234.toInt(),
            surfaceColor = prefs[SURFACE_COLOR] ?: 0xFF2A1F40.toInt(),
            containerColor = prefs[CONTAINER_COLOR] ?: 0xFF2A1F40.toInt(),
            
            // CORES ON
            onPrimaryColor = prefs[ON_PRIMARY_COLOR] ?: 0xFFFFFFFF.toInt(),
            onSecondaryColor = prefs[ON_SECONDARY_COLOR] ?: 0xFFFFFFFF.toInt(),
            onTertiaryColor = prefs[ON_TERTIARY_COLOR] ?: 0xFFFFFFFF.toInt(),
            onBackgroundColor = prefs[ON_BACKGROUND_COLOR] ?: 0xFFFFFFFF.toInt(),
            onSurfaceColor = prefs[ON_SURFACE_COLOR] ?: 0xFFE1BEE7.toInt(),
            onSurfaceVariantColor = prefs[ON_SURFACE_VARIANT_COLOR] ?: 0xFFE1BEE7.toInt(),
            
            // CONTAINERS
            primaryContainerColor = prefs[PRIMARY_CONTAINER_COLOR] ?: 0xFF6750A4.toInt(),
            onPrimaryContainerColor = prefs[ON_PRIMARY_CONTAINER_COLOR] ?: 0xFFFFFFFF.toInt(),
            secondaryContainerColor = prefs[SECONDARY_CONTAINER_COLOR] ?: 0xFFF06292.toInt(),
            onSecondaryContainerColor = prefs[ON_SECONDARY_CONTAINER_COLOR] ?: 0xFFFFFFFF.toInt(),
            surfaceVariantColor = prefs[SURFACE_VARIANT_COLOR] ?: 0xFF2A1F40.toInt(),
            tertiaryContainerColor = prefs[TERTIARY_CONTAINER_COLOR] ?: 0xFFFF8A65.toInt(),
            onTertiaryContainerColor = prefs[ON_TERTIARY_CONTAINER_COLOR] ?: 0xFFFFFFFF.toInt(),
            errorContainerColor = prefs[ERROR_CONTAINER_COLOR] ?: 0xFFFF5252.toInt(),
            onErrorContainerColor = prefs[ON_ERROR_CONTAINER_COLOR] ?: 0xFFFFFFFF.toInt(),
            
            // OUTROS
            errorColor = prefs[ERROR_COLOR] ?: 0xFFFF5252.toInt(),
            onErrorColor = prefs[ON_ERROR_COLOR] ?: 0xFFFFFFFF.toInt(),
            outlineColor = prefs[OUTLINE_COLOR] ?: 0xFFB8860B.toInt(),
            outlineVariantColor = prefs[OUTLINE_VARIANT_COLOR] ?: 0xFFB8860B.toInt(),
            surfaceTintColor = prefs[SURFACE_TINT_COLOR] ?: 0xFFFFC107.toInt(),
            inversePrimaryColor = prefs[INVERSE_PRIMARY_COLOR] ?: 0xFF6750A4.toInt(),
            inverseSurfaceColor = prefs[INVERSE_SURFACE_COLOR] ?: 0xFF2A1F40.toInt(),
            inverseOnSurfaceColor = prefs[INVERSE_ON_SURFACE_COLOR] ?: 0xFFFFFFFF.toInt(),
            scrimColor = prefs[SCRIM_COLOR] ?: 0x66000000.toInt(),
            
            // WALLPAPER
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
            // CORES PRINCIPAIS
            prefs[THEME_ENABLED] = config.isEnabled
            prefs[PRIMARY_COLOR] = config.primaryColor
            prefs[SECONDARY_COLOR] = config.secondaryColor
            prefs[TERTIARY_COLOR] = config.tertiaryColor
            prefs[BACKGROUND_COLOR] = config.backgroundColor
            prefs[SURFACE_COLOR] = config.surfaceColor
            prefs[CONTAINER_COLOR] = config.containerColor
            
            // CORES ON
            prefs[ON_PRIMARY_COLOR] = config.onPrimaryColor
            prefs[ON_SECONDARY_COLOR] = config.onSecondaryColor
            prefs[ON_TERTIARY_COLOR] = config.onTertiaryColor
            prefs[ON_BACKGROUND_COLOR] = config.onBackgroundColor
            prefs[ON_SURFACE_COLOR] = config.onSurfaceColor
            prefs[ON_SURFACE_VARIANT_COLOR] = config.onSurfaceVariantColor
            
            // CONTAINERS
            prefs[PRIMARY_CONTAINER_COLOR] = config.primaryContainerColor
            prefs[ON_PRIMARY_CONTAINER_COLOR] = config.onPrimaryContainerColor
            prefs[SECONDARY_CONTAINER_COLOR] = config.secondaryContainerColor
            prefs[ON_SECONDARY_CONTAINER_COLOR] = config.onSecondaryContainerColor
            prefs[SURFACE_VARIANT_COLOR] = config.surfaceVariantColor
            prefs[TERTIARY_CONTAINER_COLOR] = config.tertiaryContainerColor
            prefs[ON_TERTIARY_CONTAINER_COLOR] = config.onTertiaryContainerColor
            prefs[ERROR_CONTAINER_COLOR] = config.errorContainerColor
            prefs[ON_ERROR_CONTAINER_COLOR] = config.onErrorContainerColor
            
            // OUTROS
            prefs[ERROR_COLOR] = config.errorColor
            prefs[ON_ERROR_COLOR] = config.onErrorColor
            prefs[OUTLINE_COLOR] = config.outlineColor
            prefs[OUTLINE_VARIANT_COLOR] = config.outlineVariantColor
            prefs[SURFACE_TINT_COLOR] = config.surfaceTintColor
            prefs[INVERSE_PRIMARY_COLOR] = config.inversePrimaryColor
            prefs[INVERSE_SURFACE_COLOR] = config.inverseSurfaceColor
            prefs[INVERSE_ON_SURFACE_COLOR] = config.inverseOnSurfaceColor
            prefs[SCRIM_COLOR] = config.scrimColor
            
            // WALLPAPER
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
            prefs.remove(TERTIARY_COLOR)
            prefs.remove(BACKGROUND_COLOR)
            prefs.remove(SURFACE_COLOR)
            prefs.remove(CONTAINER_COLOR)
            prefs.remove(ON_PRIMARY_COLOR)
            prefs.remove(ON_SECONDARY_COLOR)
            prefs.remove(ON_TERTIARY_COLOR)
            prefs.remove(ON_BACKGROUND_COLOR)
            prefs.remove(ON_SURFACE_COLOR)
            prefs.remove(ON_SURFACE_VARIANT_COLOR)
            prefs.remove(PRIMARY_CONTAINER_COLOR)
            prefs.remove(ON_PRIMARY_CONTAINER_COLOR)
            prefs.remove(SECONDARY_CONTAINER_COLOR)
            prefs.remove(ON_SECONDARY_CONTAINER_COLOR)
            prefs.remove(SURFACE_VARIANT_COLOR)
            prefs.remove(TERTIARY_CONTAINER_COLOR)
            prefs.remove(ON_TERTIARY_CONTAINER_COLOR)
            prefs.remove(ERROR_CONTAINER_COLOR)
            prefs.remove(ON_ERROR_CONTAINER_COLOR)
            prefs.remove(ERROR_COLOR)
            prefs.remove(ON_ERROR_COLOR)
            prefs.remove(OUTLINE_COLOR)
            prefs.remove(OUTLINE_VARIANT_COLOR)
            prefs.remove(SURFACE_TINT_COLOR)
            prefs.remove(INVERSE_PRIMARY_COLOR)
            prefs.remove(INVERSE_SURFACE_COLOR)
            prefs.remove(INVERSE_ON_SURFACE_COLOR)
            prefs.remove(SCRIM_COLOR)
            prefs.remove(WALLPAPER_TYPE)
            prefs.remove(WALLPAPER_COLOR)
            prefs.remove(WALLPAPER_URI)
            prefs.remove(WALLPAPER_URL)
            prefs.remove(WALLPAPER_BLUR)
            prefs.remove(WALLPAPER_DIM)
        }
    }
}