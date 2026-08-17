package com.flatcode.littleplayer.data.entity

import androidx.room.ColumnInfo
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
    val orderIndex: Int,
    @ColumnInfo(defaultValue = "0") val originalOrderIndex: Int = 0,
    val dominantColor: Int? = null,
    val vibrantColor: Int? = null
)