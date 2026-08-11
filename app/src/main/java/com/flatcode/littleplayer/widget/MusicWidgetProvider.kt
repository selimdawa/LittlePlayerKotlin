package com.flatcode.littleplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.service.MusicService

@UnstableApi
class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == MusicService.ACTION_UPDATE_WIDGET) {
            val title = intent.getStringExtra("title") ?: context.getString(R.string.song_title)
            val artist = intent.getStringExtra("artist") ?: context.getString(R.string.artist_name)
            val isPlaying = intent.getBooleanExtra("isPlaying", false)
            val imagePath = intent.getStringExtra("imagePath")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val smallWidget = ComponentName(context, MusicWidgetProvider::class.java)
            val largeWidget = ComponentName(context, MusicWidgetProviderLarge::class.java)

            updateWidgetUI(context, appWidgetManager, smallWidget, R.layout.music_widget_small, title, artist, isPlaying, imagePath)
            updateWidgetUI(context, appWidgetManager, largeWidget, R.layout.music_widget_large, title, artist, isPlaying, imagePath)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Initial update with default values
        val views = RemoteViews(context.packageName, R.layout.music_widget_small)
        setupButtons(context, views)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun setupButtons(context: Context, views: RemoteViews) {
            val playPauseIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY_PAUSE
            }
            val nextIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_NEXT
            }
            val prevIntent = Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_PREV
            }
            val openAppIntent = Intent(context, PlayerActivity::class.java)

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            views.setOnClickPendingIntent(R.id.widgetPlayPause, PendingIntent.getService(context, 0, playPauseIntent, flags))
            views.setOnClickPendingIntent(R.id.widgetNext, PendingIntent.getService(context, 1, nextIntent, flags))
            views.setOnClickPendingIntent(R.id.widgetPrev, PendingIntent.getService(context, 2, prevIntent, flags))
            views.setOnClickPendingIntent(R.id.widgetAlbumArt, PendingIntent.getActivity(context, 3, openAppIntent, flags))
        }

        fun updateWidgetUI(
            context: Context,
            appWidgetManager: AppWidgetManager,
            componentName: ComponentName,
            layoutId: Int,
            title: String,
            artist: String,
            isPlaying: Boolean,
            imagePath: String?
        ) {
            val views = RemoteViews(context.packageName, layoutId)
            views.setTextViewText(R.id.widgetTitle, title)
            views.setTextViewText(R.id.widgetArtist, artist)
            views.setImageViewResource(R.id.widgetPlayPause, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

            if (imagePath != null) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widgetAlbumArt, bitmap)
                } else {
                    views.setImageViewResource(R.id.widgetAlbumArt, R.drawable.ic_music)
                }
            } else {
                views.setImageViewResource(R.id.widgetAlbumArt, R.drawable.ic_music)
            }

            setupButtons(context, views)
            appWidgetManager.updateAppWidget(componentName, views)
        }
    }
}

@UnstableApi
class MusicWidgetProviderLarge : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.music_widget_large)
            MusicWidgetProvider.setupButtons(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}