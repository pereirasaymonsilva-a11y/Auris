package com.goldensystem.auris.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.goldensystem.auris.data.model.VideoItem
import com.goldensystem.auris.data.model.VideoQueue
import com.goldensystem.auris.utils.VideoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlayerState { IDLE, BUFFERING, READY, ENDED, ERROR }

data class PlayerUiState(
    val currentVideo: VideoItem = VideoItem.EMPTY,
    val queue: VideoQueue = VideoQueue.EMPTY,
    val playerState: PlayerState = PlayerState.IDLE,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val queue: VideoQueue = savedStateHandle.get<VideoQueue>("queue") ?: VideoQueue.EMPTY

    private val _uiState = MutableStateFlow(
        PlayerUiState(queue = queue, currentVideo = queue.current ?: VideoItem.EMPTY)
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var exoPlayer: ExoPlayer? = null
        private set

    private var wasPlayingBeforePause = false
    private var positionUpdater: kotlinx.coroutines.Job? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        val video = queue.current ?: return
        releasePlayer()

        val player = ExoPlayer.Builder(getApplication()).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(video.path)))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    _uiState.update {
                        it.copy(
                            playerState = when (state) {
                                Player.STATE_IDLE -> PlayerState.IDLE
                                Player.STATE_BUFFERING -> PlayerState.BUFFERING
                                Player.STATE_READY -> PlayerState.READY
                                Player.STATE_ENDED -> PlayerState.ENDED
                                else -> PlayerState.ERROR
                            }
                        )
                    }
                    if (state == Player.STATE_ENDED) {
                        advanceToNext()
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    _uiState.update { it.copy(isPlaying = playing) }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _uiState.update { it.copy(playerState = PlayerState.ERROR, errorMessage = error.message) }
                }
            })
        }

        exoPlayer = player
        startPositionUpdates()
    }

    fun playPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekBy(deltaMs: Long): Boolean {
        val player = exoPlayer ?: return false
        return player.safeSeekBy(deltaMs)
    }

    fun seekTo(positionMs: Long): Boolean {
        val player = exoPlayer ?: return false
        return player.safeSeekTo(positionMs)
    }

    fun advanceToNext() {
        if (!queue.hasNext) return
        val nextQueue = queue.moveToNext()
        playQueueItem(nextQueue)
    }

    fun goToPrevious() {
        if (!queue.hasPrevious) return
        val prevQueue = queue.moveToPrevious()
        playQueueItem(prevQueue)
    }

    private fun playQueueItem(newQueue: VideoQueue) {
        val video = newQueue.current ?: return
        val player = exoPlayer ?: return

        player.setMediaItem(MediaItem.fromUri(Uri.parse(video.path)))
        player.prepare()
        player.playWhenReady = true

        _uiState.update { it.copy(queue = newQueue, currentVideo = video, errorMessage = null) }
    }

    fun onResume() {
        if (wasPlayingBeforePause) {
            exoPlayer?.play()
            wasPlayingBeforePause = false
        }
    }

    fun onPause() {
        wasPlayingBeforePause = exoPlayer?.isPlaying == true
        exoPlayer?.pause()
    }

    fun releasePlayer() {
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    private fun startPositionUpdates() {
        positionUpdater?.cancel()
        positionUpdater = viewModelScope.launch {
            while (isActive) {
                val player = exoPlayer ?: continue
                val pos = player.currentPosition
                val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
                _uiState.update {
                    it.copy(
                        currentPositionMs = pos,
                        durationMs = dur ?: it.durationMs
                    )
                }
                delay(250)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdater?.cancel()
        positionUpdater = null
    }
}