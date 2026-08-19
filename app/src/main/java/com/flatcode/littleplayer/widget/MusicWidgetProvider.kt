package com.flatcode.littleplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
open class MusicWidgetBase : AppWidgetProvider() {

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val layoutId = getLayoutId()
            val views = RemoteViews(context.packageName, layoutId)
            MusicWidgetUtils.setupButtons(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MusicService.ACTION_UPDATE_WIDGET) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

            val title = intent.getStringExtra("title") ?: context.getString(R.string.song_title)
            val artist = intent.getStringExtra("artist") ?: context.getString(R.string.artist_name)
            val isPlaying = intent.getBooleanExtra("isPlaying", false)
            val isShuffle = intent.getBooleanExtra("isShuffle", false)
            val isFavorite = intent.getBooleanExtra("isFavorite", false)
            val imagePath = intent.getStringExtra("imagePath")

            scope.launch {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)

                    // Process bitmap once on IO thread
                    val processedBitmap = withContext(Dispatchers.IO) {
                        MusicWidgetUtils.loadAndProcessBitmap(imagePath)
                    }

                    val widgets = listOf(
                        MusicWidget2x1::class.java to R.layout.layout_widget_2x1,
                        MusicWidget2x2::class.java to R.layout.layout_widget_2x2,
                        MusicWidget4x1::class.java to R.layout.layout_widget_4x1,
                        MusicWidget4x2::class.java to R.layout.layout_widget_4x2,
                        MusicWidget4x4::class.java to R.layout.layout_widget_4x4
                    )

                    widgets.forEach { (clazz, layoutId) ->
                        val component = ComponentName(context, clazz)
                        if (appWidgetManager.getAppWidgetIds(component).isNotEmpty()) {
                            MusicWidgetUtils.updateWidgetUI(
                                context,
                                appWidgetManager,
                                component,
                                layoutId,
                                title,
                                artist,
                                isPlaying,
                                isShuffle,
                                isFavorite,
                                processedBitmap
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    open fun getLayoutId(): Int = R.layout.layout_widget_4x2
}

@UnstableApi
class MusicWidget2x1 : MusicWidgetBase() {
    override fun getLayoutId() = R.layout.layout_widget_2x1
}

@UnstableApi
class MusicWidget2x2 : MusicWidgetBase() {
    override fun getLayoutId() = R.layout.layout_widget_2x2
}

@UnstableApi
class MusicWidget4x1 : MusicWidgetBase() {
    override fun getLayoutId() = R.layout.layout_widget_4x1
}

@UnstableApi
class MusicWidget4x2 : MusicWidgetBase() {
    override fun getLayoutId() = R.layout.layout_widget_4x2
}

@UnstableApi
class MusicWidget4x4 : MusicWidgetBase() {
    override fun getLayoutId() = R.layout.layout_widget_4x4
}

@UnstableApi
object MusicWidgetUtils {
    private var lastImagePath: String? = null
    private var cachedBitmap: Bitmap? = null

    fun loadAndProcessBitmap(imagePath: String?): Bitmap? {
        if (imagePath == null) {
            lastImagePath = null
            cachedBitmap = null
            return null
        }
        if (imagePath == lastImagePath && cachedBitmap != null) {
            return cachedBitmap
        }

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imagePath, options)
            options.inSampleSize = calculateInSampleSize(options, 512, 512)
            options.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeFile(imagePath, options)

            if (bitmap != null) {
                val radius = bitmap.width.coerceAtMost(bitmap.height) / 2f
                val rounded = getRoundedCornerBitmap(bitmap, radius)
                lastImagePath = imagePath
                cachedBitmap = rounded
                rounded
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setupButtons(context: Context, views: RemoteViews) {
        val playPauseIntent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_PLAY_PAUSE
        }
        val nextIntent =
            Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_NEXT }
        val prevIntent =
            Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_PREV }
        val shuffleIntent =
            Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_SHUFFLE }
        val favoriteIntent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_FAVORITE
        }
        val openAppIntent = Intent(context, PlayerActivity::class.java)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        views.setOnClickPendingIntent(
            R.id.widgetPlayPause, PendingIntent.getService(context, 0, playPauseIntent, flags)
        )
        views.setOnClickPendingIntent(
            R.id.widgetNext, PendingIntent.getService(context, 1, nextIntent, flags)
        )
        views.setOnClickPendingIntent(
            R.id.widgetPrev, PendingIntent.getService(context, 2, prevIntent, flags)
        )
        views.setOnClickPendingIntent(
            R.id.widgetShuffle, PendingIntent.getService(context, 3, shuffleIntent, flags)
        )
        views.setOnClickPendingIntent(
            R.id.widgetFavorite, PendingIntent.getService(context, 4, favoriteIntent, flags)
        )

