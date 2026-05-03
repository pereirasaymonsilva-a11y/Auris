package com.goldensystem.auris.presentation.screens

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayerScreen(filePath: String) {
    val context = LocalContext.current
    AndroidView(
        factory = {
            val player = ExoPlayer.Builder(context).build()
            val mediaItem = MediaItem.fromUri(Uri.parse(filePath))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            PlayerView(context).apply { this.player = player }
        },
        modifier = Modifier.fillMaxSize()
    )
}