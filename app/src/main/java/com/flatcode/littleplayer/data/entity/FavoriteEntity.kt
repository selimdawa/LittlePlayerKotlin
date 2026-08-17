package com.flatcode.littleplayer.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites_table")
data class FavoriteEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val albumId: String? = null,
    val duration: String?,
    val path: String,
    @ColumnInfo(defaultValue = "0") val timestamp: Long = System.currentTimeMillis()
)