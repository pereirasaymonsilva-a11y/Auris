package com.goldensystem.auris.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoQueue(
    val videos: List<VideoItem>,
    val currentIndex: Int = 0,
    val context: QueueContext = QueueContext.ALL
) : Parcelable {

    val current: VideoItem?
        get() = videos.getOrNull(currentIndex)

    val hasNext: Boolean get() = currentIndex < videos.lastIndex
    val hasPrevious: Boolean get() = currentIndex > 0
    val isEmpty: Boolean get() = videos.isEmpty()
    val size: Int get() = videos.size
    val positionDescription: String
        get() = if (isEmpty) "0 de 0" else "${currentIndex + 1} de $size"

    fun moveToNext(): VideoQueue =
        if (hasNext) copy(currentIndex = currentIndex + 1) else this

    fun moveToPrevious(): VideoQueue =
        if (hasPrevious) copy(currentIndex = currentIndex - 1) else this

    fun startAt(index: Int): VideoQueue =
        copy(currentIndex = index.coerceIn(0, videos.lastIndex))

    companion object {
        val EMPTY = VideoQueue(emptyList())
    }
}