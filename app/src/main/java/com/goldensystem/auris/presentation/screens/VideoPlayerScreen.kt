package com.goldensystem.auris.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.goldensystem.auris.presentation.viewmodel.PlayerState
import com.goldensystem.auris.presentation.viewmodel.VideoPlayerViewModel
import com.goldensystem.auris.utils.VideoUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showControls by remember { mutableStateOf(true) }
    var doubleTapFeedback by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var adjustmentType by remember { mutableStateOf<AdjustmentType?>(null) }
    var adjustmentValue by remember { mutableFloatStateOf(0f) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    // Força landscape ao entrar
    LaunchedEffect(Unit) {
        try {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (_: Exception) {}
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        try {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } catch (_: Exception) {}
        onBack()
    }

    // Auto-hide dos controles
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(2500)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).also {
                    playerView = it
                    it.useController = false
                    it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    it.keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Associa o player assim que disponível
        DisposableEffect(viewModel.exoPlayer) {
            playerView?.player = viewModel.exoPlayer
            onDispose { }
        }

        // Thumbnail enquanto idle/buffering
        if (state.playerState == PlayerState.IDLE || state.playerState == PlayerState.BUFFERING) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.currentVideo.path)
                    .videoFrameMillis(1000)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay gradiente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Indicador de buffering
        if (state.playerState == PlayerState.BUFFERING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 3.dp
            )
        }

        // Feedback de double-tap
        doubleTapFeedback?.let { (offsetX, _) ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.5f, animationSpec = tween(200)),
                exit = fadeOut(tween(300)) + scaleOut(targetScale = 1.5f, animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (offsetX < 0.5f) Icons.Filled.Replay10 else Icons.Filled.Forward10,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (offsetX < 0.5f) "-10s" else "+10s",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            LaunchedEffect(Unit) {
                delay(600)
                doubleTapFeedback = null
            }
        }

        // Indicador lateral de brilho/volume
        adjustmentType?.let { type ->
            Box(
                modifier = Modifier
                    .align(if (type == AdjustmentType.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (type == AdjustmentType.BRIGHTNESS) Icons.Outlined.BrightnessHigh else Icons.Outlined.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${(adjustmentValue * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(60.dp)
                            .width(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(adjustmentValue)
                                .width(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                        )
                    }
                }
            }
            LaunchedEffect(adjustmentType, adjustmentValue) {
                if (adjustmentType != null) {
                    delay(800)
                    adjustmentType = null
                }
            }
        }

        // Controles principais (AnimatedVisibility)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 8 }),
            exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 8 })
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Topo: voltar + nome
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            try {
                                (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            } catch (_: Exception) {}
                            onBack()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            state.currentVideo.title,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            state.currentVideo.folderName,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Inferior: seekbar, tempo e botões
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .navigationBarsPadding()
                ) {
                    Slider(
                        value = state.currentPositionMs.toFloat().coerceAtMost(state.durationMs.toFloat()),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        valueRange = 0f..(state.durationMs.toFloat().coerceAtLeast(1f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(VideoUtils.formatDuration(state.currentPositionMs), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        Text(VideoUtils.formatDuration(state.durationMs), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.goToPrevious() }, enabled = state.queue.hasPrevious) {
                            Icon(Icons.Filled.SkipPrevious, "Anterior", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.seekBy(-10000) }) {
                            Icon(Icons.Filled.Replay10, "-10s", tint = Color.White)
                        }
                        IconButton(
                            onClick = { viewModel.playPause() },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.seekBy(10000) }) {
                            Icon(Icons.Filled.Forward10, "+10s", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.advanceToNext() }, enabled = state.queue.hasNext) {
                            Icon(Icons.Filled.SkipNext, "Próximo", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.queue.positionDescription,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // Camada de gestos unificada
        var dragStartX by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = { offset ->
                            val delta = if (offset.x < size.width / 2f) -10000L else 10000L
                            viewModel.seekBy(delta)
                            doubleTapFeedback = offset.x / size.width to offset.y / size.height
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> dragStartX = offset.x },
                        onVerticalDrag = { _, dragAmount ->
                            val delta = -dragAmount / size.height.toFloat()
                            if (dragStartX < size.width / 2) {
                                val window = (context as? Activity)?.window
                                val current = window?.attributes?.screenBrightness ?: 0.5f
                                val new = (current + delta).coerceIn(0.01f, 1.0f)
                                window?.attributes = window?.attributes?.apply { screenBrightness = new }
                                adjustmentType = AdjustmentType.BRIGHTNESS
                                adjustmentValue = new
                            } else {
                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val newVol = (currentVol + (delta * maxVol).toInt()).coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI)
                                adjustmentType = AdjustmentType.VOLUME
                                adjustmentValue = newVol.toFloat() / maxVol
                            }
                        },
                        onDragEnd = {},
                        onDragCancel = {}
                    )
                }
        )
    }
}

private enum class AdjustmentType { BRIGHTNESS, VOLUME }