package com.goldensystem.auris.utils

import android.content.Context
import android.media.AudioManager
import android.text.format.Formatter
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.goldensystem.auris.data.model.VideoItem

object VideoUtils {

    fun ExoPlayer.safeSeekTo(positionMs: Long): Boolean {
        val d = duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: return false
        seekTo(positionMs.coerceIn(0, d))
        return true
    }

    fun ExoPlayer.safeSeekBy(deltaMs: Long): Boolean {
        val current = currentPosition.takeIf { it >= 0 } ?: return false
        return safeSeekTo(current + deltaMs)
    }

    fun formatDuration(ms: Long): String = VideoItem.formatDuration(ms)

    fun formatFileSize(context: Context, bytes: Long): String {
        return if (bytes > 0) Formatter.formatShortFileSize(context, bytes) else ""
    }

    fun extractFolderName(folderPath: String): String =
        folderPath.substringAfterLast("/").ifEmpty { "Raiz" }

    data class FolderInfo(
        val name: String,
        val path: String,
        val videoCount: Int,
        val representativeVideo: VideoItem?
    )

    fun extractFolders(videos: List<VideoItem>): List<FolderInfo> {
        return videos.groupBy { it.folderPath }.map { (path, folderVideos) ->
            FolderInfo(
                name = extractFolderName(path),
                path = path,
                videoCount = folderVideos.size,
                representativeVideo = folderVideos.firstOrNull()
            )
        }.sortedBy { it.name.lowercase() }
    }
}