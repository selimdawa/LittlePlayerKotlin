package com.flatcode.littleplayer.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.ImageView
import androidx.core.net.toUri
import coil.load
import coil.size.Scale
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.service.MusicService
import java.io.File

object VOID {

    fun intent1(context: Context, c: Class<*>?) {
        val intent = Intent(context, c)
        context.startActivity(intent)
    }

    fun intentExtra(context: Context, c: Class<*>?, key: String?, value: String?) {
        val intent = Intent(context, c)
        intent.putExtra(key, value)
        context.startActivity(intent)
    }

    fun intentExtraInt(context: Context, c: Class<*>?, key: String?, value: Int) {
        val intent = Intent(context, c)
        intent.putExtra(key, value)
        context.startActivity(intent)
    }

    fun intentExtra2Int(
        context: Context, c: Class<*>?, key: String?, value: String?, key2: String?, value2: Int
    ) {
        val intent = Intent(context, c)
        intent.putExtra(key, value)
        intent.putExtra(key2, value2)
        context.startActivity(intent)
    }

    fun coil(url: Bitmap?, image: ImageView) {
        image.load(url) {
            crossfade(true)
            placeholder(R.color.image_profile)
            error(R.color.image_profile)
        }
    }

    fun coilBitmap(url: Bitmap?, image: ImageView) {
        image.load(url ?: R.drawable.logo) {
            crossfade(true)
            placeholder(R.color.image_profile)
            error(R.drawable.logo)
        }
    }

    fun coilImage(context: Context, songId: String?, image: ImageView, size: Int) {
        if (!songId.isNullOrEmpty()) {
            try {
                val trackUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong()
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bitmap: Bitmap = context.contentResolver.loadThumbnail(
                        trackUri, Size(size, size), null
                    )
                    image.load(bitmap) {
                        crossfade(true)
                        placeholder(R.drawable.logo)
                        error(R.drawable.logo)
                    }
                } else {
                    val sArtworkUri = "content://media/external/audio/albumart".toUri()
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, songId.toLong())
                    image.load(albumArtUri) {
                        crossfade(true)
                        placeholder(R.drawable.logo)
                        error(R.drawable.logo)
                    }
                }
            } catch (_: Exception) {
                image.load(R.drawable.logo)
            }
        } else {
            image.load(R.drawable.logo)
        }
    }

    fun coilImageBlur(context: Context, songId: String?, image: ImageView, level: Int) {
        if (!songId.isNullOrEmpty()) {
            try {
                val trackUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong()
                )
                val coilRadius = (level / 4f).coerceIn(1f, 25f)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bitmap: Bitmap = context.contentResolver.loadThumbnail(
                        trackUri, Size(150, 150), null
                    )
                    image.load(bitmap) {
                        crossfade(true)
                        placeholder(R.drawable.logo)
                        error(R.drawable.logo)
                        transformations(CoilBlurTransformation(context, coilRadius))
                    }
                } else {
                    val sArtworkUri = "content://media/external/audio/albumart".toUri()
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, songId.toLong())
                    image.load(albumArtUri) {
                        crossfade(true)
                        placeholder(R.drawable.logo)
                        error(R.drawable.logo)
                        transformations(CoilBlurTransformation(context, coilRadius))
                    }
                }
            } catch (_: Exception) {
                image.load(R.drawable.logo) {
                    val coilRadius = (level / 4f).coerceIn(1f, 25f)
                    transformations(CoilBlurTransformation(context, coilRadius))
                }
            }
        } else {
            image.load(R.drawable.logo) {
                val coilRadius = (level / 4f).coerceIn(1f, 25f)
                transformations(CoilBlurTransformation(context, coilRadius))
            }
        }
    }

    fun coilAlbumImage(cachedPath: String?, image: ImageView) {
        if (!cachedPath.isNullOrEmpty()) {
            image.load(File(cachedPath)) {
                scale(Scale.FILL)
                crossfade(true)
                placeholder(R.drawable.logo)
                error(R.drawable.logo)
            }
        } else {
            image.load(R.drawable.logo)
        }
    }

    fun loadRawAlbumArt(songPath: String?): Bitmap? {
        if (songPath.isNullOrEmpty()) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(songPath)
            val art = retriever.embeddedPicture
            if (art != null) {
                BitmapFactory.decodeByteArray(art, 0, art.size)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun paletteGradient(bitmap: Bitmap, imageViewBlur: ImageView) {
        androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
            palette?.let {
                val startColor = it.getDarkVibrantColor(0xFF212121.toInt())
                val endColor = it.getDarkMutedColor(0xFF121212.toInt())
                val gradientDrawable = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(startColor, endColor)
                )
                imageViewBlur.setImageDrawable(null)
                imageViewBlur.background = gradientDrawable
            }
        }
    }

    fun playPauseBtn(
        service: MusicService?, button: ImageView, onPause: () -> Unit, onStart: () -> Unit
    ) {
        service?.let {
            if (it.isPlaying()) {
                button.setBackgroundResource(R.drawable.ic_play)
                button.setImageResource(R.drawable.ic_play)
                it.pause()
                onPause()
            } else {
                button.setBackgroundResource(R.drawable.ic_pause)
                button.setImageResource(R.drawable.ic_pause)
                it.start()
                onStart()
            }
        }
    }
}