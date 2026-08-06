package com.flatcode.littleplayer.model

import android.os.Parcelable
import com.flatcode.littleplayer.utils.DATA
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
    val playCount: Int = 0,
    val cachedImagePath: String? = null,
    val lyrics: String? = null,
    val dateAdded: Long = 0,
    val size: Long = 0,
    val year: Int = 0,
    val color: Int? = null
) : Parcelable {
    val safeTitle: String
        get() = title ?: DATA.UNKNOWN

    val safeArtist: String
        get() = artist ?: DATA.UNKNOWN

    val durationLong: Long
        get() = duration?.toLongOrNull() ?: 0L

    val durationDuration: Duration
        get() = durationLong.milliseconds
}