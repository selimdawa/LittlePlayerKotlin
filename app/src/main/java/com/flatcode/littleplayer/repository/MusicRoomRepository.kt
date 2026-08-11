package com.flatcode.littleplayer.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.flatcode.littleplayer.data.dao.AlbumImageDao
import com.flatcode.littleplayer.data.dao.MusicDao
import com.flatcode.littleplayer.data.dao.SongDao
import com.flatcode.littleplayer.data.entity.AlbumImageEntity
import com.flatcode.littleplayer.data.entity.EqualizerEntity
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.data.entity.PlaybackStateEntity
import com.flatcode.littleplayer.data.entity.PlaylistEntity
import com.flatcode.littleplayer.data.entity.RecentEntity
import com.flatcode.littleplayer.data.entity.SongEntity
import com.flatcode.littleplayer.utils.DATA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRoomRepository @Inject constructor(
    private val songDao: SongDao,
    private val albumImageDao: AlbumImageDao,
    private val musicDao: MusicDao,
    private val dataStore: DataStore<Preferences>
) {
    val excludedFolders: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[stringSetPreferencesKey(DATA.EXCLUDED_FOLDERS)] ?: emptySet()
    }.distinctUntilChanged()
    suspend fun insertSong(song: SongEntity) = withContext(Dispatchers.IO) {
        songDao.insertSong(song)
    }

    suspend fun insertAlbumImage(albumImage: AlbumImageEntity) = withContext(Dispatchers.IO) {
        albumImageDao.insertAlbumImage(albumImage)
    }

    suspend fun getAlbumImageByName(albumName: String): AlbumImageEntity? =
        withContext(Dispatchers.IO) {
            albumImageDao.getAlbumImageByName(albumName)
        }

    suspend fun insertFavorite(song: FavoriteEntity) = withContext(Dispatchers.IO) {
        musicDao.insertFavorite(song)
    }

    suspend fun deleteFavorite(song: FavoriteEntity) = withContext(Dispatchers.IO) {
        musicDao.deleteFavorite(song)
    }

    suspend fun deleteFavoriteById(id: String) = withContext(Dispatchers.IO) {
        musicDao.deleteFavoriteById(id)
    }

    fun getAllFavorites(): Flow<List<FavoriteEntity>> = musicDao.getAllFavorites()

    suspend fun isFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        musicDao.isFavorite(id)
    }

    suspend fun insertToPlaylist(playlistItem: PlaylistEntity) = withContext(Dispatchers.IO) {
        musicDao.insertToPlaylist(playlistItem)
    }

    suspend fun insertToPlaylist(playlistItems: List<PlaylistEntity>) =
        withContext(Dispatchers.IO) {
            musicDao.insertToPlaylist(playlistItems)
        }

    suspend fun deleteFromPlaylist(name: String, songId: String) = withContext(Dispatchers.IO) {
        musicDao.deleteFromPlaylist(name, songId)
    }

    suspend fun deletePlaylist(name: String) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylist(name)
    }

    suspend fun renamePlaylist(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        musicDao.renamePlaylist(oldName, newName)
    }

    fun getSongsFromPlaylist(name: String): Flow<List<PlaylistEntity>> =
        musicDao.getSongsFromPlaylist(name)

    suspend fun getSongsFromPlaylistSync(name: String): List<PlaylistEntity> =
        withContext(Dispatchers.IO) {
            musicDao.getSongsFromPlaylistSync(name)
        }

    fun getAllPlaylistNames(): Flow<List<String>> = musicDao.getAllPlaylistNames()

    fun getPlaylistsContainingSong(songId: String): Flow<List<String>> =
        musicDao.getPlaylistsContainingSong(songId)

    fun getAllAlbumImages(): Flow<List<AlbumImageEntity>> = albumImageDao.getAllAlbumImages()

    suspend fun insertRecent(song: RecentEntity) = withContext(Dispatchers.IO) {
        musicDao.insertRecent(song)
        musicDao.trimRecent()
    }

    fun getAllRecent(): Flow<List<RecentEntity>> = musicDao.getAllRecent()

    suspend fun incrementPlayCount(songId: String) = withContext(Dispatchers.IO) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun updateWaveform(songId: String, waveform: String) = withContext(Dispatchers.IO) {
        songDao.updateWaveform(songId, waveform)
    }

    suspend fun getSongById(songId: String): SongEntity? = withContext(Dispatchers.IO) {
        songDao.getSongById(songId)
    }

    suspend fun saveEqualizerSettings(equalizerEntity: EqualizerEntity) =
        withContext(Dispatchers.IO) {
            musicDao.saveEqualizerSettings(equalizerEntity)
        }

    fun getEqualizerSettings(): Flow<EqualizerEntity?> = musicDao.getEqualizerSettings()

    suspend fun savePlaybackState(state: PlaybackStateEntity) = withContext(Dispatchers.IO) {
        musicDao.savePlaybackState(state)
    }

    fun getPlaybackState(): Flow<PlaybackStateEntity?> = musicDao.getPlaybackState()

    suspend fun getPlaybackStateSync(): PlaybackStateEntity? = withContext(Dispatchers.IO) {
        musicDao.getPlaybackStateSync()
    }

    suspend fun getQueue() = withContext(Dispatchers.IO) {
        musicDao.getQueue()
    }
}