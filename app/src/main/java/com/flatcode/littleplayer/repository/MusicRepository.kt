package com.flatcode.littleplayer.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flatcode.littleplayer.data.dao.AlbumImageDao
import com.flatcode.littleplayer.data.dao.MusicDao
import com.flatcode.littleplayer.data.dao.SongDao
import com.flatcode.littleplayer.data.entity.AlbumImageEntity
import com.flatcode.littleplayer.data.entity.CurrentQueueEntity
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.getAlbumArtBytes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val songDao: SongDao,
    private val albumImageDao: AlbumImageDao,
    private val musicDao: MusicDao
) {
    private val sortingKey = stringPreferencesKey(DATA.SORTING)

    private val _currentPlaylist = MutableStateFlow<List<MusicFiles>>(emptyList())
    val currentPlaylist: StateFlow<List<MusicFiles>> = _currentPlaylist.asStateFlow()

    val sortOrderFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[sortingKey] ?: DATA.SORT_BY_DATE
    }.distinctUntilChanged()

    fun getSongsFlow(sortOrder: String): Flow<List<MusicFiles>> {
        return combine(
            songDao.getAllSongs(), albumImageDao.getAllAlbumImages()
        ) { dbSongs, cachedImages ->
            val sortedDbSongs = when (sortOrder) {
                DATA.SORT_BY_NAME -> dbSongs.sortedBy { it.title.lowercase() }
                DATA.SORT_BY_DATE -> dbSongs.sortedByDescending { it.dateAdded }
                DATA.SORT_BY_PLAY_COUNT -> dbSongs.sortedByDescending { it.playCount }
                DATA.SORT_BY_SIZE -> dbSongs.sortedByDescending { it.size }
                DATA.SORT_BY_RELEASE_DATE -> dbSongs.sortedByDescending { it.year }
                else -> dbSongs
            }

            val imageMap = cachedImages.associateBy { it.albumName }
            sortedDbSongs.map { dbSong ->
                MusicFiles(
                    path = dbSong.path,
                    title = dbSong.title,
                    artist = dbSong.artist,
                    album = dbSong.album,
                    duration = dbSong.duration.toString(),
                    id = dbSong.id,
                    albumId = dbSong.albumId,
                    waveform = dbSong.waveform,
                    playCount = dbSong.playCount,
                    cachedImagePath = imageMap[dbSong.album ?: DATA.UNKNOWN]?.imagePath,
                    dateAdded = dbSong.dateAdded,
                    size = dbSong.size,
                    year = dbSong.year
                )
            }
        }
    }

    suspend fun getAllAudio(sortOrder: String): ArrayList<MusicFiles> =
        withContext(Dispatchers.IO) {
            val tempAudioList = ArrayList<MusicFiles>()

            try {
                val dbSongs = songDao.getAllSongsSync()
                if (dbSongs.isNotEmpty()) {
                    val cachedImages = try {
                        albumImageDao.getAllAlbumImagesSync().associateBy { it.albumName }
                    } catch (_: Exception) {
                        emptyMap()
                    }

                    dbSongs.forEach { dbSong ->
                        tempAudioList.add(
                            MusicFiles(
                                path = dbSong.path,
                                title = dbSong.title,
                                artist = dbSong.artist,
                                album = dbSong.album,
                                duration = dbSong.duration.toString(),
                                id = dbSong.id,
                                albumId = dbSong.albumId,
                                waveform = dbSong.waveform,
                                playCount = dbSong.playCount,
                                cachedImagePath = cachedImages[dbSong.album
                                    ?: DATA.UNKNOWN]?.imagePath,
                                dateAdded = dbSong.dateAdded,
                                size = dbSong.size,
                                year = dbSong.year
                            )
                        )
                    }

                    when (sortOrder) {
                        DATA.SORT_BY_NAME -> tempAudioList.sortBy { it.title?.lowercase() }
                        DATA.SORT_BY_PLAY_COUNT -> tempAudioList.sortByDescending { it.playCount }
                        DATA.SORT_BY_DATE -> tempAudioList.sortByDescending { it.dateAdded }
                        DATA.SORT_BY_SIZE -> tempAudioList.sortByDescending { it.size }
                        DATA.SORT_BY_RELEASE_DATE -> tempAudioList.sortByDescending { it.year }
                    }

                    if (tempAudioList.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                syncWithMediaStore()
                            } catch (_: Exception) {
                            }
                        }
                        return@withContext tempAudioList
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
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
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.YEAR
                )

                context.contentResolver.query(uri, projection, null, null, mediaStoreSortOrder)
                    ?.use { cursor ->
                        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        val durationColumn =
                            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                        val artistColumn =
                            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val albumIdColumn =
                            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                        val dateAddedColumn =
                            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                        val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                        while (cursor.moveToNext()) {
                            tempAudioList.add(
                                MusicFiles(
                                    path = cursor.getString(pathColumn) ?: "",
                                    title = cursor.getString(titleColumn) ?: DATA.UNKNOWN,
                                    artist = cursor.getString(artistColumn) ?: DATA.UNKNOWN,
                                    album = cursor.getString(albumColumn) ?: DATA.UNKNOWN,
                                    duration = cursor.getLong(durationColumn).toString(),
                                    id = cursor.getString(idColumn) ?: "",
                                    albumId = cursor.getString(albumIdColumn) ?: "",
                                    dateAdded = cursor.getLong(dateAddedColumn),
                                    size = cursor.getLong(sizeColumn),
                                    year = cursor.getInt(yearColumn)
                                )
                            )
                        }
                    }

                if (tempAudioList.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            syncWithMediaStore()
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.YEAR
        )

        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        val songEntities = mutableListOf<com.flatcode.littleplayer.data.entity.SongEntity>()
        cursor?.use {
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val yearColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (it.moveToNext()) {
                val album = it.getString(albumColumn) ?: DATA.UNKNOWN
                val title = it.getString(titleColumn) ?: DATA.UNKNOWN
                val duration = it.getLong(durationColumn)
                val path = it.getString(pathColumn) ?: ""
                val artist = it.getString(artistColumn) ?: DATA.UNKNOWN
                val id = it.getString(idColumn) ?: ""
                val albumId = it.getString(albumIdColumn) ?: ""
                val dateAdded = it.getLong(dateAddedColumn)
                val size = it.getLong(sizeColumn)
                val year = it.getInt(yearColumn)

                songEntities.add(
                    com.flatcode.littleplayer.data.entity.SongEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        path = path,
                        albumId = albumId,
                        dateAdded = dateAdded,
                        size = size,
                        year = year
                    )
                )
            }
        }
        if (songEntities.isNotEmpty()) {
            songDao.insertSongs(songEntities)
        }
    }

    suspend fun cacheAlbumArt(song: MusicFiles) = withContext(Dispatchers.IO) {
        val albumName = song.album ?: DATA.UNKNOWN
        if (albumName == DATA.UNKNOWN) return@withContext

        val existing = albumImageDao.getAlbumImageByName(albumName)
        if ((existing != null) && File(existing.imagePath).exists()) return@withContext

        val artBytes = getAlbumArtBytes(song.path) ?: return@withContext

        val folder = File(context.filesDir, "album_art")
        if (!folder.exists()) folder.mkdirs()

        val fileName = "${albumName.hashCode()}.jpg"
        val file = File(folder, fileName)

        try {
            FileOutputStream(file).use { out ->
                out.write(artBytes)
            }
            albumImageDao.insertAlbumImage(AlbumImageEntity(albumName, file.absolutePath))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun startBackgroundArtCaching(songs: List<MusicFiles>) = withContext(Dispatchers.IO) {
        val processedAlbums = mutableSetOf<String>()
        songs.forEach { song ->
            val album = song.album ?: DATA.UNKNOWN
            if (album != DATA.UNKNOWN && !processedAlbums.contains(album)) {
                cacheAlbumArt(song)
                processedAlbums.add(album)
            }
        }
    }


    suspend fun saveSortOrder(sortType: String) {
        dataStore.edit { preferences ->
            preferences[sortingKey] = sortType
        }
    }

    suspend fun deleteMusicFile(song: MusicFiles): Boolean = withContext(Dispatchers.IO) {
        val file = File(song.path ?: return@withContext false)
        val deleted = file.delete()
        if (deleted) {
            val contentUri = android.content.ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                (song.id ?: return@withContext false).toLong()
            )
            context.contentResolver.delete(contentUri, null, null)
            songDao.deleteSongById(song.id)
            true
        } else {
            false
        }
    }

    fun updateCurrentPlaylist(songs: List<MusicFiles>) {
        _currentPlaylist.value = songs
        CoroutineScope(Dispatchers.IO).launch {
            musicDao.clearQueue()
            val entities = songs.mapIndexed { index, song ->
                CurrentQueueEntity(
                    songId = song.id ?: "",
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    albumId = song.albumId,
                    duration = song.duration,
                    path = song.path,
                    cachedImagePath = song.cachedImagePath,
                    orderIndex = index
                )
            }
            musicDao.insertQueue(entities)
        }
    }

    suspend fun loadCurrentQueue(): List<MusicFiles> = withContext(Dispatchers.IO) {
        musicDao.getQueue().map {
            MusicFiles(
                id = it.songId,
                title = it.title,
                artist = it.artist,
                album = it.album,
                albumId = it.albumId,
                duration = it.duration,
                path = it.path,
                cachedImagePath = it.cachedImagePath
            )
        }
    }
}