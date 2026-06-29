// presentation/screens/CustomThemeScreen.kt
package com.goldensystem.auris.presentation.screens

import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
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
    "https://wallpaper.forfun.com/fetch/0f/0faffa5239e20701db8c7de8a72be9b8.jpeg",
    "https://mrwallpaper.com/images/high/a-car-is-on-the-road-at-night-yagjbzto9b57cb7h.jpg",
    "https://i.pinimg.com/originals/a0/22/c6/a022c668083bf4f8b496614729f5e0cd.jpg",
    "https://i.pinimg.com/originals/31/b8/0b/31b80bd952f3999a4310207fd00ad73a.jpg",
    "https://i.pinimg.com/originals/b8/6a/f2/b86af24be084d3ce9f1b97ff4df0fdda.jpg"
)

// Cores principais (12 cores básicas)
val MAIN_COLORS = listOf(
    0xFF000000.toInt(), // Preto
    0xFF795548.toInt(), // Marrom
    0xFFE53935.toInt(), // Vermelho
    0xFFFF9800.toInt(), // Laranja
    0xFFFFEB3B.toInt(), // Amarelo
    0xFF8BC34A.toInt(), // Verde claro
    0xFF2E7D32.toInt(), // Verde escuro
    0xFF42A5F5.toInt(), // Azul claro
    0xFF0D47A1.toInt(), // Azul escuro
    0xFF7B1FA2.toInt(), // Roxo
    0xFFE91E63.toInt(), // Rosa
    0xFFFFFFFF.toInt()  // Branco
)

