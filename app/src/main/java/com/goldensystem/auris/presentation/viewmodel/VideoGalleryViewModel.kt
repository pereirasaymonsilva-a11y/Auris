package com.goldensystem.auris.presentation.viewmodel

import android.content.ContentUris
import android.content.Context
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

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            withContext(Dispatchers.IO) {
                try {
                    val videoList = mutableListOf<VideoItem>()
                    val projection = arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.TITLE,
                        MediaStore.Video.Media.DURATION
                    )
                    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
                    val cursor = context.contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        sortOrder
                    )

                    if (cursor == null) {
                        _errorMessage.value = "Não foi possível acessar os vídeos"
                        return@withContext
                    }

                    cursor.use {
                        val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                        val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                        while (it.moveToNext()) {
                            val id = it.getLong(idCol)
                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            ).toString()
                            videoList.add(
                                VideoItem(
                                    id = id,
                                    title = it.getString(titleCol) ?: "Sem título",
                                    contentUri = contentUri,
                                    duration = it.getLong(durationCol)
                                )
                            )
                        }
                    }
                    _videos.value = videoList
                } catch (e: Exception) {
                    _errorMessage.value = "Erro ao carregar vídeos: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
}