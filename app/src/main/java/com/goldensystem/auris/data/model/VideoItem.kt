package com.goldensystem.auris.data.model

data class VideoItem(
    val id: Long,
    val title: String,
    val contentUri: String,
    val duration: Long,
    val resolution: String = "",   // ex.: "1080p"
    val size: Long = 0L            // em bytes
)