package com.flatcode.littleplayer.utils

import android.app.Activity
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.load
import coil.size.Scale
import coil.transform.Transformation
import com.flatcode.littleplayer.R
import java.io.File

inline fun <reified T : Activity> Context.launchActivity(
    extras: Intent.() -> Unit = {}
) {
    val intent = Intent(this, T::class.java)
    intent.extras()
    startActivity(intent)
}

fun Context.getColorFromAttr(attr: Int): Int {
    val typedValue = android.util.TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

fun ImageView.loadBitmap(bitmap: Bitmap?) {
    load(bitmap) {
        crossfade(enable = true)
        placeholder(R.color.image_profile)
        error(R.color.image_profile)
    }
}

fun ImageView.loadLogoOrBitmap(bitmap: Bitmap?) {
    load(bitmap ?: R.drawable.logo) {
        crossfade(enable = true)
        placeholder(R.color.image_profile)
        error(R.drawable.logo)
    }
}

fun ImageView.loadSongImage(context: Context, songId: String?, path: String?, size: Int) {
    if (!path.isNullOrEmpty()) {
        val embeddedArt = loadRawAlbumArt(path)
        if (embeddedArt != null) {
            load(embeddedArt) {
                crossfade(enable = true)
                placeholder(R.drawable.logo)
                error(R.drawable.logo)
            }
            return
        }
    }

    if (!songId.isNullOrEmpty()) {
        try {
            val trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong()
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bitmap: Bitmap = context.contentResolver.loadThumbnail(
                    trackUri, Size(size, size), null
                )
                load(bitmap) {
                    crossfade(enable = true)
                    placeholder(R.drawable.logo)
                    error(R.drawable.logo)
                }
            } else {
                val sArtworkUri = "content://media/external/audio/albumart".toUri()
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, songId.toLong())
                load(albumArtUri) {
                    crossfade(enable = true)
                    placeholder(R.drawable.logo)
                    error(R.drawable.logo)
                }
            }
        } catch (_: Exception) {
            load(R.drawable.logo)
        }
    } else {
        load(R.drawable.logo)
    }
}

fun ImageView.loadSongImageBlur(context: Context, songId: String?, path: String?, level: Int) {
    val coilRadius = (level / 2f).coerceIn(1f, 50f)

    if (!path.isNullOrEmpty()) {
        val embeddedArt = loadRawAlbumArt(path)
        if (embeddedArt != null) {
            load(embeddedArt) {
                crossfade(enable = true)
                placeholder(R.drawable.logo)
                error(R.drawable.logo)
                transformations(SimpleBlurTransformation(coilRadius))
            }
            return
        }
    }

    if (!songId.isNullOrEmpty()) {
        try {
            val trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong()
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bitmap: Bitmap = context.contentResolver.loadThumbnail(
                    trackUri, Size(150, 150), null
                )
                load(bitmap) {
                    crossfade(enable = true)
                    placeholder(R.drawable.logo)
                    error(R.drawable.logo)
                    transformations(SimpleBlurTransformation(coilRadius))
                }
            } else {
                val sArtworkUri = "content://media/external/audio/albumart".toUri()
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, songId.toLong())
                load(albumArtUri) {
                    crossfade(enable = true)
                    placeholder(R.drawable.logo)
                    error(R.drawable.logo)
                    transformations(SimpleBlurTransformation(coilRadius))
                }
            }
        } catch (_: Exception) {
            load(R.drawable.logo) {
                transformations(SimpleBlurTransformation(coilRadius))
            }
        }
    } else {
        load(R.drawable.logo) {
            transformations(SimpleBlurTransformation(coilRadius))
        }
    }
}

fun ImageView.loadCachedAlbumImage(cachedPath: String?) {
    if (!cachedPath.isNullOrEmpty()) {
        load(File(cachedPath)) {
            scale(Scale.FILL)
            crossfade(enable = true)
            placeholder(R.drawable.logo)
            error(R.drawable.logo)
        }
    } else {
        load(R.drawable.logo)
    }
}

fun ImageView.setPaletteGradient(bitmap: Bitmap) {
    Palette.from(bitmap).generate { palette ->
        palette?.let {
            val startColor = it.getDarkVibrantColor(0xFF212121.toInt())
            val endColor = it.getDarkMutedColor(0xFF121212.toInt())
            val gradientDrawable = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(startColor, endColor)
            )
            setImageDrawable(null)
            background = gradientDrawable
        }
    }
}

fun Player.togglePlayPause(button: ImageView, onPause: () -> Unit, onStart: () -> Unit) {
    if (isPlaying) {
        button.setBackgroundResource(R.drawable.ic_play)
        button.setImageResource(R.drawable.ic_play)
        pause()
        onPause()
    } else {
        button.setBackgroundResource(R.drawable.ic_pause)
        button.setImageResource(R.drawable.ic_pause)
        play()
        onStart()
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

fun getAlbumArtBytes(path: String?): ByteArray? {
    if (path.isNullOrEmpty()) return null
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        val art = retriever.embeddedPicture
        retriever.release()
        art
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

class SimpleBlurTransformation(private val radius: Float) : Transformation {
    override val cacheKey: String = "${SimpleBlurTransformation::class.java.name}-$radius"

    override suspend fun transform(input: Bitmap, size: coil.size.Size): Bitmap {
        val scaleFactor = 4
        val w = (input.width / scaleFactor).coerceAtLeast(1)
        val h = (input.height / scaleFactor).coerceAtLeast(1)
        val small = input.scale(w, h, true)
        val r = (radius / scaleFactor).toInt().coerceAtLeast(1)
        val pix = IntArray(w * h)
        small.getPixels(pix, 0, w, 0, 0, w, h)
        val blurred = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var rs = 0
            var gs = 0
            var bs = 0
            var c = 0
            for (i in -r..r) {
                val p = pix[y * w + ((x + i).coerceIn(0, w - 1))]
                rs += (p shr 16) and 0xff
                gs += (p shr 8) and 0xff
                bs += p and 0xff
                c++
            }
            blurred[y * w + x] = (0xff shl 24) or (rs / c shl 16) or (gs / c shl 8) or (bs / c)
        }
        for (x in 0 until w) for (y in 0 until h) {
            var rs = 0
            var gs = 0
            var bs = 0
            var c = 0
            for (i in -r..r) {
                val p = blurred[((y + i).coerceIn(0, h - 1)) * w + x]
                rs += (p shr 16) and 0xff
                gs += (p shr 8) and 0xff
                bs += p and 0xff
                c++
            }
            pix[y * w + x] = (0xff shl 24) or (rs / c shl 16) or (gs / c shl 8) or (bs / c)
        }
        val output = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(pix, 0, w, 0, 0, w, h)
        return output.scale(input.width, input.height, true)
    }
}
