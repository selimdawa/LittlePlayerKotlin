package com.flatcode.littleplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.service.MusicService
import java.io.File

@UnstableApi
open class MusicWidgetBase : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val layoutId = getLayoutId()
            val views = RemoteViews(context.packageName, layoutId)
            MusicWidgetUtils.setupButtons(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == MusicService.ACTION_UPDATE_WIDGET) {
            val title = intent.getStringExtra("title") ?: context.getString(R.string.song_title)
            val artist = intent.getStringExtra("artist") ?: context.getString(R.string.artist_name)
            val isPlaying = intent.getBooleanExtra("isPlaying", false)
            val isShuffle = intent.getBooleanExtra("isShuffle", false)
            val isFavorite = intent.getBooleanExtra("isFavorite", false)
            val imagePath = intent.getStringExtra("imagePath")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            val widgets = listOf(
                MusicWidget2x1::class.java to R.layout.layout_widget_2x1,
                MusicWidget2x2::class.java to R.layout.layout_widget_2x2,
                MusicWidget4x1::class.java to R.layout.layout_widget_4x1,
                MusicWidget4x2::class.java to R.layout.layout_widget_4x2,
                MusicWidget4x4::class.java to R.layout.layout_widget_4x4
            )

            widgets.forEach { (clazz, layoutId) ->
                MusicWidgetUtils.updateWidgetUI(
                    context, appWidgetManager, ComponentName(context, clazz),
                    layoutId, title, artist, isPlaying, isShuffle, isFavorite, imagePath
                )
            }
        }
    }

    open fun getLayoutId(): Int = R.layout.layout_widget_4x2
}

@UnstableApi class MusicWidget2x1 : MusicWidgetBase() { override fun getLayoutId() = R.layout.layout_widget_2x1 }
@UnstableApi class MusicWidget2x2 : MusicWidgetBase() { override fun getLayoutId() = R.layout.layout_widget_2x2 }
@UnstableApi class MusicWidget4x1 : MusicWidgetBase() { override fun getLayoutId() = R.layout.layout_widget_4x1 }
@UnstableApi class MusicWidget4x2 : MusicWidgetBase() { override fun getLayoutId() = R.layout.layout_widget_4x2 }
@UnstableApi class MusicWidget4x4 : MusicWidgetBase() { override fun getLayoutId() = R.layout.layout_widget_4x4 }

@UnstableApi
object MusicWidgetUtils {
    fun setupButtons(context: Context, views: RemoteViews) {
        val playPauseIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_PLAY_PAUSE }
        val nextIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_NEXT }
        val prevIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_PREV }
        val shuffleIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_SHUFFLE }
        val favoriteIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_FAVORITE }
        val openAppIntent = Intent(context, PlayerActivity::class.java)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Safety check for IDs that might not exist in all layouts
        views.setOnClickPendingIntent(R.id.widgetPlayPause, PendingIntent.getService(context, 0, playPauseIntent, flags))
        views.setOnClickPendingIntent(R.id.widgetNext, PendingIntent.getService(context, 1, nextIntent, flags))
        views.setOnClickPendingIntent(R.id.widgetPrev, PendingIntent.getService(context, 2, prevIntent, flags))
        views.setOnClickPendingIntent(R.id.widgetShuffle, PendingIntent.getService(context, 3, shuffleIntent, flags))
        views.setOnClickPendingIntent(R.id.widgetFavorite, PendingIntent.getService(context, 4, favoriteIntent, flags))
        
        // Background click to open app
        views.setOnClickPendingIntent(R.id.widgetBackground, PendingIntent.getActivity(context, 5, openAppIntent, flags))
    }

    fun updateWidgetUI(
        context: Context, appWidgetManager: AppWidgetManager, componentName: ComponentName, layoutId: Int,
        title: String, artist: String, isPlaying: Boolean, isShuffle: Boolean, isFavorite: Boolean, imagePath: String?
    ) {
        val views = RemoteViews(context.packageName, layoutId)
        views.setTextViewText(R.id.widgetTitle, title)
        views.setTextViewText(R.id.widgetArtist, artist)
        views.setImageViewResource(R.id.widgetPlayPause, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        
        val activeColor = ContextCompat.getColor(context, R.color.syria_red)
        
        // Update Shuffle and Favorite if they exist
        try {
            views.setInt(R.id.widgetShuffle, "setColorFilter", if (isShuffle) activeColor else Color.WHITE)
            views.setImageViewResource(R.id.widgetFavorite, if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            views.setInt(R.id.widgetFavorite, "setColorFilter", if (isFavorite) activeColor else Color.WHITE)
        } catch (_: Exception) {}

        // Handle Album Art with scaling
        if (imagePath != null) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(imagePath, options)
                options.inSampleSize = calculateInSampleSize(options, 512, 512)
                options.inJustDecodeBounds = false
                val bitmap = BitmapFactory.decodeFile(imagePath, options)
                
                if (bitmap != null) {
                    val roundedBitmap = getRoundedCornerBitmap(bitmap, 40f) // 40px radius for art
                    views.setImageViewBitmap(R.id.widgetArtSmall, roundedBitmap)
                    views.setImageViewBitmap(R.id.widgetArtLarge, roundedBitmap)
                }
            } catch (_: Exception) {
            }
        }

        // Ensure background is always the glass shape
        views.setImageViewResource(R.id.widgetBackground, R.drawable.widget_bg_shape)

        setupButtons(context, views)
        appWidgetManager.updateAppWidget(componentName, views)
    }

    private fun getRoundedCornerBitmap(bitmap: android.graphics.Bitmap, pixels: Float): android.graphics.Bitmap {
        val output = android.graphics.Bitmap.createBitmap(bitmap.width, bitmap.height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val color = -0xbdbdbe
        val paint = android.graphics.Paint()
        val rect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = android.graphics.RectF(rect)
        val roundPx = pixels

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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
