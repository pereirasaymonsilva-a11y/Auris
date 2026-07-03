package com.goldensystem.auris.presentation.viewmodel

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldensystem.auris.data.model.QueueContext
import com.goldensystem.auris.data.model.VideoItem
import com.goldensystem.auris.data.model.VideoQueue
import com.goldensystem.auris.data.repository.ViewCountRepository
import com.goldensystem.auris.utils.VideoQueueManager
import com.goldensystem.auris.utils.VideoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GalleryUiState(
    val allVideos: List<VideoItem> = emptyList(),
    val filteredVideos: List<VideoItem> = emptyList(),
    val folders: List<VideoUtils.FolderInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentContext: QueueContext = QueueContext.ALL,
    val sortMode: SortMode = SortMode.DATE_DESC,
    val currentFolder: String? = null,
    val showFoldersOnly: Boolean = false
) {
    val displayVideos: List<VideoItem>
        get() = if (searchQuery.isBlank()) filteredVideos
        else filteredVideos.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.folderName.contains(searchQuery, ignoreCase = true)
        }
}

enum class SortMode(val label: String) {
    NAME_ASC("Nome A-Z"), NAME_DESC("Nome Z-A"),
    DATE_DESC("Mais recentes"), DATE_ASC("Mais antigos"),
    DURATION_DESC("Maior duração"), DURATION_ASC("Menor duração"),
    SIZE_DESC("Maior tamanho"), SIZE_ASC("Menor tamanho")
}

