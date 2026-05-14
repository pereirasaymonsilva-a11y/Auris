package com.goldensystem.auris.utils

import com.goldensystem.auris.data.model.QueueContext
import com.goldensystem.auris.data.model.VideoItem
import com.goldensystem.auris.data.model.VideoQueue

object VideoQueueManager {

    fun build(
        context: QueueContext,
        videos: List<VideoItem>,
        startIndex: Int = 0
    ): VideoQueue {
        if (videos.isEmpty()) return VideoQueue.EMPTY
        return VideoQueue(
            videos = videos,
            currentIndex = startIndex.coerceIn(0, videos.lastIndex),
            context = context
        )
    }

    fun moveToNext(queue: VideoQueue): VideoQueue = queue.moveToNext()

    fun moveToPrevious(queue: VideoQueue): VideoQueue = queue.moveToPrevious()

    fun startFromItem(
        context: QueueContext,
        allVideos: List<VideoItem>,
        clickedVideo: VideoItem
    ): VideoQueue {
        val index = allVideos.indexOfFirst { it.id == clickedVideo.id }
        return build(context, allVideos, startIndex = index.coerceAtLeast(0))
    }

    fun buildFolderQueue(
        folderPath: String,
        allVideos: List<VideoItem>,
        startIndex: Int = 0
    ): VideoQueue {
        val folderVideos = allVideos.filter { it.folderPath == folderPath }
        return build(QueueContext.FOLDER, folderVideos, startIndex)
    }

    fun canAdvance(queue: VideoQueue): Boolean = queue.hasNext
    fun canGoBack(queue: VideoQueue): Boolean = queue.hasPrevious
}