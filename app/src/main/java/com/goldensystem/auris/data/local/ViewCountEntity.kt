package com.goldensystem.auris.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "view_counts")
data class ViewCountEntity(
    @PrimaryKey
    val videoId: Long,
    var viewCount: Int = 0
)