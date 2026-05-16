package com.goldensystem.auris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ViewCountDao {
    @Query("SELECT viewCount FROM view_counts WHERE videoId = :videoId")
    suspend fun getViewCount(videoId: Long): Int?

    @Query("SELECT * FROM view_counts")
    fun getAllViewCounts(): Flow<List<ViewCountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ViewCountEntity)

    @Query("UPDATE view_counts SET viewCount = viewCount + 1 WHERE videoId = :videoId")
    suspend fun increment(videoId: Long)

    @Query("DELETE FROM view_counts")
    suspend fun clear()
}