        // Background click to open app
        views.setOnClickPendingIntent(
            R.id.widgetMainLayout, PendingIntent.getActivity(context, 5, openAppIntent, flags)
        )

        // Force white tint for control buttons
        try {
            views.setInt(R.id.widgetPlayPause, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widgetNext, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widgetPrev, "setColorFilter", Color.WHITE)
        } catch (_: Exception) {
        }
    }

    fun updateWidgetUI(
        context: Context,
        appWidgetManager: AppWidgetManager,
        componentName: ComponentName,
        layoutId: Int,
        title: String,
        artist: String,
        isPlaying: Boolean,
        isShuffle: Boolean,
        isFavorite: Boolean,
        processedBitmap: Bitmap?
    ) {
        val views = RemoteViews(context.packageName, layoutId)
        views.setTextViewText(R.id.widgetTitle, title)
        views.setTextViewText(R.id.widgetArtist, artist)
        views.setImageViewResource(
            R.id.widgetPlayPause, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        // Force white tint for control buttons
        try {
            views.setInt(R.id.widgetPlayPause, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widgetNext, "setColorFilter", Color.WHITE)
            views.setInt(R.id.widgetPrev, "setColorFilter", Color.WHITE)
        } catch (_: Exception) {
        }

        val activeColor = ContextCompat.getColor(context, R.color.syria_red)

        try {
            views.setInt(
                R.id.widgetShuffle, "setColorFilter", if (isShuffle) activeColor else Color.WHITE
            )
            views.setImageViewResource(
                R.id.widgetFavorite,
                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            views.setInt(
                R.id.widgetFavorite, "setColorFilter", if (isFavorite) activeColor else Color.WHITE
            )
        } catch (_: Exception) {
        }

        // Handle Album Art
        if (processedBitmap != null) {
            views.setImageViewBitmap(R.id.widgetArtSmall, processedBitmap)
            views.setImageViewBitmap(R.id.widgetArtLarge, processedBitmap)
        } else {
            // Back to circular placeholder
            views.setImageViewResource(R.id.widgetArtSmall, R.drawable.widget_art_placeholder)
            views.setImageViewResource(R.id.widgetArtLarge, R.drawable.widget_art_placeholder)
        }

        // Apply size-specific background
        val bgResource = when (layoutId) {
            R.layout.layout_widget_2x1, R.layout.layout_widget_4x1 -> R.drawable.widget_bg_small
            R.layout.layout_widget_2x2, R.layout.layout_widget_4x2 -> R.drawable.widget_bg_medium
            R.layout.layout_widget_4x4 -> R.drawable.widget_bg_large
            else -> R.drawable.widget_bg_medium
        }

        try {
            views.setInt(R.id.widgetMainLayout, "setBackgroundResource", bgResource)
        } catch (_: Exception) {
        }

        setupButtons(context, views)
        appWidgetManager.updateAppWidget(componentName, views)
    }

    private fun getRoundedCornerBitmap(
        bitmap: Bitmap, pixels: Float
    ): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = Bitmap.createBitmap(
            size, size, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)

        val paint = Paint()
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = Color.BLACK
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        // Center crop
        val srcRect = Rect(
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            (bitmap.width + size) / 2,
            (bitmap.height + size) / 2
        )
        canvas.drawBitmap(bitmap, srcRect, rect, paint)
        
        // Add a clean white stroke around the circle (1.5dp equivalent)
        paint.xfermode = null
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#33FFFFFF") // white_20
        paint.strokeWidth = 4f 
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 2f, paint)
        
        return output
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}