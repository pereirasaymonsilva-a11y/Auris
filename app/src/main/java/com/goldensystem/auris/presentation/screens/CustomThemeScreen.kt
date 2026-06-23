// presentation/screens/CustomThemeScreen.kt
package com.goldensystem.auris.presentation.screens

import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.goldensystem.auris.R
import com.goldensystem.auris.data.preferences.CustomThemeConfig
import com.goldensystem.auris.data.preferences.WallpaperType
import com.goldensystem.auris.presentation.navigation.Screen
import com.goldensystem.auris.presentation.viewmodel.CustomThemeViewModel
import com.goldensystem.auris.ui.theme.customColorScheme
import kotlinx.coroutines.launch

// Links para wallpapers do servidor (substitua depois)
val SERVER_WALLPAPERS = listOf(
    "https://example.com/wallpaper1.jpg",
    "https://example.com/wallpaper2.jpg",
    "https://example.com/wallpaper3.jpg",
    "https://example.com/wallpaper4.jpg",
    "https://example.com/wallpaper5.jpg"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeScreen(
    navController: NavController,
    viewModel: CustomThemeViewModel = hiltViewModel()
) {
    val config by viewModel.customThemeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Cores, 1: Wallpaper

    // Preview do tema atual
    val previewColorScheme = remember(config) {
        customColorScheme(config, true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.custom_theme_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.auth_cd_back))
                    }
                },
                actions = {
                    // Reset
                    IconButton(onClick = { viewModel.resetToDefault() }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.cd_reset))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = previewColorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(previewColorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview do Player (mini)
            CustomThemePreviewCard(config = config)

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = previewColorScheme.surface.copy(alpha = 0.8f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.custom_theme_colors)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.custom_theme_wallpaper)) }
                )
            }

            when (selectedTab) {
                0 -> ColorPickerSection(config = config, viewModel = viewModel)
                1 -> WallpaperSection(config = config, viewModel = viewModel)
            }

            // Botão Salvar
            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveCustomTheme()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = previewColorScheme.primary,
                    contentColor = previewColorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_apply), fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun CustomThemePreviewCard(config: CustomThemeConfig) {
    val colorScheme = remember(config) { customColorScheme(config, true) }
    
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primary.copy(alpha = 0.15f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.3f),
                            colorScheme.surface
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Album Art Mock
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primary)
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "Música Exemplo",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Artista Exemplo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Botões de controle mock
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Icons.Rounded.SkipPrevious,
                            Icons.Rounded.PlayArrow,
                            Icons.Rounded.SkipNext
                        ).forEach { icon ->
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.fillMaxSize().padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Cores usadas (mini amostras)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    colorScheme.primary,
                    colorScheme.secondary,
                    colorScheme.surface,
                    colorScheme.background
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPickerSection(
    config: CustomThemeConfig,
    viewModel: CustomThemeViewModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.custom_theme_colors_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Cores pré-definidas
        val presetColors = listOf(
            0xFF6750A4.toInt(), 0xFFF06292, 0xFFFF8A65, 0xFF4CAF50,
            0xFF2196F3, 0xFFFFC107, 0xFF9C27B0, 0xFF3F51B5,
            0xFFE91E63, 0xFF00BCD4, 0xFFFF5722, 0xFF8BC34A
        )

        // Cor Primária
        ColorPickerRow(
            label = stringResource(R.string.custom_theme_primary_color),
            currentColor = config.primaryColor,
            presetColors = presetColors,
            onColorSelected = { viewModel.updatePrimaryColor(it) }
        )

        // Cor Secundária
        ColorPickerRow(
            label = stringResource(R.string.custom_theme_secondary_color),
            currentColor = config.secondaryColor,
            presetColors = presetColors,
            onColorSelected = { viewModel.updateSecondaryColor(it) }
        )

        // Cor de Fundo
        ColorPickerRow(
            label = stringResource(R.string.custom_theme_background_color),
            currentColor = config.backgroundColor,
            presetColors = presetColors + listOf(0xFF000000, 0xFFFFFFFF, 0xFF1E1234),
            onColorSelected = { viewModel.updateBackgroundColor(it) }
        )

        // Cor de Superfície
        ColorPickerRow(
            label = stringResource(R.string.custom_theme_surface_color),
            currentColor = config.surfaceColor,
            presetColors = presetColors,
            onColorSelected = { viewModel.updateSurfaceColor(it) }
        )
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    currentColor: Int,
    presetColors: List<Int>,
    onColorSelected: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presetColors) { color ->
                val isSelected = color == currentColor
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 44.dp else 36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .clickable { onColorSelected(color) }
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, Color.White, CircleShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(color).contrastTextColor(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Seletor de cor customizada (abre o color picker do sistema)
            item {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            // Abrir color picker nativo
                            // Implementar com o ColorPicker do sistema ou uma biblioteca
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.custom_theme_custom_color),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperSection(
    config: CustomThemeConfig,
    viewModel: CustomThemeViewModel
) {
    val context = LocalContext.current
    var showImagePicker by remember { mutableStateOf(false) }

    // Launcher para selecionar imagem da galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setWallpaperFromGallery(it.toString())
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.custom_theme_wallpaper_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Tipo de Wallpaper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WallpaperType.entries.forEach { type ->
                val isSelected = config.wallpaperType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setWallpaperType(type) },
                    label = {
                        Text(
                            when (type) {
                                WallpaperType.SOLID -> stringResource(R.string.wallpaper_type_solid)
                                WallpaperType.GALLERY -> stringResource(R.string.wallpaper_type_gallery)
                                WallpaperType.SERVER -> stringResource(R.string.wallpaper_type_server)
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        when (config.wallpaperType) {
            WallpaperType.SOLID -> {
                ColorPickerRow(
                    label = stringResource(R.string.wallpaper_color),
                    currentColor = config.wallpaperColor,
                    presetColors = listOf(
                        0xFF1E1234, 0xFF000000, 0xFFFFFFFF, 0xFF2196F3,
                        0xFF4CAF50, 0xFFFFC107, 0xFFE91E63, 0xFF9C27B0,
                        0xFFFF5722, 0xFF607D8B, 0xFF795548, 0xFF3F51B5
                    ),
                    onColorSelected = { viewModel.setWallpaperColor(it) }
                )
            }

            WallpaperType.GALLERY -> {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wallpaper_select_from_gallery))
                }

                config.wallpaperUri?.let { uri ->
                    AsyncImage(
                        model = Uri.parse(uri),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            WallpaperType.SERVER -> {
                Text(
                    stringResource(R.string.wallpaper_server_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SERVER_WALLPAPERS) { url ->
                        val isSelected = config.wallpaperUrl == url
                        Box(
                            modifier = Modifier
                                .size(120.dp, 80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setWallpaperFromServer(url) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    } else Modifier
                                )
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Controles adicionais (blur e dim)
        if (config.wallpaperType != WallpaperType.SOLID) {
            Column {
                SliderWithLabel(
                    label = stringResource(R.string.wallpaper_blur),
                    value = config.wallpaperBlur,
                    onValueChange = { viewModel.setWallpaperBlur(it) },
                    valueRange = 0f..1f
                )
                SliderWithLabel(
                    label = stringResource(R.string.wallpaper_dim),
                    value = config.wallpaperDim,
                    onValueChange = { viewModel.setWallpaperDim(it) },
                    valueRange = 0f..0.8f
                )
            }
        }
    }
}

@Composable
private fun SliderWithLabel(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Utilitário para contraste de cor
fun Color.contrastTextColor(): Color {
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return if (luminance > 0.5) Color.Black else Color.White
}