@HiltViewModel
class VideoGalleryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val viewCountRepository: ViewCountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var _allVideos = mutableListOf<VideoItem>()
    private val thumbnailCache = mutableMapOf<Long, Bitmap>()

    private var previousContext: QueueContext = QueueContext.ALL
    private var wasInFoldersMode: Boolean = false

    init { loadVideos() }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val rawVideos = withContext(Dispatchers.IO) { fetchAllVideos() }
                val viewCountMap = withContext(Dispatchers.IO) {
                    viewCountRepository.getAllViewCountsFlow().first()
                }
                val videosWithCounts = rawVideos.map { video ->
                    video.copy(viewCount = viewCountMap[video.id] ?: 0)
                }
                _allVideos = videosWithCounts.toMutableList()

                val folders = VideoUtils.extractFolders(videosWithCounts)
                _uiState.update {
                    it.copy(
                        allVideos = videosWithCounts,
                        filteredVideos = applySortAndContext(videosWithCounts, it.currentContext, it.sortMode),
                        folders = folders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun getVideoThumbnail(videoId: Long): Bitmap? {
        // Verifica se já está no cache
        thumbnailCache[videoId]?.let { return it }

        val bitmap = try {
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoId)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) - método novo e recomendado
                context.contentResolver.loadThumbnail(uri, Size(320, 180), null)
            } else {
                // Android 9 ou inferior - método antigo
                @Suppress("DEPRECATION")
                MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver,
                    videoId,
                    MediaStore.Video.Thumbnails.MICRO_KIND,
                    null
                )
            }
        } catch (e: Exception) {
            null
        }

        // Guarda no cache se não for null
        bitmap?.let { thumbnailCache[videoId] = it }
        return bitmap
    }

    fun clearThumbnailCache() {
        thumbnailCache.clear()
    }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun setSortMode(mode: SortMode) {
        _uiState.update { state ->
            state.copy(
                sortMode = mode,
                filteredVideos = applySortAndContext(state.allVideos, state.currentContext, mode)
            )
        }
    }

    fun setContext(contextType: QueueContext) {
        _uiState.update { state ->
            state.copy(
                currentContext = contextType,
                filteredVideos = applySortAndContext(state.allVideos, contextType, state.sortMode),
                currentFolder = if (contextType == QueueContext.FOLDER) state.currentFolder else null,
                showFoldersOnly = false
            )
        }
    }

    fun setShowFoldersOnly(show: Boolean) {
        _uiState.update { it.copy(showFoldersOnly = show) }
        if (show) loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            val folders = VideoUtils.extractFolders(_uiState.value.allVideos)
            _uiState.update { it.copy(folders = folders, showFoldersOnly = true, filteredVideos = emptyList()) }
        }
    }

    fun enterFolder(folderPath: String) {
        previousContext = _uiState.value.currentContext
        wasInFoldersMode = _uiState.value.showFoldersOnly
        _uiState.update { state ->
            val folderVideos = state.allVideos.filter { it.folderPath == folderPath }
            state.copy(
                currentContext = QueueContext.FOLDER,
                currentFolder = folderPath,
                filteredVideos = applySort(folderVideos, state.sortMode),
                showFoldersOnly = false
            )
        }
    }

    fun exitFolder() {
        if (wasInFoldersMode) {
            _uiState.update { state ->
                state.copy(
                    currentContext = QueueContext.ALL,
                    currentFolder = null,
                    showFoldersOnly = true,
                    filteredVideos = emptyList()
                )
            }
            loadFolders()
        } else {
            val restoredContext = previousContext
            _uiState.update { state ->
                state.copy(
                    currentContext = restoredContext,
                    currentFolder = null,
                    filteredVideos = applySortAndContext(state.allVideos, restoredContext, state.sortMode),
                    showFoldersOnly = false
                )
            }
        }
    }

    fun buildQueue(clickedVideo: VideoItem? = null): VideoQueue {
        val state = _uiState.value
        val sourceVideos = when (state.currentContext) {
            QueueContext.ALL -> state.allVideos
            QueueContext.RECENT -> state.allVideos.filter { isRecent(it.dateAddedMs) }
            QueueContext.FOLDER -> state.allVideos.filter { it.folderPath == state.currentFolder }
            QueueContext.SEARCH -> state.displayVideos
        }
        return if (clickedVideo != null) VideoQueueManager.startFromItem(state.currentContext, sourceVideos, clickedVideo)
        else VideoQueueManager.build(state.currentContext, sourceVideos)
    }

    fun incrementViewCount(videoId: Long) {
        viewModelScope.launch {
            viewCountRepository.incrementViewCount(videoId)
            
            val viewCountMap = withContext(Dispatchers.IO) {
                viewCountRepository.getAllViewCountsFlow().first().toMap()
            }
            
            _allVideos = _allVideos.map { video ->
                video.copy(viewCount = viewCountMap[video.id] ?: 0)
            }.toMutableList()
            
            _uiState.update { state ->
                state.copy(
                    allVideos = _allVideos,
                    filteredVideos = applySortAndContext(_allVideos, state.currentContext, state.sortMode)
                )
            }
        }
    }

    fun getFeaturedVideo(): VideoItem? {
        val state = _uiState.value
        val sourceVideos = when (state.currentContext) {
            QueueContext.ALL -> state.allVideos
            QueueContext.RECENT -> state.allVideos.filter { isRecent(it.dateAddedMs) }
            else -> state.allVideos
        }
        if (sourceVideos.isEmpty()) return null
        val maxView = sourceVideos.maxByOrNull { it.viewCount }
        return if (maxView?.viewCount == 0) sourceVideos.maxByOrNull { it.dateAddedMs } else maxView
    }

    private fun applySortAndContext(videos: List<VideoItem>, context: QueueContext, mode: SortMode): List<VideoItem> {
        val base = when (context) {
            QueueContext.ALL -> videos
            QueueContext.RECENT -> videos.filter { isRecent(it.dateAddedMs) }
            QueueContext.FOLDER -> videos.filter { it.folderPath == _uiState.value.currentFolder }
            QueueContext.SEARCH -> videos
        }
        return if (context == QueueContext.RECENT) {
            base.sortedByDescending { it.dateAddedMs }
        } else {
            applySort(base, mode)
        }
    }

    private fun applySort(videos: List<VideoItem>, mode: SortMode): List<VideoItem> = when (mode) {
        SortMode.NAME_ASC -> videos.sortedBy { it.title.lowercase() }
        SortMode.NAME_DESC -> videos.sortedByDescending { it.title.lowercase() }
        SortMode.DATE_DESC -> videos.sortedByDescending { it.dateAddedMs }
        SortMode.DATE_ASC -> videos.sortedBy { it.dateAddedMs }
        SortMode.DURATION_DESC -> videos.sortedByDescending { it.durationMs }
        SortMode.DURATION_ASC -> videos.sortedBy { it.durationMs }
        SortMode.SIZE_DESC -> videos.sortedByDescending { it.sizeBytes }
        SortMode.SIZE_ASC -> videos.sortedBy { it.sizeBytes }
    }

    private fun isRecent(dateAddedMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
        return dateAddedMs > sevenDaysAgo
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
        context.contentResolver.query(collection, projection, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { cursor ->
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
                videos.add(VideoItem(
                    id = id,
                    title = cursor.getString(nameCol) ?: "Desconhecido",
                    path = path,
                    durationMs = cursor.getLong(durCol),
                    resolution = if (width > 0 && height > 0) "${width}x${height}" else "",
                    sizeBytes = cursor.getLong(sizeCol),
                    folderPath = path.substringBeforeLast("/", ""),
                    dateAddedMs = cursor.getLong(dateCol) * 1000L,
                    width = width, height = height,
                    viewCount = 0
                ))
            }
        }
        return videos
    }
}