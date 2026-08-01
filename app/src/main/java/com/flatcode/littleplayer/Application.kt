package com.flatcode.littleplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import io.selimdawa.multicolors.MultiColorManager
import io.selimdawa.multicolors.MultiColorTheme
import io.selimdawa.multicolors.ThemeRegistry

@HiltAndroidApp
class Application : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        //Test
        /*
        ThemeRegistry.register(MultiColorTheme.Dynamic("BLUE", "Ocean Blue", Color.BLUE))
        ThemeRegistry.register(MultiColorTheme.Dynamic("RED", "Flaming Red", Color.RED))
        ThemeRegistry.register(MultiColorTheme.Dynamic("GREEN", "Nature Green", Color.GREEN))
        ThemeRegistry.register(MultiColorTheme.Dynamic("PURPLE", "Royal Purple", Color.parseColor("#9C27B0")))
        ThemeRegistry.register(MultiColorTheme.Dynamic("ORANGE", "Sunset Orange", Color.parseColor("#FF9800")))
        ThemeRegistry.register(MultiColorTheme.Dynamic("TEAL", "Teal Breeze", Color.parseColor("#009688")))
        ThemeRegistry.register(MultiColorTheme.Dynamic("DARK_GRAY", "Midnight Gray", Color.DKGRAY))
*/

        MultiColorManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.apply {
                createNotificationChannel(
                    NotificationChannel(
                        PLAYBACK_CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "Ongoing music playback notification" })
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this).memoryCache {
            MemoryCache.Builder(this).maxSizePercent(0.25).build()
        }.diskCache {
            DiskCache.Builder().directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(50L * 1024 * 1024).build()
        }.crossfade(true).build()
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "music_playback_channel"
    }
}