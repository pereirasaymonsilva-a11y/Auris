package com.goldensystem.auris.data.repository

import com.goldensystem.auris.data.local.AppDatabase
import com.goldensystem.auris.data.local.ViewCountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewCountRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val dao = database.viewCountDao()

    suspend fun getViewCount(videoId: Long): Int {
        return dao.getViewCount(videoId) ?: 0
    }

    fun getAllViewCountsFlow(): Flow<Map<Long, Int>> {
        return dao.getAllViewCounts().map { list ->
            list.associate { it.videoId to it.viewCount }
        }
    }

    suspend fun incrementViewCount(videoId: Long) {
    val rowsUpdated = dao.incrementIfExists(videoId)
    if (rowsUpdated == 0) {
        dao.insertIfNotExists(ViewCountEntity(videoId, 1))
    }
}

    suspend fun clearAll() {
        dao.clear()
    }
}