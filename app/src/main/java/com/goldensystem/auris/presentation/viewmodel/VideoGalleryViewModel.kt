package com.goldensystem.auris.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
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

    init {
        loadVideos()
    }

    private fun hasVideoPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (!hasVideoPermission()) {
                    _errorMessage.value = "Permissão para acessar vídeos não concedida. Conceda nas configurações."
                    _isLoading.value = false
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    val videoList = mutableListOf<VideoItem>()
                    val projection = arrayOf(
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.TITLE,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DURATION
                    )
                    val cursor = context.contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        MediaStore.Video.Media.DATE_ADDED + " DESC"
                    )
                    cursor?.use {
                        val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                        val dataCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                        val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                        while (it.moveToNext()) {
                            videoList.add(
                                VideoItem(
                                    id = it.getLong(idCol),
                                    title = it.getString(titleCol) ?: "Sem título",
                                    filePath = it.getString(dataCol) ?: "",
                                    duration = it.getLong(durationCol),
                                    thumbnailUri = null
                                )
                            )
                        }
                    }
                    _videos.value = videoList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao carregar vídeos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}