package com.flatcode.littleplayer.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class MusicRepository @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val songDao: SongDao,
    private val albumImageDao: AlbumImageDao,
    private val musicDao: MusicDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            scheduleSync()
        }
    }

    private fun scheduleSync() {
        syncJob?.cancel()
        syncJob = repositoryScope.launch {
            delay(3.seconds)
            try {
                syncWithMediaStore()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
        )
    }

    private val _currentPlaylist = MutableStateFlow<List<MusicFiles>>(emptyList())
    val currentPlaylist: StateFlow<List<MusicFiles>> = _currentPlaylist.asStateFlow()

    fun getSortOrder(category: String): Flow<String> = dataStore.data.map { preferences ->
        preferences[stringPreferencesKey(category)]
            ?: if (category == DATA.SONGS) DATA.SORT_BY_DATE else DATA.SORT_BY_NAME
    }.distinctUntilChanged()

    suspend fun saveSortOrder(category: String, sortType: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(category)] = sortType
        }
    }

    val excludedFolders: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[stringSetPreferencesKey(DATA.EXCLUDED_FOLDERS)] ?: emptySet()
    }.distinctUntilChanged()

    suspend fun addExcludedFolder(path: String) {
        dataStore.edit { preferences ->
            val current = preferences[stringSetPreferencesKey(DATA.EXCLUDED_FOLDERS)] ?: emptySet()
            preferences[stringSetPreferencesKey(DATA.EXCLUDED_FOLDERS)] = current + path
        }
    }

    suspend fun removeExcludedFolder(path: String) {
        dataStore.edit { preferences ->
            val current = preferences[stringSetPreferencesKey(DATA.EXCLUDED_FOLDERS)] ?: emptySet()
            preferences[stringSetPreferencesKey(DATA.EXCLUDED_FOLDERS)] = current - path
        }
    }

    fun getSongsFlow(sortOrder: String): Flow<List<MusicFiles>> {
        return combine(
            songDao.getAllSongs(), albumImageDao.getAllAlbumImages(), excludedFolders
        ) { dbSongs, cachedImages, excluded ->
            val filteredDbSongs = dbSongs.filter { song ->
                excluded.none { excludedPath -> song.path.startsWith(excludedPath) }
            }

            val sortedDbSongs = when (sortOrder) {
                DATA.SORT_BY_NAME -> filteredDbSongs.sortedWith(compareBy({ it.title.lowercase() }, { it.id }))
                DATA.SORT_BY_DATE -> filteredDbSongs.sortedWith(compareByDescending<com.flatcode.littleplayer.data.entity.SongEntity> { it.dateAdded }.thenBy { it.title })
                DATA.SORT_BY_PLAY_COUNT -> filteredDbSongs.sortedWith(compareByDescending<com.flatcode.littleplayer.data.entity.SongEntity> { it.playCount }.thenBy { it.title })
                DATA.SORT_BY_SIZE -> filteredDbSongs.sortedWith(compareByDescending<com.flatcode.littleplayer.data.entity.SongEntity> { it.size }.thenBy { it.title })
                DATA.SORT_BY_RELEASE_DATE -> filteredDbSongs.sortedWith(compareByDescending<com.flatcode.littleplayer.data.entity.SongEntity> { it.year }.thenBy { it.title })
                else -> filteredDbSongs
            }

            val imageMap = cachedImages.associateBy { it.albumName }
            sortedDbSongs.map { dbSong ->
                val cleanedAlbum = if (dbSong.album != null && dbSong.path.isNotEmpty()) {
                    val folderName = File(dbSong.path).parentFile?.name
                    if (dbSong.album.equals(
                            folderName, ignoreCase = true
                        )
                    ) DATA.UNKNOWN else dbSong.album
                } else {
                    dbSong.album ?: DATA.UNKNOWN
                }

                MusicFiles(
                    path = dbSong.path,
                    title = dbSong.title,
                    artist = dbSong.artist,
                    album = cleanedAlbum,
                    duration = dbSong.duration.toString(),
                    id = dbSong.id,
                    albumId = dbSong.albumId,
                    waveform = dbSong.waveform,
                    playCount = dbSong.playCount,
                    cachedImagePath = imageMap[cleanedAlbum]?.imagePath,
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
            val excluded = try {
                excludedFolders.first()
            } catch (_: Exception) {
                emptySet()
            }

            try {
                val dbSongs = songDao.getAllSongsSync()
                if (dbSongs.isNotEmpty()) {
                    val cachedImages = try {
                        albumImageDao.getAllAlbumImagesSync().associateBy { it.albumName }
                    } catch (_: Exception) {
                        emptyMap()
                    }

                    dbSongs.forEach { dbSong ->
                        if (excluded.any { dbSong.path.startsWith(it) }) return@forEach

                        val cleanedAlbum = if (dbSong.album != null && dbSong.path.isNotEmpty()) {
                            val folderName = File(dbSong.path).parentFile?.name
                            if (dbSong.album.equals(
                                    folderName, ignoreCase = true
                                )
                            ) DATA.UNKNOWN else dbSong.album
                        } else {
                            dbSong.album ?: DATA.UNKNOWN
                        }

                        tempAudioList.add(
                            MusicFiles(
                                path = dbSong.path,
                                title = dbSong.title,
                                artist = dbSong.artist,
                                album = cleanedAlbum,
                                duration = dbSong.duration.toString(),
                                id = dbSong.id,
                                albumId = dbSong.albumId,
                                waveform = dbSong.waveform,
                                playCount = dbSong.playCount,
                                cachedImagePath = cachedImages[cleanedAlbum]?.imagePath,
                                dateAdded = dbSong.dateAdded,
                                size = dbSong.size,
                                year = dbSong.year
                            )
                        )
                    }

                    when (sortOrder) {
                        DATA.SORT_BY_NAME -> tempAudioList.sortWith(compareBy({ it.title?.lowercase() }, { it.id }))
                        DATA.SORT_BY_PLAY_COUNT -> tempAudioList.sortWith(compareByDescending<MusicFiles> { it.playCount }.thenBy { it.title })
                        DATA.SORT_BY_DATE -> tempAudioList.sortWith(compareByDescending<MusicFiles> { it.dateAdded }.thenBy { it.title })
                        DATA.SORT_BY_SIZE -> tempAudioList.sortWith(compareByDescending<MusicFiles> { it.size }.thenBy { it.title })
                        DATA.SORT_BY_RELEASE_DATE -> tempAudioList.sortWith(compareByDescending<MusicFiles> { it.year }.thenBy { it.title })
                    }

                    if (tempAudioList.isNotEmpty()) {
                        scheduleSync()
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
                            val path = cursor.getString(pathColumn) ?: ""
                            if (path.contains(
                                    "/Android/data", true
                                ) || path.contains("/Android/media", true)
                            ) continue
                            if (excluded.any { path.startsWith(it) }) continue

                            val rawAlbum = cursor.getString(albumColumn) ?: DATA.UNKNOWN
                            val folderName = File(path).parentFile?.name
                            val cleanedAlbum = if (rawAlbum.equals(
                                    folderName, ignoreCase = true
                                )
                            ) DATA.UNKNOWN else rawAlbum

                            tempAudioList.add(
                                MusicFiles(
                                    path = path,
                                    title = cursor.getString(titleColumn) ?: DATA.UNKNOWN,
                                    artist = cursor.getString(artistColumn) ?: DATA.UNKNOWN,
                                    album = cleanedAlbum,
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
                    scheduleSync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            tempAudioList
        }

    private suspend fun syncWithMediaStore() {
        val excluded = try {
            excludedFolders.first()
        } catch (_: Exception) {
            emptySet()
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

        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        val songEntities = mutableListOf<com.flatcode.littleplayer.data.entity.SongEntity>()
        cursor?.use { c ->
            val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val albumIdColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val yearColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (c.moveToNext()) {
                val path = c.getString(pathColumn) ?: ""
                if (path.contains("/Android/data", true) || path.contains(
                        "/Android/media", true
                    )
                ) continue
                if (excluded.any { path.startsWith(it) }) continue

                val album = c.getString(albumColumn) ?: DATA.UNKNOWN
                val title = c.getString(titleColumn) ?: DATA.UNKNOWN
                val duration = c.getLong(durationColumn)
                val artist = c.getString(artistColumn) ?: DATA.UNKNOWN
                val id = c.getString(idColumn) ?: ""
                val albumId = c.getString(albumIdColumn) ?: ""
                val dateAdded = c.getLong(dateAddedColumn)
                val size = c.getLong(sizeColumn)
                val year = c.getInt(yearColumn)

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

        val dbSongs = songDao.getAllSongsSync()
        val mediaStoreIds = songEntities.map { it.id }.toSet()
        dbSongs.filter { it.id !in mediaStoreIds }.forEach {
            songDao.deleteSongById(it.id)
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


    suspend fun deleteFromDatabase(songId: String) {
        songDao.deleteSongById(songId)
    }

    suspend fun updateMetadata(songId: String, title: String, artist: String, album: String?) {
        songDao.updateMetadata(songId, title, artist, album)
    }

    fun getSongUri(songId: String): Uri {
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong()
        )
    }

    suspend fun getPlaybackStateSync() = withContext(Dispatchers.IO) {
        musicDao.getPlaybackStateSync()
    }

    fun updateCurrentPlaylist(songs: List<MusicFiles>, saveToRoom: Boolean = true) {
        _currentPlaylist.value = songs
        if (saveToRoom) {
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
    }

    suspend fun loadCurrentQueue(): List<MusicFiles> = withContext(Dispatchers.IO) {
        val excluded = try {
            excludedFolders.first()
        } catch (_: Exception) {
            emptySet()
        }
        musicDao.getQueue().filter { entity ->
            excluded.none { excludedPath -> entity.path?.startsWith(excludedPath) == true }
        }.map {
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

    suspend fun clearArtCache() = withContext(Dispatchers.IO) {
        albumImageDao.clearAllAlbumImages()
        val folder = File(context.filesDir, "album_art")
        if (folder.exists()) {
            folder.listFiles()?.forEach { it.delete() }
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        musicDao.clearRecent()
    }

    fun getCacheSize(): Long {
        val folder = File(context.filesDir, "album_art")
        return if (folder.exists()) {
            folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }
}