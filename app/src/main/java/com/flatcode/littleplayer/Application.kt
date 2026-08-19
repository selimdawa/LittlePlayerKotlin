package com.flatcode.littleplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.flatcode.littleplayer.utils.AudioArtFetcher
import com.flatcode.littleplayer.utils.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import io.selimdawa.multicolors.MultiColorManager
import javax.inject.Inject

@HiltAndroidApp
class Application : Application(), ImageLoaderFactory {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate() {
        ThemeManager.init(this)
        super.onCreate()

        ThemeManager.init(dataStore)
        MultiColorManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.apply {
                createNotificationChannel(
                    NotificationChannel(
                        PLAYBACK_CHANNEL_ID,
                        getString(R.string.playback_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = getString(R.string.playback_channel_desc) })
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this).components {
            add(AudioArtFetcher.Factory(this@Application))
        }.memoryCache {
            MemoryCache.Builder(this).maxSizePercent(0.25).build()
        }.diskCache {
            DiskCache.Builder().directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(50L * 1024 * 1024).build()
        }.crossfade(true).allowRgb565(true) // Optimize memory by using RGB_565 for images
            .build()
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "music_playback_channel"
    }
}