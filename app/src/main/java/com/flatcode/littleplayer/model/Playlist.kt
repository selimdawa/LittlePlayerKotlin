package com.flatcode.littleplayer.model

data class Playlist(
    val name: String, val songCount: Int, val firstSongPath: String? = null
)