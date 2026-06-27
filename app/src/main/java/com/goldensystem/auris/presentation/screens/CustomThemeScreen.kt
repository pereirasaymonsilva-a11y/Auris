package com.goldensystem.auris.presentation.screens

import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.navigation.NavController
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.goldensystem.auris.R
import com.goldensystem.auris.data.preferences.CustomThemeConfig
import com.goldensystem.auris.data.preferences.WallpaperType
import com.goldensystem.auris.presentation.navigation.Screen
import com.goldensystem.auris.presentation.viewmodel.CustomThemeViewModel
import com.goldensystem.auris.ui.theme.COLOR_PALETTE
import com.goldensystem.auris.ui.theme.customColorScheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeScreen(
    navController: NavController,
    viewModel: CustomThemeViewModel = hiltViewModel()
) {
    val config by viewModel.customThemeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }

    val previewColorScheme = remember(config) {
        customColorScheme(config, true)
    }
    var resetTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(resetTrigger) {
        if (resetTrigger) {
            viewModel.resetToDefault()
            resetTrigger = false
        }
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
                    IconButton(onClick = { resetTrigger = true }) {
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
            CustomThemePreviewCard(config = config)

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
    var showColorPalette by remember { mutableStateOf(false) }
    var pendingColorTarget by remember { mutableStateOf<((Int) -> Unit)?>(null) }
    
    val presetColors = COLOR_PALETTE.map { it.color.toArgb() }
    val colorNameMap = COLOR_PALETTE.associate { it.color.toArgb() to it.name }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.custom_theme_colors_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ColorPickerRow(
            label = stringResource(R.string.custom_theme_primary_color),
            currentColor = config.primaryColor,
            presetColors = presetColors,
            colorNameMap = colorNameMap,
            onColorSelected = { viewModel.updatePrimaryColor(it) },
            onOpenPalette = {
                pendingColorTarget = viewModel::updatePrimaryColor
                showColorPalette = true
            }
        )

        ColorPickerRow(
            label = stringResource(R.string.custom_theme_secondary_color),
            currentColor = config.secondaryColor,
            presetColors = presetColors,
            colorNameMap = colorNameMap,
            onColorSelected = { viewModel.updateSecondaryColor(it) },
            onOpenPalette = {
                pendingColorTarget = viewModel::updateSecondaryColor
                showColorPalette = true
            }
        )

        ColorPickerRow(
            label = stringResource(R.string.custom_theme_background_color),
            currentColor = config.backgroundColor,
            presetColors = presetColors + listOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
            colorNameMap = colorNameMap,
            onColorSelected = { viewModel.updateBackgroundColor(it) },
            onOpenPalette = {
                pendingColorTarget = viewModel::updateBackgroundColor
                showColorPalette = true
            }
        )

        ColorPickerRow(
            label = stringResource(R.string.custom_theme_surface_color),
            currentColor = config.surfaceColor,
            presetColors = presetColors,
            colorNameMap = colorNameMap,
            onColorSelected = { viewModel.updateSurfaceColor(it) },
            onOpenPalette = {
                pendingColorTarget = viewModel::updateSurfaceColor
                showColorPalette = true
            }
        )
    }

    if (showColorPalette) {
        Dialog(
            onDismissRequest = {
                showColorPalette = false
                pendingColorTarget = null
            }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ColorPaletteDialogContent(
                    onColorSelected = { colorArgb ->
                        pendingColorTarget?.invoke(colorArgb)
                        showColorPalette = false
                        pendingColorTarget = null
                    },
                    onDismiss = {
                        showColorPalette = false
                        pendingColorTarget = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    currentColor: Int,
    presetColors: List<Int>,
    colorNameMap: Map<Int, String> = emptyMap(),
    onColorSelected: (Int) -> Unit,
    onOpenPalette: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            
            if (colorNameMap.isNotEmpty()) {
                val currentColorName = colorNameMap[currentColor] ?: ""
                if (currentColorName.isNotEmpty()) {
                    Text(
                        currentColorName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presetColors) { color ->
                val isSelected = color == currentColor
                val colorName = colorNameMap[color] ?: ""
                
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
                            contentDescription = colorName.ifEmpty { null },
                            tint = Color(color).contrastTextColor(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onOpenPalette() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Abrir paleta de cores",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteDialogContent(
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colorCategories = remember {
        val grouped = COLOR_PALETTE.groupBy { item ->
            when {
                item.name.contains("Rosa") || item.name.contains("Vermelho") || item.name.contains("Magenta") -> "Rosa / Vermelho"
                item.name.contains("Roxo") || item.name.contains("Lilás") || item.name.contains("Violeta") -> "Roxo / Violeta"
                item.name.contains("Índigo") || item.name.contains("Azul") && !item.name.contains("Ciano") && !item.name.contains("Turquesa") -> "Azul"
                item.name.contains("Ciano") || item.name.contains("Teal") || item.name.contains("Turquesa") -> "Ciano / Teal"
                item.name.contains("Verde") || item.name.contains("Menta") || item.name.contains("Limão") || item.name.contains("Esmeralda") -> "Verde"
                item.name.contains("Amarelo") || item.name.contains("Ouro") || item.name.contains("Mostarda") -> "Amarelo / Ouro"
                item.name.contains("Laranja") || item.name.contains("Pêssego") || item.name.contains("Salmão") -> "Laranja / Pêssego"
                item.name.contains("Marrom") || item.name.contains("Café") || item.name.contains("Terra") -> "Marrom / Terra"
                item.name.contains("Cinza") || item.name.contains("Branco") || item.name.contains("Preto") -> "Cinza / Neutros"
                else -> "Cores Especiais"
            }
        }
        grouped.toList().sortedBy { it.first }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Escolha uma cor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Fechar")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.heightIn(max = 500.dp)
        ) {
            colorCategories.forEach { (category, items) ->
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { item ->
                                val colorArgb = item.color.toArgb()
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                        .clickable {
                                            onColorSelected(colorArgb)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.name.take(1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = item.color.contrastTextColor(),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
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
                val presetColors = COLOR_PALETTE.map { it.color.toArgb() }
                val colorNameMap = COLOR_PALETTE.associate { it.color.toArgb() to it.name }
                
                ColorPickerRow(
                    label = stringResource(R.string.wallpaper_color),
                    currentColor = config.wallpaperColor,
                    presetColors = presetColors,
                    colorNameMap = colorNameMap,
                    onColorSelected = { viewModel.setWallpaperColor(it) },
                    onOpenPalette = {
                        // Implementar se quiser abrir a paleta para wallpaper também
                    }
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

                val serverWallpapers = listOf(
                    "https://example.com/wallpaper1.jpg",
                    "https://example.com/wallpaper2.jpg",
                    "https://example.com/wallpaper3.jpg",
                    "https://example.com/wallpaper4.jpg",
                    "https://example.com/wallpaper5.jpg"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(serverWallpapers) { url ->
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

fun Color.contrastTextColor(): Color {
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return if (luminance > 0.5) Color.Black else Color.White
}