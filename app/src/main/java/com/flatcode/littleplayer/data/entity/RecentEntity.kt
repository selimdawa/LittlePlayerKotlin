package com.flatcode.littleplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_table")
data class RecentEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumId: String? = null,
    val duration: String?,
    val path: String,
    val timestamp: Long = System.currentTimeMillis()
)