package com.goldensystem.auris.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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

    LaunchedEffect(state.currentVideo) {
        val orientation = if (state.currentVideo.isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        (context as? Activity)?.requestedOrientation = orientation
    }

    DisposableEffect(Unit) {
        onDispose {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onBack()
    }

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
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        if (state.playerState == PlayerState.BUFFERING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 3.dp
            )
        }

        doubleTapFeedback?.let { (offsetX, _) ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.5f, animationSpec = tween(200)),
                exit = fadeOut(tween(300)) + scaleOut(targetScale = 1.5f, animationSpec = tween(300))
            ) {
                Icon(
                    imageVector = if (offsetX < 0.5f) Icons.Filled.FastRewind else Icons.Filled.FastForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = (offsetX - 0.5f).times(200).dp)
                        .size(48.dp)
                )
            }
            LaunchedEffect(Unit) {
                delay(600)
                doubleTapFeedback = null
            }
        }

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
            LaunchedEffect(Unit) {
                delay(800)
                adjustmentType = null
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 8 }),
            exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 8 })
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            onBack()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            state.currentVideo.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
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

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .clickable { viewModel.playPause() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = state.isPlaying, label = "play") { playing ->
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Slider(
                        value = state.currentPositionMs.toFloat().coerceAtMost(state.durationMs.toFloat()),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            VideoUtils.formatDuration(state.currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            VideoUtils.formatDuration(state.durationMs),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { viewModel.goToPrevious() },
                            enabled = state.queue.hasPrevious
                        ) {
                            Icon(Icons.Filled.FastRewind, "Anterior", tint = Color.White)
                        }
                        Text(
                            state.queue.positionDescription,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        IconButton(
                            onClick = { viewModel.advanceToNext() },
                            enabled = state.queue.hasNext
                        ) {
                            Icon(Icons.Filled.FastForward, "Próximo", tint = Color.White)
                        }
                    }
                }
            }
        }

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
                            showControls = true
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
                                window?.attributes = window?.attributes?.apply {
                                    screenBrightness = new
                                }
                                adjustmentType = AdjustmentType.BRIGHTNESS
                                adjustmentValue = new
                            } else {
                                val audioManager =
                                    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                val maxVol =
                                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val currentVol =
                                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val newVol =
                                    (currentVol + (delta * maxVol).toInt()).coerceIn(0, maxVol)
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    newVol,
                                    0
                                )
                                adjustmentType = AdjustmentType.VOLUME
                                adjustmentValue = newVol.toFloat() / maxVol
                            }
                            showControls = true
                        },
                        onDragEnd = {},
                        onDragCancel = {}
                    )
                }
        )
    }
}

private enum class AdjustmentType { BRIGHTNESS, VOLUME }