@file:Suppress("SpellCheckingInspection")

package com.flatcode.littleplayer.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.Options
import coil.size.Scale
import coil.transform.Transformation
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.selimdawa.multicolors.MultiColorManager
import io.selimdawa.multicolors.MultiColorTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Locale
import kotlin.time.Duration

inline fun <reified T : Activity> Context.launchActivity(
    options: Bundle? = null,
    extras: Intent.() -> Unit = {},
) {
    val intent = Intent(this, T::class.java)
    intent.extras()
    startActivity(intent, options)
}

/* OK */
fun Context.getColorFromAttr(@AttrRes attr: Int, fallback: Int = Color.WHITE): Int {
    val typedValue = TypedValue()
    if (theme.resolveAttribute(attr, typedValue, true)) {
        return typedValue.data
    }
    if (applicationContext.theme.resolveAttribute(attr, typedValue, true)) {
        return typedValue.data
    }
    return fallback
}

fun Long.formatAsSize(): String {
    val sizeMb = this.toDouble() / (1024 * 1024)
    return String.format(Locale.getDefault(), "%.2f MB", sizeMb)
}

val Context.appVersionName: String
    get() = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }

fun View.setGradientBackground(
    @ColorInt startColor: Int,
    @ColorInt endColor: Int,
    layerIndex: Int = 0,
) {
    val background = background?.mutate()
    val shape = if (background is LayerDrawable) {
        background.getDrawable(layerIndex) as? GradientDrawable
    } else {
        background as? GradientDrawable
    }
    shape?.colors = intArrayOf(startColor, endColor)
}

fun View.setHaloBackground(
    @ColorInt startColor: Int,
    @ColorInt endColor: Int,
) {
    val background = background?.mutate() as? LayerDrawable ?: return
    val count = background.numberOfLayers
    if (count < 2) return

    val r = (Color.red(startColor) + Color.red(endColor)) / 2
    val g = (Color.green(startColor) + Color.green(endColor)) / 2
    val b = (Color.blue(startColor) + Color.blue(endColor)) / 2

    val haloColor = Color.argb(90, r, g, b)

    // Layer 0: Outer Fog
    (background.getDrawable(0) as? GradientDrawable)?.colors =
        intArrayOf(haloColor, Color.TRANSPARENT)

    if (count >= 3) {
        // Layer 1: Inner Glow (Optional)
        (background.getDrawable(1) as? GradientDrawable)?.colors =
            intArrayOf(Color.TRANSPARENT, haloColor)
        // Last Layer: Main Border
        (background.getDrawable(count - 1) as? GradientDrawable)?.colors =
            intArrayOf(startColor, endColor)
    } else {
        // Layer 1: Main Border (If only 2 layers)
        (background.getDrawable(1) as? GradientDrawable)?.colors = intArrayOf(startColor, endColor)
    }
}

fun View.setHaloSolidBackground(@ColorInt color: Int) {
    setHaloBackground(color, color)
}

fun View.setSolidBackground(@ColorInt color: Int, layerIndex: Int = 0) {
    setGradientBackground(color, color, layerIndex)
}

fun View.applySimpleGradient(@ColorInt startColor: Int, @ColorInt endColor: Int) {
    background = GradientDrawable(
        GradientDrawable.Orientation.TL_BR, intArrayOf(startColor, endColor)
    ).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 1000f // Circular
    }
}

suspend fun Context.getSongArtwork(
    albumId: String?,
    path: String? = null,
    cachedPath: String? = null,
    album: String? = null,
    size: Int = 600
): Bitmap? {
    val model = getSongImageModel(albumId, path, cachedPath, album)
    val request =
        ImageRequest.Builder(this).data(model).size(size).precision(coil.size.Precision.INEXACT)
            .allowHardware(false).bitmapConfig(Bitmap.Config.ARGB_8888).build()

    val result = imageLoader.execute(request)
    return (result.drawable as? BitmapDrawable)?.bitmap
}

