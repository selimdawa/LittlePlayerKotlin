package com.flatcode.littleplayer.model

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
data class MusicFiles(
    val path: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val duration: String? = null,
    val id: String? = null,
    val albumId: String? = null,
    val waveform: String? = null,
    val playCount: Int = 0
) : Parcelable {
    val safeTitle: String
        get() = title ?: "Unknown Track"

    val safeArtist: String
        get() = artist ?: "Unknown Artist"

    val safeUri: Uri?
        get() = path?.toUri()

    val durationLong: Long
        get() = duration?.toLongOrNull() ?: 0L

    val durationDuration: Duration
        get() = durationLong.milliseconds
}