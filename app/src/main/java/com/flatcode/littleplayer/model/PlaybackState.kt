package com.flatcode.littleplayer.model

import android.net.Uri

data class PlaybackState(
    val songTitle: String = "",
    val artistName: String = "",
    val duration: Int = 0,
    val currentPosition: Int = 0,
    val isPlaying: Boolean = false,
    val isShuffleOn: Boolean = false,
    val isRepeatOn: Boolean = false,
    val albumArtUri: Uri? = null,
    val songDurationText: String = "00:00"
)