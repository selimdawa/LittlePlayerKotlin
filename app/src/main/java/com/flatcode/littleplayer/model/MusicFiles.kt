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
    val cachedBlurPath: String? = null,
    val dominantColor: Int? = null,
    val vibrantColor: Int? = null,
    val dateAdded: Long = 0,
    val size: Long = 0,
    val year: Int = 0,
    val songsCount: Int = 0,
    val isPlaying: Boolean = false
) : Parcelable {
    val safeTitle: String
        get() = title ?: DATA.UNKNOWN

    val safeArtist: String
        get() = artist ?: DATA.UNKNOWN

    val safeAlbum: String
        get() = album ?: DATA.UNKNOWN

    val durationLong: Long
        get() = duration?.toLongOrNull() ?: 0L

    val durationDuration: Duration
        get() = durationLong.milliseconds

    companion object {
        fun getCleanedAlbum(album: String?, path: String?): String {
            if (album == null || album.isBlank() || album == DATA.UNKNOWN) return DATA.UNKNOWN
            
            // Logic used in Songs fragment via MusicRepository
            val folderName = if (!path.isNullOrBlank()) {
                val file = java.io.File(path)
                file.parentFile?.name
            } else null

            return if (album.equals(folderName, ignoreCase = true)) DATA.UNKNOWN else album
        }
    }
}