/* OK */
fun Context.extractPalette(data: Any, onPaletteGenerated: (Palette?) -> Unit) {
    val request = ImageRequest.Builder(this).data(data).allowHardware(enable = false)
        .size(200, 200) // Optimization: Downsample for palette extraction
        .listener(
            onError = { _, _ -> onPaletteGenerated(null) },
            onCancel = { onPaletteGenerated(null) },
        ).target { result ->
            val bitmap = (result as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    onPaletteGenerated(palette)
                }
            } else {
                onPaletteGenerated(null)
            }
        }.build()
    imageLoader.enqueue(request)
}

fun SeekBar.onProgressChanged(action: (progress: Int, fromUser: Boolean) -> Unit) {
    setOnSeekBarChangeListener(
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                action(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        },
    )
}

fun Context.showDialog(
    title: Int,
    message: Int,
    positiveButton: Int = android.R.string.ok,
) {
    val dialog = MaterialAlertDialogBuilder(this).setTitle(title).setMessage(message)
        .setPositiveButton(positiveButton, null).create()

    dialog.setCanceledOnTouchOutside(false)
    dialog.show()
}

/* OK */
@SuppressLint("DiscouragedApi")
fun Context.getLibraryColor(attrName: String): Int {
    var id = resources.getIdentifier(attrName, "attr", packageName)

    if (id == 0) {
        id = resources.getIdentifier(attrName, "attr", "android")
    }

    val fallback = if (attrName == "mc_track") ContextCompat.getColor(this, R.color.purple_500)
    else ContextCompat.getColor(this, R.color.purple_700)
    val color = if (id != 0) getColorFromAttr(id, Color.TRANSPARENT) else Color.TRANSPARENT

    if (color != Color.TRANSPARENT) return color

    return try {
        when (val theme = MultiColorManager.getCurrentTheme(this)) {
            is MultiColorTheme.Gradient -> {
                when (attrName) {
                    "mc_track" -> theme.colors.first()
                    "mc_tick" -> theme.colors.last()
                    else -> fallback
                }
            }

            is MultiColorTheme.Xml -> {
                val typedValue = TypedValue()
                val tempTheme = resources.newTheme()
                tempTheme.applyStyle(theme.styleRes, true)
                if (tempTheme.resolveAttribute(id, typedValue, true)) {
                    typedValue.data
                } else fallback
            }
        }
    } catch (_: Exception) {
        fallback
    }
}

fun <T> Flow<T>.collectWithLifecycle(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit,
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(state) {
            collect { action(it) }
        }
    }
}

interface PlaybackAnimatable {
    fun updatePlaybackState(path: String?, isPlaying: Boolean)
    fun updateThemeState(mode: Int, color: Int, colorSecond: Int)
    fun updateListThemeState(enabled: Boolean)
}

@UnstableApi
fun Context.openPlayer(position: Int, transitionView: View? = null) {
    val options = transitionView?.let {
        ActivityOptionsCompat.makeSceneTransitionAnimation(
            this.getAppCompatActivity() ?: return@let null,
            it,
            "song_image",
        ).toBundle()
    }
    launchActivity<PlayerActivity>(options) {
        putExtra(DATA.POSITION, position)
    }
}

