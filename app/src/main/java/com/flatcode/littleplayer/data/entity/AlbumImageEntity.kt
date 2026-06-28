package com.flatcode.littleplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album_images_table")
data class AlbumImageEntity(
    @PrimaryKey
    val albumName: String,
    val imagePath: String
)
