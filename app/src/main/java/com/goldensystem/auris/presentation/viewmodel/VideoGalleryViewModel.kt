package com.goldensystem.auris.presentation.viewmodel

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldensystem.auris.data.model.QueueContext
import com.goldensystem.auris.data.model.VideoItem
import com.goldensystem.auris.data.model.VideoQueue
import com.goldensystem.auris.utils.VideoQueueManager
import com.goldensystem.auris.utils.VideoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init { loadVideos() }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val videos = withContext(Dispatchers.IO) { fetchAllVideos() }
                val folders = VideoUtils.extractFolders(videos)
                _uiState.update {
                    it.copy(
                        allVideos = videos,
                        filteredVideos = applySort(videos, it.sortMode),
                        folders = folders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun setSortMode(mode: SortMode) {
        _uiState.update { state -> state.copy(sortMode = mode, filteredVideos = applySort(state.allVideos, mode)) }
    }

    fun setContext(contextType: QueueContext) {
        _uiState.update { state ->
            val videos = when (contextType) {
                QueueContext.ALL -> state.allVideos
                QueueContext.RECENT -> state.allVideos.sortedByDescending { it.dateAddedMs }
                QueueContext.FOLDER -> state.allVideos.filter { it.folderPath == state.currentFolder }
                QueueContext.SEARCH -> state.displayVideos
            }
            state.copy(
                currentContext = contextType,
                filteredVideos = applySort(videos, state.sortMode),
                currentFolder = if (contextType == QueueContext.FOLDER) state.currentFolder else null,
                showFoldersOnly = false
            )
        }
    }

    fun setShowFoldersOnly(show: Boolean) {
        _uiState.update { it.copy(showFoldersOnly = show) }
    }

    fun enterFolder(folderPath: String) {
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

    fun exitFolder() = setContext(QueueContext.ALL)

    fun buildQueue(clickedVideo: VideoItem? = null): VideoQueue {
        val state = _uiState.value
        val sourceVideos = when (state.currentContext) {
            QueueContext.ALL -> state.allVideos
            QueueContext.RECENT -> state.allVideos.sortedByDescending { it.dateAddedMs }
            QueueContext.FOLDER -> state.allVideos.filter { it.folderPath == state.currentFolder }
            QueueContext.SEARCH -> state.displayVideos
        }
        return if (clickedVideo != null) VideoQueueManager.startFromItem(state.currentContext, sourceVideos, clickedVideo)
        else VideoQueueManager.build(state.currentContext, sourceVideos)
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

    private fun fetchAllVideos(): List<VideoItem> {
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
                    path = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString(),
                    durationMs = cursor.getLong(durCol),
                    resolution = if (width > 0 && height > 0) "${width}x${height}" else "",
                    sizeBytes = cursor.getLong(sizeCol),
                    folderPath = path.substringBeforeLast("/", ""),
                    dateAddedMs = cursor.getLong(dateCol) * 1000L,
                    width = width, height = height
                ))
            }
        }
        return videos
    }
}