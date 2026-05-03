package com.goldensystem.auris.data.model

data class VideoItem(
    val id: Long,
    val title: String,
    val filePath: String,
    val thumbnailUri: String?,
    val duration: Long
)