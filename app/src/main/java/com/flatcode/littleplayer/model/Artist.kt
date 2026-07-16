package com.flatcode.littleplayer.model

data class Artist(
    val name: String,
    val songsCount: Int,
    val sampleSongId: String? = null,
    val sampleSongPath: String? = null
)