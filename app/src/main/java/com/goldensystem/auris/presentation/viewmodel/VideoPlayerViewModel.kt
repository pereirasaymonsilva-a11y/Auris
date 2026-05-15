package com.goldensystem.auris.presentation.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.goldensystem.auris.data.model.QueueContext
import com.goldensystem.auris.data.model.VideoItem
import com.goldensystem.auris.data.model.VideoQueue
import com.goldensystem.auris.utils.VideoUtils.safeSeekBy
import com.goldensystem.auris.utils.VideoUtils.safeSeekTo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class PlayerState { IDLE, BUFFERING, READY, ENDED, ERROR }

data class VideoPlayerUiState(
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

    private val initialQueue: VideoQueue = savedStateHandle.get<VideoQueue>("queue") ?: VideoQueue.EMPTY
    private val _uiState = MutableStateFlow(VideoPlayerUiState(queue = initialQueue, currentVideo = initialQueue.current ?: VideoItem.EMPTY))
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    var exoPlayer: ExoPlayer? = null
        private set

    private var wasPlayingBeforePause = false
    private var positionUpdater: kotlinx.coroutines.Job? = null

    init {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(getApplication(), permission) != PackageManager.PERMISSION_GRANTED) {
            _uiState.update { it.copy(errorMessage = "Permissão de armazenamento não concedida") }
            return
        }

        // Se a fila estiver vazia, carrega todos os vídeos como fallback
        if (initialQueue.isEmpty) {
            loadFallbackQueue()
        } else if (initialQueue.current != null && initialQueue.current!!.path.isNotBlank()) {
            initializePlayer()
        } else {
            _uiState.update { it.copy(errorMessage = "Nenhum vídeo disponível") }
        }
    }

    private fun loadFallbackQueue() {
        viewModelScope.launch {
            try {
                val videos = withContext(Dispatchers.IO) { fetchAllVideos() }
                if (videos.isNotEmpty()) {
                    val newQueue = VideoQueue(
                        videos = videos,
                        currentIndex = 0,
                        context = QueueContext.ALL
                    )
                    _uiState.update { it.copy(queue = newQueue, currentVideo = newQueue.current ?: VideoItem.EMPTY) }
                    initializePlayer()
                } else {
                    _uiState.update { it.copy(errorMessage = "Nenhum vídeo encontrado no dispositivo") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erro ao carregar vídeos") }
            }
        }
    }

    private suspend fun fetchAllVideos(): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE, MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT, MediaStore.Video.Media.DATE_ADDED
        )
        getApplication<Application>().contentResolver.query(
            collection, projection, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol) ?: continue
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                videos.add(
                    VideoItem(
                        id = id,
                        title = cursor.getString(nameCol) ?: "Desconhecido",
                        path = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString(),
                        durationMs = cursor.getLong(durCol),
                        resolution = if (width > 0 && height > 0) "${width}x${height}" else "",
                        sizeBytes = cursor.getLong(sizeCol),
                        folderPath = path.substringBeforeLast("/", ""),
                        dateAddedMs = cursor.getLong(dateCol) * 1000L,
                        width = width,
                        height = height
                    )
                )
            }
        }
        return videos
    }

    private fun initializePlayer() {
        val video = _uiState.value.currentVideo
        if (video == VideoItem.EMPTY || video.path.isBlank()) return
        releasePlayer()

        val contentUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            video.id
        )

        val player = ExoPlayer.Builder(getApplication()).build().apply {
            setMediaItem(MediaItem.fromUri(contentUri))
            playWhenReady = true
            prepare()

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
                    if (state == Player.STATE_ENDED) advanceToNext()
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

    fun playPause() = exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() }

    fun seekBy(deltaMs: Long): Boolean = exoPlayer?.safeSeekBy(deltaMs) ?: false
    fun seekTo(positionMs: Long): Boolean = exoPlayer?.safeSeekTo(positionMs) ?: false

    fun advanceToNext() {
        val currentQueue = _uiState.value.queue
        if (!currentQueue.hasNext) return
        val nextQueue = currentQueue.moveToNext()
        playQueueItem(nextQueue)
    }

    fun goToPrevious() {
        val currentQueue = _uiState.value.queue
        if (!currentQueue.hasPrevious) return
        val prevQueue = currentQueue.moveToPrevious()
        playQueueItem(prevQueue)
    }

    private fun playQueueItem(newQueue: VideoQueue) {
        val video = newQueue.current ?: return
        val player = exoPlayer ?: return
        if (video.path.isBlank()) return
        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video.id)
        player.setMediaItem(MediaItem.fromUri(contentUri))
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
                val player = exoPlayer
                if (player != null && player.isPlaying) {
                    _uiState.update {
                        it.copy(
                            currentPositionMs = player.currentPosition,
                            durationMs = player.duration.takeIf { d -> d > 0 && d != C.TIME_UNSET } ?: it.durationMs
                        )
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdater?.cancel()
        positionUpdater = null
    }
}