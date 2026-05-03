package com.goldensystem.auris.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldensystem.auris.data.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoGalleryViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            withContext(Dispatchers.IO) {
                try {
                    val videoList = mutableListOf<VideoItem>()

                    // Primeira tentativa: MediaStore (rápido)
                    val projection = arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.TITLE,
                        MediaStore.Video.Media.DURATION
                    )
                    val cursor = context.contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        "${MediaStore.Video.Media.DATE_ADDED} DESC"
                    )
                    cursor?.use {
                        val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                        val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                        while (it.moveToNext()) {
                            val id = it.getLong(idCol)
                            videoList.add(
                                VideoItem(
                                    id = id,
                                    title = it.getString(titleCol) ?: "Sem título",
                                    contentUri = Uri.withAppendedPath(
                                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                        id.toString()
                                    ).toString(),
                                    duration = it.getLong(durationCol)
                                )
                            )
                        }
                    }

                    // Se o MediaStore não trouxe nada, faz varredura bruta de TODAS as pastas
                    if (videoList.isEmpty()) {
                        val root = Environment.getExternalStorageDirectory() // /storage/emulated/0
                        val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp")
                        var tempId = 1L

                        root.walkTopDown().forEach { file ->
                            if (file.isFile && file.extension.lowercase() in videoExtensions) {
                                videoList.add(
                                    VideoItem(
                                        id = tempId++,
                                        title = file.nameWithoutExtension,
                                        contentUri = Uri.fromFile(file).toString(),
                                        duration = 0L
                                    )
                                )
                            }
                        }
                    }

                    _videos.value = videoList
                } catch (e: Exception) {
                    _errorMessage.value = "Erro: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
}