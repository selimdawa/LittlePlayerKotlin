package com.flatcode.littleplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.flatcode.littleplayer.utils.SongArtMapper
import dagger.hilt.android.HiltAndroidApp
import io.selimdawa.multicolors.MultiColorManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class Application : Application(), ImageLoaderFactory {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate() {
        super.onCreate()

        MainScope().launch {
            val darkModeKey = intPreferencesKey("dark_mode_preference")
            val mode =
                dataStore.data.map { it[darkModeKey] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
                    .first()
            AppCompatDelegate.setDefaultNightMode(mode)
        }

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
        }.components {
            add(SongArtMapper())
        }.crossfade(true).build()
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "music_playback_channel"
    }
}