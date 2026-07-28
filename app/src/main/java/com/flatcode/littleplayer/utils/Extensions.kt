package com.flatcode.littleplayer.utils

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.size.Scale
import coil.transform.Transformation
import com.flatcode.littleplayer.R
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration

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

fun Context.getLibraryColor(attrName: String): Int {
    val id = resources.getIdentifier(attrName, "attr", packageName)
    return if (id != 0) getColorFromAttr(id) else android.graphics.Color.WHITE
}

fun <T> Flow<T>.collectWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            collect { action(it) }
        }
    }
}

fun Long.formatAsTime(): String {
    val totalSeconds = this / 1000
    val seconds = (totalSeconds % 60).toString()
    val minutes = (totalSeconds / 60).toString()
    return if (seconds.length == 1) "$minutes:0$seconds" else "$minutes:$seconds"
}

fun Duration.formatAsTime(): String {
    val totalSeconds = this.inWholeSeconds
    val seconds = (totalSeconds % 60).toString()
    val minutes = (totalSeconds / 60).toString()
    return if (seconds.length == 1) "$minutes:0$seconds" else "$minutes:$seconds"
}

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.isVisible(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun View.snackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}

fun ImageView.loadSongImage(albumId: String?, path: String? = null, cachedPath: String? = null) {
    val model: Any = if (!cachedPath.isNullOrEmpty()) {
        File(cachedPath)
    } else if (albumId != null && albumId != "-1" && albumId != "0") {
        getSongArt(albumId)
    } else if (!path.isNullOrEmpty()) {
        getAlbumArtBytes(path) ?: R.color.image_profile
    } else {
        R.color.image_profile
    }

    load(model) {
        crossfade(true)
        placeholder(R.color.image_profile)
        error(R.color.image_profile)
    }
}

fun ImageView.loadSongImageByPath(path: String?, cachedPath: String? = null) {
    val model: Any = if (!cachedPath.isNullOrEmpty()) {
        File(cachedPath)
    } else if (!path.isNullOrEmpty()) {
        getAlbumArtBytes(path) ?: R.color.image_profile
    } else {
        R.color.image_profile
    }
    load(model) {
        crossfade(true)
        placeholder(R.color.image_profile)
        error(R.color.image_profile)
    }
}

fun ImageView.loadSongImageBlur(
    albumId: String?, level: Int, path: String? = null, cachedPath: String? = null
) {
    val model: Any = if (!cachedPath.isNullOrEmpty()) {
        File(cachedPath)
    } else if (albumId != null && albumId != "-1" && albumId != "0") {
        getSongArt(albumId)
    } else if (!path.isNullOrEmpty()) {
        getAlbumArtBytes(path) ?: R.color.image_profile
    } else {
        R.color.image_profile
    }

    load(model) {
        crossfade(true)
        placeholder(R.color.image_profile)
        error(R.color.image_profile)
        transformations(SimpleBlurTransformation(level.toFloat()))
    }
}

fun ImageView.loadCachedAlbumImage(cachedPath: String?) {
    load(if (!cachedPath.isNullOrEmpty()) File(cachedPath) else R.color.image_profile) {
        scale(Scale.FILL)
        crossfade(true)
        placeholder(R.color.image_profile)
        error(R.color.image_profile)
    }
}

private fun getSongArt(albumId: String?): Any {
    return if (!albumId.isNullOrEmpty()) {
        ContentUris.withAppendedId(
            "content://media/external/audio/albumart".toUri(), albumId.toLong()
        )
    } else {
        R.color.image_profile
    }
}

fun Player.togglePlayPause(button: ImageView, onPause: () -> Unit, onStart: () -> Unit) {
    if (isPlaying) {
        button.setImageResource(R.drawable.ic_play)
        pause()
        onPause()
    } else {
        button.setImageResource(R.drawable.ic_pause)
        play()
        onStart()
    }
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
    imm.showSoftInput(this, 0)
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
        val scaleFactor = 8
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