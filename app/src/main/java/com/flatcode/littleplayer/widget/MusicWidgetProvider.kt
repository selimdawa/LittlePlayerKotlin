package com.flatcode.littleplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.service.MusicService

@UnstableApi
class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, R.layout.layout_widget_modern)
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
            
            // Update Full Version
            updateWidgetUI(context, appWidgetManager, ComponentName(context, MusicWidgetProvider::class.java), 
                R.layout.layout_widget_modern, title, artist, isPlaying, isShuffle, isFavorite, imagePath)
                
            // Update Compact Version
            updateWidgetUI(context, appWidgetManager, ComponentName(context, MusicWidgetProviderCompact::class.java), 
                R.layout.layout_widget_modern_compact, title, artist, isPlaying, isShuffle, isFavorite, imagePath)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, layoutId: Int) {
        val views = RemoteViews(context.packageName, layoutId)
        setupButtons(context, views)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun setupButtons(context: Context, views: RemoteViews) {
            val playPauseIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_PLAY_PAUSE }
            val nextIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_NEXT }
            val prevIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_PREV }
            val shuffleIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_SHUFFLE }
            val favoriteIntent = Intent(context, MusicService::class.java).apply { action = MusicService.ACTION_FAVORITE }
            val openAppIntent = Intent(context, PlayerActivity::class.java)

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            views.setOnClickPendingIntent(R.id.widgetPlayPause, PendingIntent.getService(context, 0, playPauseIntent, flags))
            views.setOnClickPendingIntent(R.id.widgetNext, PendingIntent.getService(context, 1, nextIntent, flags))
            views.setOnClickPendingIntent(R.id.widgetPrev, PendingIntent.getService(context, 2, prevIntent, flags))
            views.setOnClickPendingIntent(R.id.widgetShuffle, PendingIntent.getService(context, 3, shuffleIntent, flags))
            views.setOnClickPendingIntent(R.id.widgetFavorite, PendingIntent.getService(context, 4, favoriteIntent, flags))
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
            
            val activeColor = Color.parseColor("#CE1126")
            
            // Buttons that might exist only in Full layout
            try {
                views.setInt(R.id.widgetShuffle, "setColorFilter", if (isShuffle) activeColor else Color.WHITE)
                views.setImageViewResource(R.id.widgetFavorite, if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
                views.setInt(R.id.widgetFavorite, "setColorFilter", if (isFavorite) activeColor else Color.WHITE)
            } catch (e: Exception) {}

            if (imagePath != null) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) views.setImageViewBitmap(R.id.widgetBackground, bitmap)
                else views.setImageViewResource(R.id.widgetBackground, R.drawable.widget_bg_shape)
            } else {
                views.setImageViewResource(R.id.widgetBackground, R.drawable.widget_bg_shape)
            }

            setupButtons(context, views)
            appWidgetManager.updateAppWidget(componentName, views)
        }
    }
}

@UnstableApi
class MusicWidgetProviderCompact : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.layout_widget_modern_compact)
            MusicWidgetProvider.setupButtons(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}