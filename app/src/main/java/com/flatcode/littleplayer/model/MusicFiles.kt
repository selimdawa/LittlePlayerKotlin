package com.flatcode.littleplayer.model

import android.net.Uri
import androidx.core.net.toUri

data class MusicFiles(
    val path: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val duration: String? = null,
    val id: String? = null
) {
    val safeTitle: String
        get() = title ?: "Unknown Track"

    val safeArtist: String
        get() = artist ?: "Unknown Artist"

    val safeUri: Uri?
        get() = path?.toUri()

    val durationLong: Long
        get() = duration?.toLongOrNull() ?: 0L
}