fun LifecycleOwner.observePlaybackSync(
    nowPlayerViewModel: NowPlayerViewModel,
    viewBindingRoot: View? = null,
    adapterProvider: () -> PlaybackAnimatable?,
) {
    val sync = {
        adapterProvider()?.let { adapter ->
            val context = if (this is Context) this else viewBindingRoot?.context ?: return@let
            val trackColor = context.getLibraryColor("mc_track")
            val tickColor = context.getLibraryColor("mc_tick")
            val song = nowPlayerViewModel.currentPlayingSong.value
            viewBindingRoot?.findViewById<View>(R.id.fragBottomPlayer)?.isVisible(song != null)
            adapter.updatePlaybackState(song?.path, nowPlayerViewModel.isPlaying.value)

            val colors = nowPlayerViewModel.currentThemeColor.value ?: Pair(trackColor, tickColor)
            adapter.updateThemeState(
                nowPlayerViewModel.themeColorMode.value, colors.first, colors.second
            )
            adapter.updateListThemeState(nowPlayerViewModel.listItemThemeEnabled.value)
        }
    }

    // Initial sync
    sync()

    nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
        viewBindingRoot?.findViewById<View>(R.id.fragBottomPlayer)?.isVisible(song != null)
        adapterProvider()?.updatePlaybackState(song?.path, nowPlayerViewModel.isPlaying.value)
    }
    nowPlayerViewModel.isPlaying.collectWithLifecycle(this) { isPlaying ->
        adapterProvider()?.updatePlaybackState(
            nowPlayerViewModel.currentPlayingSong.value?.path,
            isPlaying,
        )
    }
    nowPlayerViewModel.themeColorMode.collectWithLifecycle(this) { mode ->
        val context =
            if (this is Context) this else viewBindingRoot?.context ?: return@collectWithLifecycle
        val trackColor = context.getLibraryColor("mc_track")
        val tickColor = context.getLibraryColor("mc_tick")
        val colors = nowPlayerViewModel.currentThemeColor.value ?: Pair(trackColor, tickColor)
        adapterProvider()?.updateThemeState(
            mode, colors.first, colors.second
        )
    }
    nowPlayerViewModel.currentThemeColor.collectWithLifecycle(this) { colorPair ->
        val context =
            if (this is Context) this else viewBindingRoot?.context ?: return@collectWithLifecycle
        val trackColor = context.getLibraryColor("mc_track")
        val tickColor = context.getLibraryColor("mc_tick")
        val colors = colorPair ?: Pair(trackColor, tickColor)
        adapterProvider()?.updateThemeState(
            nowPlayerViewModel.themeColorMode.value, colors.first, colors.second
        )
    }
    nowPlayerViewModel.listItemThemeEnabled.collectWithLifecycle(this) { enabled ->
        adapterProvider()?.updateListThemeState(enabled)
    }
}

fun PlaybackAnimatable.bindToPlaybackSync(
    lifecycleOwner: LifecycleOwner,
    nowPlayerViewModel: NowPlayerViewModel,
    viewBindingRoot: View? = null
) {
    lifecycleOwner.observePlaybackSync(nowPlayerViewModel, viewBindingRoot) { this }
}

