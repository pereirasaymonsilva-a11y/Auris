package com.goldensystem.auris.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.goldensystem.auris.data.model.QueueContext
import com.goldensystem.auris.data.model.VideoItem
import com.goldensystem.auris.data.model.VideoQueue
import com.goldensystem.auris.presentation.viewmodel.GalleryUiState
import com.goldensystem.auris.presentation.viewmodel.SortMode
import com.goldensystem.auris.presentation.viewmodel.VideoGalleryViewModel
import com.goldensystem.auris.utils.VideoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoGalleryScreen(
    onOpenPlayerWithQueue: (VideoQueue) -> Unit,
    onBack: () -> Unit,
    viewModel: VideoGalleryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GalleryTopBar(
                state = state,
                onSearchChange = viewModel::setSearchQuery,
                onSortChange = viewModel::setSortMode,
                onContextChange = viewModel::setContext,
                onBack = {
                    if (state.currentContext == QueueContext.FOLDER) viewModel.exitFolder()
                    else onBack()
                },
                isInFolder = state.currentContext == QueueContext.FOLDER
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.errorMessage != null -> ErrorState(state.errorMessage!!) { viewModel.loadVideos() }
                state.showFoldersOnly && state.folders.isEmpty() -> EmptyState()
                state.displayVideos.isEmpty() && !state.showFoldersOnly && state.searchQuery.isNotBlank() -> EmptySearchState()
                state.displayVideos.isEmpty() && !state.showFoldersOnly && state.searchQuery.isBlank() -> EmptyState()
                else -> {
                    Column {
                        if (state.currentContext != QueueContext.FOLDER && state.searchQuery.isBlank()) {
                            ContextTabs(
                                current = state.currentContext,
                                showFoldersOnly = state.showFoldersOnly,
                                onChange = viewModel::setContext,
                                onToggleShowFolders = viewModel::setShowFoldersOnly
                            )
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.showFoldersOnly) {
                                items(state.folders, key = { it.path }) { folder ->
                                    FolderItem(folder) { viewModel.enterFolder(folder.path) }
                                }
                            } else {
                                items(state.displayVideos, key = { it.id }) { video ->
                                    VideoGridItem(
                                        video = video,
                                        onClick = { onOpenPlayerWithQueue(viewModel.buildQueue(video)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(
    state: GalleryUiState,
    onSearchChange: (String) -> Unit,
    onSortChange: (SortMode) -> Unit,
    onContextChange: (QueueContext) -> Unit,
    onBack: () -> Unit,
    isInFolder: Boolean
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column {
        CenterAlignedTopAppBar(
            title = {
                if (isInFolder) {
                    Text(text = state.currentFolder?.substringAfterLast("/") ?: "Pasta", fontWeight = FontWeight.SemiBold)
                } else {
                    Text("Vídeos", fontWeight = FontWeight.Bold)
                }
            },
            navigationIcon = {
                if (isInFolder || state.currentContext == QueueContext.FOLDER) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            },
            actions = {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Filled.Sort, "Ordenar")
                }
            }
        )

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            placeholder = { Text("Pesquisar vídeos...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label, fontWeight = if (mode == state.sortMode) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onSortChange(mode); showSortMenu = false }
                )
            }
        }
    }
}

@Composable
private fun ContextTabs(
    current: QueueContext,
    showFoldersOnly: Boolean,
    onChange: (QueueContext) -> Unit,
    onToggleShowFolders: (Boolean) -> Unit
) {
    TabRow(selectedTabIndex = if (showFoldersOnly) 3 else current.ordinal) {
        Tab(selected = current == QueueContext.ALL && !showFoldersOnly, onClick = {
            onChange(QueueContext.ALL)
            onToggleShowFolders(false)
        }, text = { Text("Todos") })
        Tab(selected = current == QueueContext.RECENT && !showFoldersOnly, onClick = {
            onChange(QueueContext.RECENT)
            onToggleShowFolders(false)
        }, text = { Text("Recentes") })
        Tab(selected = current == QueueContext.FOLDER && !showFoldersOnly, onClick = {
            // Já está em uma pasta? Se sim, não faz nada. Senão, mostra a lista de pastas.
            if (current != QueueContext.FOLDER) onToggleShowFolders(true)
        }, text = { Text("Pastas") })
    }
}

@Composable
private fun VideoGridItem(video: VideoItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Box(modifier = Modifier.aspectRatio(16f / 9f)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.path)
                        .videoFrameMillis(1000)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))
                    )
                )
                Text(
                    text = video.durationFormatted,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                )
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.Center).size(28.dp)
                )
            }
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun FolderItem(folder: VideoUtils.FolderInfo, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "folderScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Folder, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(folder.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${folder.videoCount} vídeos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun LoadingState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
@Composable private fun ErrorState(message: String, onRetry: () -> Unit) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(message, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)); TextButton(onClick = onRetry) { Text("Tentar novamente") } } }
@Composable private fun EmptyState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhum vídeo encontrado", style = MaterialTheme.typography.titleMedium) }
@Composable private fun EmptySearchState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhum resultado para a busca", style = MaterialTheme.typography.titleMedium) }