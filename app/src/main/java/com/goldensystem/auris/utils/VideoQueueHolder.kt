package com.goldensystem.auris.utils

import com.goldensystem.auris.data.model.VideoQueue

object VideoQueueHolder {
    private var queue: VideoQueue? = null
    fun setQueue(q: VideoQueue) { queue = q }
    fun getQueue(): VideoQueue? = queue
}