fun AppCompatActivity.initToolbar(title: String? = null) {
    findViewById<MaterialToolbar>(R.id.customToolbar)?.apply {
        title?.let { this.title = it }
        setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}

fun Duration.formatAsTime(): String {
    val totalSeconds = this.inWholeSeconds
    val seconds = (totalSeconds % 60).toString()
    val minutes = (totalSeconds / 60).toString()
    return if (seconds.length == 1) "$minutes:0$seconds" else "$minutes:$seconds"
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun ImageView.loadBitmap(
    bitmap: Bitmap?,
    blurRadius: Float = 0f,
    fallback: Int = R.drawable.ic_cover_song,
    crossfade: Boolean = true,
    onComplete: (() -> Unit)? = null
) {
    if (bitmap == null) {
        load(fallback) {
            crossfade(crossfade)
            listener(
                onSuccess = { _, _ -> onComplete?.invoke() },
                onError = { _, _ -> onComplete?.invoke() })
        }
        return
    }

    load(bitmap) {
        crossfade(crossfade)
        if (blurRadius > 0) {
            size(200)
            transformations(SimpleBlurTransformation(blurRadius))
        }
        listener(
            onSuccess = { _, _ -> onComplete?.invoke() },
            onError = { _, _ -> onComplete?.invoke() })
    }
}

fun View?.isVisible(show: Boolean) {
    this?.visibility = if (show) View.VISIBLE else View.GONE
}

fun View.snackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}

fun ImageView.loadSongImage(
    albumId: String?,
    path: String? = null,
    cachedPath: String? = null,
    album: String? = null,
    fallback: Int = R.drawable.ic_cover_song,
    onComplete: (() -> Unit)? = null,
) {
    val model = getSongImageModel(albumId, path, cachedPath, album, fallback)

    load(model) {
        crossfade(enable = true)
        placeholder(R.color.image_profile)
        error(fallback)
        listener(
            onSuccess = { _, _ -> onComplete?.invoke() },
            onError = { _, _ -> onComplete?.invoke() },
        )
    }
}

fun ImageView.loadSongImageByPath(
    path: String?, cachedPath: String? = null, fallback: Int = R.drawable.ic_cover_song
) {
    val model: Any = if (!cachedPath.isNullOrEmpty()) {
        File(cachedPath)
    } else if (!path.isNullOrEmpty()) {
        getAlbumArtBytes(path) ?: fallback
    } else {
        fallback
    }
    load(model) {
        crossfade(true)
        placeholder(R.color.image_profile)
        error(fallback)
    }
}

fun ImageView.loadSongImageBlur(
    albumId: String?,
    level: Int,
    path: String? = null,
    cachedPath: String? = null,
    album: String? = null,
    fallback: Int = R.drawable.ic_cover_song,
) {
    val model = getSongImageModel(albumId, path, cachedPath, album, fallback)
    val actualFallback =
        if (fallback == R.drawable.ic_cover_song) R.drawable.ic_cover_song_blur else fallback

    load(model) {
        crossfade(enable = true)
        placeholder(R.color.image_profile)
        error(actualFallback)
        allowHardware(enable = false)
        size(80, 80) // High optimization: Tiny size for blur
        if ((model is Int) && (model == R.drawable.ic_cover_song)) {
            target { _ ->
                this@loadSongImageBlur.load(R.drawable.ic_cover_song_blur)
            }
        }
        transformations(SimpleBlurTransformation(level.toFloat()))
    }
}

fun ImageView.loadCachedAlbumImage(cachedPath: String?) {
    load(if (!cachedPath.isNullOrEmpty()) File(cachedPath) else R.color.image_profile) {
        scale(Scale.FIT)
        crossfade(true)
        placeholder(R.color.image_profile)
        error(R.color.image_profile)
    }
}

fun getSongImageModel(
    albumId: String?,
    path: String? = null,
    cachedPath: String? = null,
    album: String? = null,
    fallback: Int = R.drawable.ic_cover_song,
): Any {
    if (!cachedPath.isNullOrEmpty()) return File(cachedPath)

    val isUnknownAlbum = album == null || album == DATA.UNKNOWN
    if (!isUnknownAlbum && (!albumId.isNullOrEmpty()) && (albumId != "-1") && (albumId != "0")) {
        return ContentUris.withAppendedId(
            "content://media/external/audio/albumart".toUri(),
            albumId.toLong(),
        )
    }

    if (!isUnknownAlbum && !path.isNullOrEmpty()) {
        try {
            val file = File(path)
            val parent = file.parentFile
            if (parent != null && parent.exists()) {
                val imageFile = parent.listFiles { _, name ->
                    val n = name.lowercase()
                    n == "cover.jpg" || n == "cover.png" || n == "folder.jpg" || n == "album.jpg" || n == "albumart.jpg"
                }?.firstOrNull()
                if (imageFile != null) return imageFile
            }
        } catch (_: Exception) {
        }
    }

    if (!path.isNullOrEmpty()) {
        val file = File(path)
        if (file.exists()) return file
    }

    return fallback
}

class AudioArtFetcher(private val file: File, private val context: Context) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val art = getAlbumArtBytes(file.absolutePath) ?: return null
        return SourceResult(
            source = ImageSource(
                source = ByteArrayInputStream(art).source().buffer(), context = context
            ), mimeType = "image/jpeg", dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<File> {
        override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher? {
            val ext = data.extension.lowercase()
            if (ext == "mp3" || ext == "m4a" || ext == "wav" || ext == "flac" || ext == "ogg") {
                return AudioArtFetcher(data, context)
            }
            return null
        }
    }
}

fun Player.togglePlayPause(
    button: ImageView,
    onPause: () -> Unit,
    onStart: () -> Unit,
) {
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

fun Fragment.requestDeletion(
    uri: Uri,
    launcher: ActivityLauncher,
    onSuccess: () -> Unit,
) {
    val resolver: ContentResolver = requireContext().contentResolver
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
            val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            launcher.launch(request)
        } else {
            resolver.delete(uri, null, null)
            onSuccess()
        }
    } catch (e: Exception) {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && (e is RecoverableSecurityException)) {
            val request =
                IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
            launcher.launch(request)
        } else {
            view?.snackbar("Error: ${e.message}")
        }
    }
}

