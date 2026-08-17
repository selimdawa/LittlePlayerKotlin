package com.flatcode.littleplayer.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.palette.graphics.Palette
import com.flatcode.littleplayer.data.dao.AlbumImageDao
import com.flatcode.littleplayer.data.dao.MusicDao
import com.flatcode.littleplayer.data.dao.SongDao
import com.flatcode.littleplayer.data.entity.CurrentQueueEntity
import com.flatcode.littleplayer.data.entity.PlaybackStateEntity
import com.flatcode.littleplayer.data.entity.SongEntity
import com.flatcode.littleplayer.di.IoDispatcher
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.Resource
import com.flatcode.littleplayer.utils.extractDynamicColors
import com.flatcode.littleplayer.utils.getAlbumArtBytes
import com.flatcode.littleplayer.utils.getLibraryColor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Singleton
class MusicRepository @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val songDao: SongDao,
    private val albumImageDao: AlbumImageDao,
    private val musicDao: MusicDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val repositoryScope = CoroutineScope(ioDispatcher + SupervisorJob())
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
                _syncStatus.value = Resource.Loading()
                syncWithMediaStore()
                _syncStatus.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _syncStatus.value = Resource.Error(e.message ?: "Unknown sync error")
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

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _syncStatus = MutableStateFlow<Resource<Unit>>(Resource.Success(Unit))
    val syncStatus: StateFlow<Resource<Unit>> = _syncStatus.asStateFlow()

    private var queueUpdateJob: Job? = null

    fun getSortOrder(category: String): Flow<String> = dataStore.data.map { preferences ->
        preferences[stringPreferencesKey(category)]
            ?: if (category == DATA.SONGS) DATA.SORT_BY_DATE else DATA.SORT_BY_NAME
    }.distinctUntilChanged()

    suspend fun saveSortOrder(category: String, sortType: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(category)] = sortType
        }
    }

    val shuffleMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[androidx.datastore.preferences.core.booleanPreferencesKey(DATA.SHUFFLE_MODE)]
            ?: false
    }.distinctUntilChanged()

    suspend fun saveShuffleMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[androidx.datastore.preferences.core.booleanPreferencesKey(DATA.SHUFFLE_MODE)] =
                enabled
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
            songDao.getAllSongs(), excludedFolders
        ) { dbSongs, excluded ->
            val filteredDbSongs = dbSongs.filter { song ->
                excluded.none { excludedPath -> song.path.startsWith(excludedPath) }
            }

            val sortedDbSongs = when (sortOrder) {
                DATA.SORT_BY_NAME -> filteredDbSongs.sortedWith(
                    compareBy({ it.title.lowercase() }, { it.id })
                )

                DATA.SORT_BY_DATE -> filteredDbSongs.sortedWith(compareByDescending<SongEntity> { it.dateAdded }.thenBy { it.title })
                DATA.SORT_BY_PLAY_COUNT -> filteredDbSongs.sortedWith(compareByDescending<SongEntity> { it.playCount }.thenBy { it.title })
                DATA.SORT_BY_SIZE -> filteredDbSongs.sortedWith(compareByDescending<SongEntity> { it.size }.thenBy { it.title })
                DATA.SORT_BY_RELEASE_DATE -> filteredDbSongs.sortedWith(compareByDescending<SongEntity> { it.year }.thenBy { it.title })
                else -> filteredDbSongs
            }

            sortedDbSongs.map { dbSong ->
                val cleanedAlbum = if (dbSong.album != null && dbSong.path.isNotEmpty()) {
                    val path = dbSong.path
                    val lastSlash = path.lastIndexOf(File.separatorChar)
                    val secondLastSlash = if (lastSlash > 0) path.lastIndexOf(File.separatorChar, lastSlash - 1) else -1
                    val folderName = if (lastSlash > 0) {
                        path.substring(secondLastSlash + 1, lastSlash)
                    } else null

                    if (dbSong.album.equals(folderName, ignoreCase = true)) DATA.UNKNOWN else dbSong.album
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
                    cachedImagePath = dbSong.cachedImagePath,
                    dominantColor = dbSong.dominantColor,
                    vibrantColor = dbSong.vibrantColor,
                    dateAdded = dbSong.dateAdded,
                    size = dbSong.size,
                    year = dbSong.year
                )
            }
        }.flowOn(ioDispatcher)
    }

    suspend fun getAllAudio(sortOrder: String): ArrayList<MusicFiles> =
        withContext(ioDispatcher) {
            val tempAudioList = ArrayList<MusicFiles>()
            val excluded = try {
                excludedFolders.first()
            } catch (_: Exception) {
                emptySet()
            }

            try {
                val dbSongs = songDao.getAllSongsSync()
                if (dbSongs.isNotEmpty()) {
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
                                cachedImagePath = dbSong.cachedImagePath,
                                dominantColor = dbSong.dominantColor,
                                vibrantColor = dbSong.vibrantColor,
                                dateAdded = dbSong.dateAdded,
                                size = dbSong.size,
                                year = dbSong.year
                            )
                        )
                    }

                    when (sortOrder) {
                        DATA.SORT_BY_NAME -> tempAudioList.sortWith(
                            compareBy({ it.title?.lowercase() }, { it.id })
                        )

                        DATA.SORT_BY_PLAY_COUNT -> tempAudioList.sortWith(compareByDescending<MusicFiles> { it.playCount }.thenBy { it.title })
                        DATA.SORT_BY_DATE -> tempAudioList.sortWith(compareByDescending<MusicFiles> { it.dateAdded }.thenBy { it.title })
                        DATA.SORT_BY_SIZE -> tempAudioList.sortWith(compareByDescending<MusicFiles> { it.size }.thenBy { it.title })
                        DATA.SORT_BY_RELEASE_DATE -> tempAudioList.sortWith(compareByDescending<MusicFiles> { it.year }.thenBy { it.title })
                    }

                    if (tempAudioList.isNotEmpty()) {
                        scheduleSync()
                        if (_isInitialLoading.value) {
                            startBackgroundColorExtraction()
                            startBackgroundArtCaching(tempAudioList)
                            _isInitialLoading.value = false
                        } else {
                            startBackgroundColorExtraction()
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
                    syncWithMediaStore() // Perform sync immediately if it was the first time and empty DB
                    if (_isInitialLoading.value) {
                        startBackgroundColorExtraction()
                        startBackgroundArtCaching(tempAudioList)
                        _isInitialLoading.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isInitialLoading.value = false // Ensure we don't get stuck
            }

            tempAudioList
        }

    private suspend fun syncWithMediaStore() {
        val excluded = try {
            excludedFolders.first()
        } catch (_: Exception) {
            emptySet()
        }

        val lastSyncTime = dataStore.data.map { it[androidx.datastore.preferences.core.longPreferencesKey(DATA.LAST_SYNC_TIME)] ?: 0L }.first()
        val currentTime = System.currentTimeMillis() / 1000

        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        
        // 1. Handle Deletions: Get all IDs from MediaStore
        val mediaStoreIds = mutableSetOf<String>()
        context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media._ID), null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (cursor.moveToNext()) {
                mediaStoreIds.add(cursor.getString(idColumn))
            }
        }

        val dbSongs = songDao.getAllSongsSync()
        dbSongs.filter { it.id !in mediaStoreIds }.forEach {
            songDao.deleteSongById(it.id)
            musicDao.deleteRecentById(it.id)
            musicDao.deleteFavoriteById(it.id)
        }

        // 2. Handle Additions/Updates: Query items modified since last sync
        val selection = "${MediaStore.Audio.Media.DATE_MODIFIED} > ?"
        val selectionArgs = arrayOf(lastSyncTime.toString())
        
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

        val allFavs = musicDao.getAllFavoritesSync().associateBy({ it.songId }, { it.timestamp })
        val songEntities = mutableListOf<SongEntity>()

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { c ->
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
                if (path.contains("/Android/data", true) || path.contains("/Android/media", true)) continue
                if (excluded.any { path.startsWith(it) }) continue

                val id = c.getString(idColumn) ?: ""
                val existing = dbSongs.find { it.id == id }

                songEntities.add(
                    SongEntity(
                        id = id,
                        title = c.getString(titleColumn) ?: DATA.UNKNOWN,
                        artist = c.getString(artistColumn) ?: DATA.UNKNOWN,
                        album = c.getString(albumColumn) ?: DATA.UNKNOWN,
                        duration = c.getLong(durationColumn),
                        path = path,
                        albumId = c.getString(albumIdColumn),
                        dateAdded = c.getLong(dateAddedColumn),
                        size = c.getLong(sizeColumn),
                        year = c.getInt(yearColumn),
                        isFavorite = id in allFavs,
                        favoriteDate = allFavs[id] ?: 0L,
                        cachedImagePath = existing?.cachedImagePath,
                        dominantColor = existing?.dominantColor,
                        vibrantColor = existing?.vibrantColor,
                        playCount = existing?.playCount ?: 0,
                        waveform = existing?.waveform
                    )
                )
            }
        }

        if (songEntities.isNotEmpty()) {
            songDao.insertSongs(songEntities)
            scheduleBackgroundColorExtraction()
        }

        dataStore.edit { it[androidx.datastore.preferences.core.longPreferencesKey(DATA.LAST_SYNC_TIME)] = currentTime }
    }

    private var colorExtractionJob: Job? = null
    private fun scheduleBackgroundColorExtraction() {
        colorExtractionJob?.cancel()
        colorExtractionJob = repositoryScope.launch {
            delay(5.seconds)
            startBackgroundColorExtraction()
        }
    }

    suspend fun startBackgroundColorExtraction() = withContext(ioDispatcher) {
        val missing = songDao.getSongsMissingColors()
        if (missing.isEmpty()) return@withContext

        val track = context.getLibraryColor("mc_track")
        val tick = context.getLibraryColor("mc_tick")

        // Process in chunks to avoid overwhelming the system
        missing.chunked(20).forEach { chunk ->
            chunk.map { song ->
                launch { extractColorsForSong(song.id, song.path, song.albumId, track, tick) }
            }.joinAll()
            delay(50.milliseconds)
        }
    }

    suspend fun extractColorsForSong(
        songId: String, path: String, albumId: String?, defaultStart: Int, defaultEnd: Int
    ) = withContext(ioDispatcher) {
        try {
            // Check if already has colors
            val existing = songDao.getSongById(songId)
            if (existing?.dominantColor != null && existing.vibrantColor != null && existing.dominantColor != 0) return@withContext

            val bitmap = getSmallArtBitmap(albumId, path)
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val colors = palette.extractDynamicColors(defaultStart, defaultEnd)
                songDao.updateSongColors(songId, colors.second, colors.first)
                bitmap.recycle()
            } else {
                songDao.updateSongColors(songId, 0, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSmallArtBitmap(
        albumId: String?, path: String
    ): Bitmap? {
        val artBytes = getAlbumArtBytes(path)
        if (artBytes != null) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, options)
            options.inSampleSize = calculateInSampleSize(options, 100, 100)
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, options)
        }

        try {
            val file = File(path)
            val parent = file.parentFile
            if (parent != null && parent.exists()) {
                val imageFile = parent.listFiles { _, name ->
                    val n = name.lowercase()
                    n == "cover.jpg" || n == "cover.png" || n == "folder.jpg" || n == "album.jpg" || n == "albumart.jpg"
                }?.firstOrNull()
                if (imageFile != null) {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(imageFile.absolutePath, options)
                    options.inSampleSize = calculateInSampleSize(options, 100, 100)
                    options.inJustDecodeBounds = false
                    return BitmapFactory.decodeFile(imageFile.absolutePath, options)
                }
            }
        } catch (_: Exception) {
        }

        return null
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    suspend fun cacheAlbumArt(song: MusicFiles) = withContext(ioDispatcher) {
        val songId = song.id ?: return@withContext
        val path = song.path ?: ""

        val folder = File(context.filesDir, "song_art")
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, "$songId.jpg")
        if (file.exists()) {
            songDao.updateCachedImagePath(songId, file.absolutePath)
            return@withContext
        }

        val artBytes = getAlbumArtBytes(path) ?: return@withContext

        try {
            FileOutputStream(file).use { out ->
                out.write(artBytes)
            }
            songDao.updateCachedImagePath(songId, file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun startBackgroundArtCaching(songs: List<MusicFiles>) = withContext(ioDispatcher) {
        val uncachedSongs = songs.filter { it.cachedImagePath == null }
        if (uncachedSongs.isEmpty()) return@withContext

        uncachedSongs.chunked(10).forEach { chunk ->
            chunk.map { song ->
                launch { cacheAlbumArt(song) }
            }.joinAll()
            delay(50.milliseconds)
        }
    }

    suspend fun deleteFromDatabase(songId: String) {
        songDao.deleteSongById(songId)
        musicDao.deleteRecentById(songId)
        musicDao.deleteFavoriteById(songId)
    }

    suspend fun updateMetadata(songId: String, title: String, artist: String, album: String?) {
        songDao.updateMetadata(songId, title, artist, album)
    }

    fun getSongUri(songId: String): Uri {
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong()
        )
    }

    fun updateCurrentPlaylist(
        songs: List<MusicFiles>,
        startIndex: Int = -1,
        saveToRoom: Boolean = true,
        forceShuffleMode: Boolean? = null
    ) {
        val finalSongs = songs
        _currentPlaylist.value = finalSongs

        queueUpdateJob?.cancel()
        queueUpdateJob = repositoryScope.launch {
            if (saveToRoom) {
                musicDao.clearQueue()

                // Save playback state if startIndex is provided
                if (startIndex != -1 && songs.isNotEmpty() && startIndex in songs.indices) {
                    val song = songs[startIndex]
                    val currentState = musicDao.getPlaybackStateSync() ?: PlaybackStateEntity()
                    musicDao.savePlaybackState(
                        currentState.copy(
                            currentSongId = song.id,
                            lastPosition = startIndex,
                            shuffleModeEnabled = forceShuffleMode ?: currentState.shuffleModeEnabled
                        )
                    )
                }

                // OPTIMIZATION: Use a map for original indices
                val originalIndexMap = songs.mapIndexed { index, song -> song.id to index }.toMap()

                val entities = finalSongs.mapIndexed { index, song ->
                    val originalIdx = originalIndexMap[song.id] ?: index
                    CurrentQueueEntity(
                        songId = song.id ?: "",
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        albumId = song.albumId,
                        duration = song.duration,
                        path = song.path,
                        cachedImagePath = song.cachedImagePath,
                        orderIndex = index,
                        originalOrderIndex = originalIdx,
                        dominantColor = song.dominantColor,
                        vibrantColor = song.vibrantColor
                    )
                }
                musicDao.insertQueue(entities)
            }
        }
    }

    suspend fun loadCurrentQueue(): List<MusicFiles> = withContext(ioDispatcher) {
        val excluded = try {
            excludedFolders.first()
        } catch (_: Exception) {
            emptySet()
        }
        val queue = musicDao.getQueue().filter { entity ->
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
                cachedImagePath = it.cachedImagePath,
                dominantColor = it.dominantColor,
                vibrantColor = it.vibrantColor
            )
        }
        _currentPlaylist.value = queue
        queue
    }

    suspend fun clearArtCache() = withContext(ioDispatcher) {
        albumImageDao.clearAllAlbumImages()
        songDao.clearAllCachedImagePaths()
        musicDao.clearQueueCachedImagePaths()
        val folder = File(context.filesDir, "song_art")
        if (folder.exists()) {
            folder.listFiles()?.forEach { it.delete() }
        }

        // Optionally re-trigger background caching for the current list
        val currentSongs = getAllAudio(DATA.SORT_BY_DATE)
        startBackgroundArtCaching(currentSongs)
    }

    suspend fun clearHistory() = withContext(ioDispatcher) {
        musicDao.clearRecent()
    }

    suspend fun resetPaletteColors() = withContext(ioDispatcher) {
        songDao.resetAllColors()
        startBackgroundColorExtraction()
    }

    fun getCacheSize(): Long {
        val folder = File(context.filesDir, "song_art")
        return if (folder.exists()) {
            folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }
}