package com.flatcode.littleplayer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flatcode.littleplayer.data.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("SELECT * FROM songs_table ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs_table ORDER BY title ASC")
    suspend fun getAllSongsSync(): List<SongEntity>

    @Query("UPDATE songs_table SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String)

    @Query("UPDATE songs_table SET waveform = :waveform WHERE id = :songId")
    suspend fun updateWaveform(songId: String, waveform: String)

    @Query("UPDATE songs_table SET title = :title, artist = :artist, album = :album WHERE id = :songId")
    suspend fun updateMetadata(songId: String, title: String, artist: String, album: String?)

    @Query("UPDATE songs_table SET cachedImagePath = :path WHERE id = :songId")
    suspend fun updateCachedImagePath(songId: String, path: String?)

    @Query("UPDATE songs_table SET cachedBlurPath = :path WHERE id = :songId")
    suspend fun updateCachedBlurPath(songId: String, path: String?)

    @Query("UPDATE songs_table SET cachedImagePath = NULL")
    suspend fun clearAllCachedImagePaths()

    @Query("UPDATE songs_table SET dominantColor = :dominantColor, vibrantColor = :vibrantColor WHERE id = :songId")
    suspend fun updateSongColors(songId: String, dominantColor: Int?, vibrantColor: Int?)

    @Query("SELECT * FROM songs_table WHERE id = :songId")
    suspend fun getSongById(songId: String): SongEntity?

    @Query("SELECT * FROM songs_table WHERE dominantColor IS NULL OR vibrantColor IS NULL")
    suspend fun getSongsMissingColors(): List<SongEntity>

    @Query("SELECT * FROM songs_table WHERE isFavorite = 1 ORDER BY favoriteDate DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs_table SET dominantColor = NULL, vibrantColor = NULL")
    suspend fun resetAllColors()

    @Query("SELECT * FROM songs_table WHERE id IN (:ids)")
    fun getSongsByIds(ids: List<String>): Flow<List<SongEntity>>

    @Query("SELECT s.* FROM songs_table s INNER JOIN recent_table r ON s.id = r.songId ORDER BY r.timestamp DESC")
    fun getRecentSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs_table SET isFavorite = :isFavorite, favoriteDate = :favoriteDate WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: String, isFavorite: Boolean, favoriteDate: Long)

    @Query("DELETE FROM songs_table WHERE id = :songId")
    suspend fun deleteSongById(songId: String)
}