// Cores adicionais (cores vibrantes e especiais)
val ADDITIONAL_COLORS = listOf(
    0xFFFF6F00.toInt(), // Âmbar
    0xFF00BCD4.toInt(), // Ciano
    0xFF00E676.toInt(), // Verde neon
    0xFFFF4081.toInt(), // Rosa neon
    0xFF651FFF.toInt(), // Roxo profundo
    0xFF2979FF.toInt(), // Azul vibrante
    0xFFFF6E40.toInt(), // Coral
    0xFFF50057.toInt(), // Vermelho neon
    0xFF00E5FF.toInt(), // Ciano claro
    0xFF76FF03.toInt(), // Verde limão
    0xFFD500F9.toInt(), // Magenta
    0xFFFFAB00.toInt()  // Ouro
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
    var resetTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(resetTrigger) {
        if (resetTrigger) {
            viewModel.resetToDefault()
            resetTrigger = false
        }
    }

    // Animações de entrada
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "screen_alpha"
    )

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
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.auth_cd_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { resetTrigger = true },
                        modifier = Modifier
                    ) {
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
                .padding(16.dp)
                .graphicsLayer(alpha = animatedAlpha),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview do Player (mini)
            CustomThemePreviewCard(config = config)

            // Tabs com animação
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = previewColorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.custom_theme_colors)) },
                    modifier = Modifier
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.custom_theme_wallpaper)) },
                    modifier = Modifier
                )
            }

            // Conteúdo com animação de transição
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300, delayMillis = 50)) with
                            fadeOut(animationSpec = tween(200))
                },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    0 -> ColorPickerSection(config = config, viewModel = viewModel)
                    1 -> WallpaperSection(config = config, viewModel = viewModel)
                }
            }

            // Botão Salvar com animação
            val buttonInteractionSource = remember { MutableInteractionSource() }
            val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(
                targetValue = if (isButtonPressed) 0.97f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "button_scale"
            )

            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveCustomTheme()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(buttonScale),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = previewColorScheme.primary,
                    contentColor = previewColorScheme.onPrimary
                ),
                interactionSource = buttonInteractionSource
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "preview_card_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* Apenas para feedback visual */ },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primary.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        .clip(RoundedCornerShape(16.dp))
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
                    
                    // Botões de controle mock com animação
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Icons.Rounded.SkipPrevious,
                            Icons.Rounded.PlayArrow,
                            Icons.Rounded.SkipNext
                        ).forEachIndexed { index, icon ->
                            val controlInteractionSource = remember { MutableInteractionSource() }
                            val isControlPressed by controlInteractionSource.collectIsPressedAsState()
                            val controlScale by animateFloatAsState(
                                targetValue = if (isControlPressed) 0.85f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "control_scale_$index"
                            )
                            
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(controlScale)
                                    .clickable(
                                        interactionSource = controlInteractionSource,
                                        indication = null
                                    ) { /* Apenas feedback */ }
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

        // Cor Primária
        ColorPickerRow(
            label = stringResource(R.string.custom_theme_primary_color),
            currentColor = config.primaryColor,
            mainColors = MAIN_COLORS,
            additionalColors = ADDITIONAL_COLORS,
            onColorSelected = { viewModel.updatePrimaryColor(it) }
        )

        // Cor Secundária
        ColorPickerRow(
            label = stringResource(R.string.custom_theme_secondary_color),
            currentColor = config.secondaryColor,
            mainColors = MAIN_COLORS,
            additionalColors = ADDITIONAL_COLORS,
            onColorSelected = { viewModel.updateSecondaryColor(it) }
        )

        ColorPickerRow(
            label = stringResource(R.string.custom_theme_container_color),
            currentColor = config.containerColor,
            mainColors = MAIN_COLORS,
            additionalColors = ADDITIONAL_COLORS + listOf(0xFF2A1F40.toInt()),
            onColorSelected = { viewModel.updateContainerColor(it) }
        )

        // Cor de Superfície
        ColorPickerRow(
            label = stringResource(R.string.custom_theme_surface_color),
            currentColor = config.surfaceColor,
            mainColors = MAIN_COLORS,
            additionalColors = ADDITIONAL_COLORS,
            onColorSelected = { viewModel.updateSurfaceColor(it) }
        )
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    currentColor: Int,
    mainColors: List<Int>,
    additionalColors: List<Int>,
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
        
        // Primeira linha: Cores principais
        Text(
            "Principais",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mainColors) { color ->
                ColorItem(
                    color = color,
                    isSelected = color == currentColor,
                    onColorSelected = onColorSelected
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Segunda linha: Cores adicionais
        Text(
            "Adicionais",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(additionalColors) { color ->
                ColorItem(
                    color = color,
                    isSelected = color == currentColor,
                    onColorSelected = onColorSelected
                )
            }
        }
    }
}

@Composable
private fun ColorItem(
    color: Int,
    isSelected: Boolean,
    onColorSelected: (Int) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "color_item_scale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "color_item_border"
    )
    val size by animateDpAsState(
        targetValue = if (isSelected) 44.dp else 36.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "color_item_size"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(itemScale)
            .clip(CircleShape)
            .background(Color(color))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onColorSelected(color) }
            .then(
                if (isSelected) {
                    Modifier.border(borderWidth, Color.White, CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                )
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(color).contrastTextColor(),
                    modifier = Modifier.size(16.dp)
                )
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
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val chipScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "chip_scale_${type.name}"
                )
                
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
                    modifier = Modifier
                        .weight(1f)
                        .scale(chipScale),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    interactionSource = interactionSource
                )
            }
        }

        when (config.wallpaperType) {
            WallpaperType.SOLID -> {
                // ===== COR DE FUNDO MOVIDA PARA CÁ =====
                ColorPickerRow(
                    label = stringResource(R.string.custom_theme_background_color),
                    currentColor = config.backgroundColor,
                    mainColors = MAIN_COLORS,
                    additionalColors = ADDITIONAL_COLORS + listOf(0xFF1E1234.toInt()),
                    onColorSelected = { viewModel.updateBackgroundColor(it) }
                )

                // Card de pré-visualização da cor
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(config.backgroundColor)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Pré-visualização do Wallpaper",
                            color = Color(config.backgroundColor).contrastTextColor(),
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            WallpaperType.GALLERY -> {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.wallpaper_select_from_gallery))
                }

                // Mostrar a imagem selecionada - VERTICAL
                if (config.wallpaperUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            model = Uri.parse(config.wallpaperUri),
                            contentDescription = "Wallpaper selecionado",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // Placeholder quando não há imagem
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Nenhuma imagem selecionada",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            WallpaperType.SERVER -> {
                Text(
                    stringResource(R.string.wallpaper_server_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Wallpapers do servidor em formato VERTICAL
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                ) {
                    items(SERVER_WALLPAPERS) { url ->
                        val isSelected = config.wallpaperUrl == url
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val itemScale by animateFloatAsState(
                            targetValue = if (isPressed) 0.95f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                            label = "server_item_scale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(180.dp)
                                .scale(itemScale)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { viewModel.setWallpaperFromServer(url) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
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
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Controles adicionais (blur e dim) - apenas para GALERY e SERVER
        if (config.wallpaperType != WallpaperType.SOLID) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
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