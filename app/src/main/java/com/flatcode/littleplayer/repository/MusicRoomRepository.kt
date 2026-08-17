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
import com.flatcode.littleplayer.di.IoDispatcher
import com.flatcode.littleplayer.utils.DATA
import kotlinx.coroutines.CoroutineDispatcher
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
    dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    val excludedFolders: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[stringSetPreferencesKey(DATA.EXCLUDED_FOLDERS)] ?: emptySet()
    }.distinctUntilChanged()

    suspend fun getAlbumImageByName(albumName: String): AlbumImageEntity? =
        withContext(ioDispatcher) {
            albumImageDao.getAlbumImageByName(albumName)
        }

    suspend fun insertFavorite(song: FavoriteEntity) = withContext(ioDispatcher) {
        musicDao.insertFavorite(song)
        songDao.updateFavoriteStatus(song.songId, true)
    }

    suspend fun deleteFavorite(song: FavoriteEntity) = withContext(ioDispatcher) {
        musicDao.deleteFavorite(song)
        songDao.updateFavoriteStatus(song.songId, false)
    }

    suspend fun deleteFavoriteById(id: String) = withContext(ioDispatcher) {
        musicDao.deleteFavoriteById(id)
        songDao.updateFavoriteStatus(id, false)
    }

    fun getAllFavorites(): Flow<List<FavoriteEntity>> = musicDao.getAllFavorites()

    suspend fun isFavorite(id: String): Boolean = withContext(ioDispatcher) {
        musicDao.isFavorite(id)
    }

    suspend fun insertToPlaylist(playlistItems: List<PlaylistEntity>) =
        withContext(ioDispatcher) {
            musicDao.insertToPlaylist(playlistItems)
        }

    suspend fun deleteFromPlaylist(name: String, songId: String) = withContext(ioDispatcher) {
        musicDao.deleteFromPlaylist(name, songId)
    }

    suspend fun deletePlaylist(name: String) = withContext(ioDispatcher) {
        musicDao.deletePlaylist(name)
    }

    suspend fun renamePlaylist(oldName: String, newName: String) = withContext(ioDispatcher) {
        musicDao.renamePlaylist(oldName, newName)
    }

    fun getSongsFromPlaylist(name: String): Flow<List<PlaylistEntity>> =
        musicDao.getSongsFromPlaylist(name)

    suspend fun getSongsFromPlaylistSync(name: String): List<PlaylistEntity> =
        withContext(ioDispatcher) {
            musicDao.getSongsFromPlaylistSync(name)
        }

    fun getAllPlaylistNames(): Flow<List<String>> = musicDao.getAllPlaylistNames()

    fun getPlaylistsContainingSong(songId: String): Flow<List<String>> =
        musicDao.getPlaylistsContainingSong(songId)

    fun getAllAlbumImages(): Flow<List<AlbumImageEntity>> = albumImageDao.getAllAlbumImages()

    suspend fun insertRecent(song: RecentEntity) = withContext(ioDispatcher) {
        musicDao.insertRecent(song)
        musicDao.trimRecent()
    }

    fun getAllRecent(): Flow<List<RecentEntity>> = musicDao.getAllRecent()

    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()

    fun getSongsByIds(ids: List<String>): Flow<List<SongEntity>> = songDao.getSongsByIds(ids)

    fun getFavoriteSongs(): Flow<List<SongEntity>> = songDao.getFavoriteSongs()

    suspend fun deleteRecentById(songId: String) = withContext(ioDispatcher) {
        musicDao.deleteRecentById(songId)
    }

    suspend fun clearRecent() = withContext(ioDispatcher) {
        musicDao.clearRecent()
    }

    suspend fun incrementPlayCount(songId: String) = withContext(ioDispatcher) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun updateWaveform(songId: String, waveform: String) = withContext(ioDispatcher) {
        songDao.updateWaveform(songId, waveform)
    }

    suspend fun updateSongColors(songId: String, dominant: Int?, vibrant: Int?) = withContext(ioDispatcher) {
        songDao.updateSongColors(songId, dominant, vibrant)
    }

    suspend fun resetAllColors() = withContext(ioDispatcher) {
        songDao.resetAllColors()
    }

    suspend fun getSongById(songId: String): SongEntity? = withContext(ioDispatcher) {
        songDao.getSongById(songId)
    }

    suspend fun saveEqualizerSettings(equalizerEntity: EqualizerEntity) =
        withContext(ioDispatcher) {
            musicDao.saveEqualizerSettings(equalizerEntity)
        }

    fun getEqualizerSettings(): Flow<EqualizerEntity?> = musicDao.getEqualizerSettings()

    suspend fun savePlaybackState(state: PlaybackStateEntity) = withContext(ioDispatcher) {
        musicDao.savePlaybackState(state)
    }

    suspend fun getPlaybackStateSync(): PlaybackStateEntity? = withContext(ioDispatcher) {
        musicDao.getPlaybackStateSync()
    }

    suspend fun getQueue() = withContext(ioDispatcher) {
        musicDao.getQueue()
    }
}
