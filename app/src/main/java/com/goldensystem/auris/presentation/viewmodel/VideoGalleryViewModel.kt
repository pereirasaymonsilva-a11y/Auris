package com.goldensystem.auris.presentation.viewmodel

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

    init {
        loadVideos()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
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
            _isLoading.value = false
        }
    }
}