package com.flatcode.littleplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs_table")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val path: String,
    val albumId: String? = null,
    val isFavorite: Boolean = false,
    val waveform: String? = null,
    val lyrics: String? = null,
    val playCount: Int = 0,
    val dateAdded: Long = 0,
    val size: Long = 0,
    val year: Int = 0,
    val color: Int? = null
)