package com.goldensystem.auris.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoItem(
    val id: Long,
    val title: String,
    val path: String,
    val durationMs: Long = 0L,
    val resolution: String = "",
    val sizeBytes: Long = 0L,
    val folderPath: String = "",
    val dateAddedMs: Long = 0L,
    val viewCount: Int = 0,
    val width: Int = 0,
    val height: Int = 0
) : Parcelable {

    val folderName: String
        get() = folderPath.substringAfterLast("/").ifEmpty { "Raiz" }

    val durationFormatted: String
        get() = formatDuration(durationMs)

    val isLandscape: Boolean
        get() = width > 0 && height > 0 && width > height

    companion object {
        val EMPTY = VideoItem(id = -1L, title = "", path = "")

        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
    }
}