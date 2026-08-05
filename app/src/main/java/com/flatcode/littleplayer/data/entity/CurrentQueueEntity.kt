package com.flatcode.littleplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_queue_table")
data class CurrentQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: String,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumId: String?,
    val duration: String?,
    val path: String?,
    val cachedImagePath: String?,
    val lyrics: String? = null,
    val orderIndex: Int
)