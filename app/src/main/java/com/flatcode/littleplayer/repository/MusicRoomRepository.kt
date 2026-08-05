package com.flatcode.littleplayer.repository

import com.flatcode.littleplayer.data.dao.AlbumImageDao
import com.flatcode.littleplayer.data.dao.MusicDao
import com.flatcode.littleplayer.data.dao.SongDao
import com.flatcode.littleplayer.data.entity.AlbumImageEntity
import com.flatcode.littleplayer.data.entity.EqualizerEntity
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.data.entity.PlaylistEntity
import com.flatcode.littleplayer.data.entity.RecentEntity
import com.flatcode.littleplayer.data.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRoomRepository @Inject constructor(
    private val songDao: SongDao,
    private val albumImageDao: AlbumImageDao,
    private val musicDao: MusicDao
) {
    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()

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

    fun getAllFavorites(): Flow<List<FavoriteEntity>> = musicDao.getAllFavorites()

    suspend fun isFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        musicDao.isFavorite(id)
    }

    suspend fun insertToPlaylist(playlistItem: PlaylistEntity) = withContext(Dispatchers.IO) {
        musicDao.insertToPlaylist(playlistItem)
    }

    suspend fun deleteFromPlaylist(name: String, songId: String) = withContext(Dispatchers.IO) {
        musicDao.deleteFromPlaylist(name, songId)
    }

    fun getSongsFromPlaylist(name: String): Flow<List<PlaylistEntity>> =
        musicDao.getSongsFromPlaylist(name)

    fun getAllPlaylistNames(): Flow<List<String>> = musicDao.getAllPlaylistNames()

    fun getAllAlbumImages(): Flow<List<AlbumImageEntity>> = albumImageDao.getAllAlbumImages()

    suspend fun getAllAlbumImagesSync(): List<AlbumImageEntity> = withContext(Dispatchers.IO) {
        albumImageDao.getAllAlbumImagesSync()
    }

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

    suspend fun updateLyrics(songId: String, lyrics: String) = withContext(Dispatchers.IO) {
        songDao.updateLyrics(songId, lyrics)
    }

    suspend fun getSongById(songId: String): SongEntity? = withContext(Dispatchers.IO) {
        songDao.getSongById(songId)
    }

    suspend fun saveEqualizerSettings(equalizerEntity: EqualizerEntity) = withContext(Dispatchers.IO) {
        musicDao.saveEqualizerSettings(equalizerEntity)
    }

    suspend fun getEqualizerSettings(): EqualizerEntity? = withContext(Dispatchers.IO) {
        musicDao.getEqualizerSettings()
    }
}