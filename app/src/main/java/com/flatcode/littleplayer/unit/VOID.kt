package com.flatcode.littleplayer.unit

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.ImageView
import androidx.core.net.toUri
import coil.load
import com.flatcode.littleplayer.R

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

    fun coilBlurBitmap(context: Context, url: Bitmap?, image: ImageView, level: Int) {
        val radius = (level / 4f).coerceIn(1f, 25f)
        image.load(url ?: R.drawable.logo) {
            crossfade(true)
            placeholder(if (url != null) R.color.image_profile else R.drawable.logo)
            error(R.drawable.logo)
            transformations(CoilBlurTransformation(context, radius))
        }
    }

    fun coiImage(context: Context, songId: String?, image: ImageView) {
        if (!songId.isNullOrEmpty()) {
            try {
                val trackUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    songId.toLong()
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bitmap: Bitmap = context.contentResolver.loadThumbnail(
                        trackUri,
                        Size(300, 300),
                        null
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

    fun coiImageBlur(context: Context, songId: String?, image: ImageView, level: Int) {
        if (!songId.isNullOrEmpty()) {
            try {
                val trackUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    songId.toLong()
                )
                val coilRadius = (level / 4f).coerceIn(1f, 25f)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bitmap: Bitmap = context.contentResolver.loadThumbnail(
                        trackUri,
                        Size(150, 150),
                        null
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
}