package com.flatcode.littleplayer.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    private val sortingKey = stringPreferencesKey(DATA.SORTING)

    val sortOrderFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[sortingKey] ?: DATA.SORT_BY_DATE
    }

    suspend fun getAllAudio(): ArrayList<MusicFiles> = withContext(Dispatchers.IO) {
        val sortOrder = sortOrderFlow.first()
        val tempAudioList = ArrayList<MusicFiles>()

        val order = when (sortOrder) {
            DATA.SORT_BY_NAME -> MediaStore.MediaColumns.DISPLAY_NAME + " ASC"
            DATA.SORT_BY_DATE -> MediaStore.MediaColumns.DATE_ADDED + " DESC"
            DATA.SORT_BY_SIZE -> MediaStore.MediaColumns.SIZE + " DESC"
            DATA.SORT_BY_RELEASE_DATE -> MediaStore.Audio.Media.YEAR + " DESC"
            DATA.SORT_BY_PLAY_COUNT -> MediaStore.Audio.Media.DATE_MODIFIED + " DESC" // Placeholder
            else -> MediaStore.MediaColumns.DISPLAY_NAME + " ASC"
        }

        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, order)
        cursor?.use {
            while (it.moveToNext()) {
                val album = it.getString(0) ?: "Unknown"
                val title = it.getString(1) ?: "Unknown"
                val duration = it.getString(2) ?: "0"
                val path = it.getString(3) ?: ""
                val artist = it.getString(4) ?: "Unknown"
                val id = it.getString(5) ?: ""

                tempAudioList.add(MusicFiles(path, title, artist, album, duration, id))
            }
        }
        tempAudioList
    }

    suspend fun saveSortOrder(sortType: String) {
        dataStore.edit { preferences ->
            preferences[sortingKey] = sortType
        }
    }
}