typealias ActivityLauncher = ActivityResultLauncher<IntentSenderRequest>

fun View.showKeyboard() {
    requestFocus()
    val activity = context.getAppCompatActivity()
    if (activity != null) {
        WindowCompat.getInsetsController(activity.window, this).show(WindowInsetsCompat.Type.ime())
    } else {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, 0)
    }
}

/* OK */
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

/* OK */
fun getDefaultArtBytes(context: Context): ByteArray? {
    return try {
        val size = 512
        val bitmap = createBitmap(size, size)
        val canvas = android.graphics.Canvas(bitmap)

        // The context (MusicService) already has MultiColorManager.applyTheme(this) called
        val backgroundColor = context.getLibraryColor("mc_track")
        canvas.drawColor(backgroundColor)

        val drawable = AppCompatResources.getDrawable(
            context,
            R.drawable.ic_default_album_art,
        )
        if (drawable != null) {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }

        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArray = stream.toByteArray()
        stream.close()
        bitmap.recycle()
        byteArray
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/* OK */
/* OK */
internal fun Int.toMiddleColor(): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(this, hsv)
    hsv[1] = hsv[1].coerceIn(0.5f, 0.9f) // Saturation
    hsv[2] = hsv[2].coerceIn(0.6f, 0.85f) // Value/Brightness
    return Color.HSVToColor(hsv)
}

fun Palette?.extractDynamicColors(defaultStart: Int, defaultEnd: Int): Pair<Int, Int> {
    val start = this?.getVibrantColor(Color.TRANSPARENT).takeIf { it != Color.TRANSPARENT }
        ?: this?.getLightVibrantColor(Color.TRANSPARENT).takeIf { it != Color.TRANSPARENT }
        ?: this?.getDominantColor(Color.TRANSPARENT).takeIf { it != Color.TRANSPARENT }
        ?: defaultStart

    val end = this?.getMutedColor(Color.TRANSPARENT).takeIf { it != Color.TRANSPARENT }
        ?: this?.getDarkVibrantColor(Color.TRANSPARENT).takeIf { it != Color.TRANSPARENT }
        ?: this?.getDominantColor(Color.TRANSPARENT).takeIf { it != Color.TRANSPARENT }
        ?: defaultEnd

    return Pair(start.toMiddleColor(), end.toMiddleColor())
}

fun Context.getCurrentThemeColors(mode: Int, paletteColors: Pair<Int, Int>?): Pair<Int, Int> {
    val track = getLibraryColor("mc_track")
    val tick = getLibraryColor("mc_tick")

    return when (mode) {
        DATA.MODE_PALETTE -> {
            val start = paletteColors?.first ?: track
            val end = paletteColors?.second ?: tick
            Pair(start, end)
        }

        DATA.MODE_WHITE -> Pair(Color.WHITE, Color.WHITE)
        else -> Pair(track, tick)
    }
}

fun Context.getAppCompatActivity(): AppCompatActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is AppCompatActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
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
        val output = createBitmap(w, h)
        output.setPixels(pix, 0, w, 0, 0, w, h)
        return output.scale(input.width, input.height, true)
    }
}