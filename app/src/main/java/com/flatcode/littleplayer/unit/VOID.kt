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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.flatcode.littleplayer.R
import jp.wasabeef.glide.transformations.BlurTransformation

object VOID {
    fun intentClear(context: Context, c: Class<*>?) {
        val intent = Intent(context, c)
        context.startActivity(intent)
    }

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

    fun intentExtra2(
        context: Context, c: Class<*>?, key: String?, value: String?, key2: String?, value2: String?
    ) {
        val intent = Intent(context, c)
        intent.putExtra(key, value)
        intent.putExtra(key2, value2)
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

    fun glide(context: Context?, url: Bitmap?, image: ImageView) {
        try {
            Glide.with(context!!).load(url).placeholder(R.color.image_profile).into(image)
        } catch (_: Exception) {
            image.setImageResource(R.color.image_profile)
        }
    }

    fun glideBitmap(context: Context, url: Bitmap?, image: ImageView) {
        try {
            if (url != null) Glide.with(context).load(url).placeholder(R.color.image_profile)
                .into(image)
            else Glide.with(context).load(R.drawable.logo).into(image)
        } catch (_: java.lang.Exception) {
            image.setImageResource(R.drawable.logo)
        }
    }

    fun glideBlurBitmap(context: Context, url: Bitmap?, image: ImageView, level: Int) {
        try {
            if (url != null) Glide.with(context).load(url).placeholder(R.color.image_profile)
                .apply(bitmapTransform(BlurTransformation(level))).into(image)
            else Glide.with(context).load(R.drawable.logo)
                .apply(bitmapTransform(BlurTransformation(level))).into(image)
        } catch (_: java.lang.Exception) {
            image.setImageResource(R.drawable.logo)
        }
    }

    fun glideByte(context: Context, url: ByteArray?, image: ImageView) {
        try {
            if (url != null) {
                Glide.with(context)
                    .load(url)
                    .placeholder(R.color.image_profile)
                    .into(image)
            } else {
                Glide.with(context)
                    .load(R.drawable.logo)
                    .into(image)
            }
        } catch (_: Exception) {
            image.setImageResource(R.drawable.logo)
        }
    }

    fun glideBlurByte(context: Context, url: ByteArray?, image: ImageView, level: Int) {
        try {
            if (url != null) {
                Glide.with(context)
                    .load(url)
                    .placeholder(R.color.image_profile)
                    .apply(bitmapTransform(BlurTransformation(level)))
                    .into(image)
            } else {
                Glide.with(context)
                    .load(R.drawable.logo)
                    .apply(bitmapTransform(BlurTransformation(level)))
                    .into(image)
            }
        } catch (_: Exception) {
            image.setImageResource(R.drawable.logo)
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