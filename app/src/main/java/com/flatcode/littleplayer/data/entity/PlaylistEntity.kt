package com.flatcode.littleplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists_table", primaryKeys = ["playlistName", "songId"])
data class PlaylistEntity(
    val playlistName: String,
    val songId: String,
    val title: String,
    val albumId: String? = null,
    val artist: String,
    val path: String
)
