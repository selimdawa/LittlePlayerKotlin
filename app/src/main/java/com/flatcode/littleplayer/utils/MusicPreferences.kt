package com.flatcode.littleplayer.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "music_prefs")

class MusicPreferences(private val context: Context) {
    companion object {
        val MUSIC_FILE_KEY = stringPreferencesKey("STORED_MUSIC")
        val ARTIST_NAME_KEY = stringPreferencesKey("ARTIST NAME")
        val SONG_NAME_KEY = stringPreferencesKey("SONG NAME")
    }

    val data: Flow<Preferences> = context.dataStore.data
    suspend fun saveLastPlayedSong(path: String, artist: String?, title: String?) {
        context.dataStore.edit { prefs ->
            prefs[MUSIC_FILE_KEY] = path
            prefs[ARTIST_NAME_KEY] = artist ?: "Unknown Artist"
            prefs[SONG_NAME_KEY] = title ?: "Unknown Title"
        }
    }
}