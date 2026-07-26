package com.flatcode.littleplayer.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flatcode.littleplayer.data.dao.SongDao
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val songDao: SongDao
) {
    private val sortingKey = stringPreferencesKey(DATA.SORTING)

    private val _currentPlaylist = MutableStateFlow<List<MusicFiles>>(emptyList())
    val currentPlaylist: StateFlow<List<MusicFiles>> = _currentPlaylist.asStateFlow()

    val sortOrderFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[sortingKey] ?: DATA.SORT_BY_DATE
    }

    suspend fun getAllAudio(): ArrayList<MusicFiles> = withContext(Dispatchers.IO) {
        val sortOrder = sortOrderFlow.first()

        val mediaStoreSortOrder = when (sortOrder) {
            DATA.SORT_BY_NAME -> MediaStore.Audio.Media.TITLE + " ASC"
            DATA.SORT_BY_DATE -> MediaStore.Audio.Media.DATE_ADDED + " DESC"
            DATA.SORT_BY_SIZE -> MediaStore.Audio.Media.SIZE + " DESC"
            DATA.SORT_BY_RELEASE_DATE -> MediaStore.Audio.Media.YEAR + " DESC"
            else -> MediaStore.Audio.Media.TITLE + " ASC"
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

        val tempAudioList = ArrayList<MusicFiles>()
        val dbSongs = try {
            songDao.getAllSongsSync().associateBy { it.id }
        } catch (_: Exception) {
            emptyMap()
        }

        val cursor: Cursor? =
            context.contentResolver.query(uri, projection, null, null, mediaStoreSortOrder)
        cursor?.use {
            while (it.moveToNext()) {
                val album = it.getString(0) ?: "Unknown"
                val title = it.getString(1) ?: "Unknown"
                val duration = it.getLong(2)
                val path = it.getString(3) ?: ""
                val artist = it.getString(4) ?: "Unknown"
                val id = it.getString(5) ?: ""
                val albumId = it.getString(6) ?: ""

                val dbSong = dbSongs[id]

                tempAudioList.add(
                    MusicFiles(
                        path = path,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration.toString(),
                        id = id,
                        albumId = albumId,
                        waveform = dbSong?.waveform,
                        playCount = dbSong?.playCount ?: 0
                    )
                )
            }
        }

        if (sortOrder == DATA.SORT_BY_PLAY_COUNT) {
            tempAudioList.sortByDescending { it.playCount }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncWithMediaStore()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        tempAudioList
    }

    private suspend fun syncWithMediaStore() {
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

        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        val songEntities = mutableListOf<com.flatcode.littleplayer.data.entity.SongEntity>()
        cursor?.use {
            while (it.moveToNext()) {
                val album = it.getString(0) ?: "Unknown"
                val title = it.getString(1) ?: "Unknown"
                val duration = it.getLong(2)
                val path = it.getString(3) ?: ""
                val artist = it.getString(4) ?: "Unknown"
                val id = it.getString(5) ?: ""
                val albumId = it.getString(6) ?: ""

                songEntities.add(
                    com.flatcode.littleplayer.data.entity.SongEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        path = path,
                        albumId = albumId
                    )
                )
            }
        }
        if (songEntities.isNotEmpty()) {
            songDao.insertSongs(songEntities)
        }
    }

    suspend fun saveSortOrder(sortType: String) {
        dataStore.edit { preferences ->
            preferences[sortingKey] = sortType
        }
    }

    suspend fun updateWaveform(songId: String, waveform: String) = withContext(Dispatchers.IO) {
        songDao.updateWaveform(songId, waveform)
    }

    fun updateCurrentPlaylist(songs: List<MusicFiles>) {
        _currentPlaylist.value = songs
    }
}