package com.flatcode.littleplayer.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "music_prefs")

class MusicPreferences(private val context: Context) {

    companion object {
        val MUSIC_FILE_KEY = stringPreferencesKey("stored_music")
        val ARTIST_NAME_KEY = stringPreferencesKey("artist_name")
        val SONG_NAME_KEY = stringPreferencesKey("song_name")
    }

    val musicFileFlow: Flow<String?> = context.dataStore.data.map { it[MUSIC_FILE_KEY] }
    val artistNameFlow: Flow<String?> = context.dataStore.data.map { it[ARTIST_NAME_KEY] }
    val songNameFlow: Flow<String?> = context.dataStore.data.map { it[SONG_NAME_KEY] }

    suspend fun saveLastPlayedSong(path: String, artist: String?, title: String?) {
        context.dataStore.edit { preferences ->
            preferences[MUSIC_FILE_KEY] = path
            preferences[ARTIST_NAME_KEY] = artist ?: "Unknown Artist"
            preferences[SONG_NAME_KEY] = title ?: "Unknown Title"
        }
    }
}