package com.goldensystem.auris.presentation.screens

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    fileUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }
    var isBuffering by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showThumbnail by remember { mutableStateOf(true) }

    var doubleTapFeedback by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var toggleBounce by remember { mutableStateOf(false) }
    val bounceScale by animateFloatAsState(
        targetValue = if (toggleBounce) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce",
        finishedListener = { toggleBounce = false }
    )

    var adjustmentFeedback by remember { mutableStateOf<AdjustmentType?>(null) }
    var adjustmentValue by remember { mutableFloatStateOf(0f) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse(fileUri)
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = (state == Player.STATE_BUFFERING)
                    if (state == Player.STATE_READY) {
                        // CORREÇÃO: usar duration direto, sem player.
                        duration = duration.coerceAtLeast(0L)
                        showThumbnail = false
                    }
                }
            })
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            if (!isSeeking) {
                currentPosition = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    DisposableEffect(player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.playWhenReady = false
                Lifecycle.Event.ON_RESUME -> { if (isPlaying) player.playWhenReady = true }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    val window = (context as? android.app.Activity)?.window
    LaunchedEffect(Unit) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            val controller = WindowInsetsControllerCompat(it, it.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            window?.let {
                val controller = WindowInsetsControllerCompat(it, it.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(it, true)
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    fun revealControls() {
        showControls = true
    }

    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(showControls, isSeeking, isDragging) {
        if (showControls && !isSeeking && !isDragging) {
            delay(3000)
            showControls = false
        }
    }

    BackHandler { onBack() }

    var enterAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        enterAnimation = true
    }
    val enterAlpha by animateFloatAsState(
        targetValue = if (enterAnimation) 1f else 0f,
        animationSpec = tween(500),
        label = "enterAlpha"
    )
    val enterScale by animateFloatAsState(
        targetValue = if (enterAnimation) 1f else 0.92f,
        animationSpec = tween(500),
        label = "enterScale"
    )

    var dragStartX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                alpha = enterAlpha
                scaleX = enterScale
                scaleY = enterScale
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player   // Atribuição correta
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    controllerAutoShow = false
                    controllerHideOnTouch = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (showThumbnail) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(fileUri))
                    .videoFrameMillis(1000)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )

        if (isBuffering && !showThumbnail) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.8f),
                strokeWidth = 3.dp
            )
        }

        doubleTapFeedback?.let { (offsetX, offsetY) ->
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

        adjustmentFeedback?.let { type ->
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
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(adjustmentValue * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(80.dp)
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
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(300)),
            exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it / 8 }, animationSpec = tween(300))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .statusBarsPadding()
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .pointerInput(player) {
                            detectTapGestures {
                                if (player.isPlaying) player.pause() else player.play()
                                toggleBounce = true
                                revealControls()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(36.dp).scale(bounceScale)) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "playPause"
                        ) { playing ->
                            Icon(
                                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                if (playing) "Pausar" else "Reproduzir",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = if (isSeeking) seekPosition else currentPosition.toFloat().coerceAtMost(duration.toFloat()),
                        onValueChange = { value ->
                            isSeeking = true
                            seekPosition = value
                            revealControls()
                        },
                        onValueChangeFinished = {
                            player.seekTo(seekPosition.toLong())
                            isSeeking = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        thumb = {
                            SliderDefaults.Thumb(
                                interactionSource = remember { MutableInteractionSource() },
                                colors = SliderDefaults.colors(thumbColor = Color.White)
                            )
                        },
                        valueRange = 0f..(duration.toFloat().coerceAtLeast(1f))
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Camada unificada de gestos
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { revealControls() },
                        onDoubleTap = { offset ->
                            val seekAmount = if (offset.x < size.width / 2f) -10000L else 10000L
                            val newPosition = (player.currentPosition + seekAmount).coerceIn(0, player.duration)
                            player.seekTo(newPosition)
                            doubleTapFeedback = offset.x / size.width to offset.y / size.height
                            revealControls()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragStartX = offset.x
                            isDragging = true
                            adjustmentFeedback = if (dragStartX < size.width / 2)
                                AdjustmentType.BRIGHTNESS else AdjustmentType.VOLUME
                            revealControls()
                        },
                        onVerticalDrag = { _, dragAmount ->
                            val delta = -dragAmount / size.height.toFloat()
                            val halfWidth = size.width / 2f
                            if (dragStartX < halfWidth) {
                                val current = window?.attributes?.screenBrightness ?: 0.5f
                                val new = (current + delta).coerceIn(0.01f, 1.0f)
                                window?.attributes = window?.attributes?.apply {
                                    screenBrightness = new
                                }
                                adjustmentValue = new
                            } else {
                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val newVol = (currentVol + (delta * maxVol).toInt()).coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                adjustmentValue = newVol.toFloat() / maxVol.toFloat()
                            }
                            adjustmentFeedback = if (dragStartX < halfWidth)
                                AdjustmentType.BRIGHTNESS else AdjustmentType.VOLUME
                        },
                        onDragEnd = {
                            isDragging = false
                            adjustmentFeedback = null
                        },
                        onDragCancel = {
                            isDragging = false
                            adjustmentFeedback = null
                        }
                    )
                }
        )
    }
}

private enum class AdjustmentType { BRIGHTNESS, VOLUME }

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}