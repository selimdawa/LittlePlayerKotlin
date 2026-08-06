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

    @Query("SELECT * FROM songs_table ORDER BY playCount DESC")
    suspend fun getSongsByPlayCountSync(): List<SongEntity>

    @Query("SELECT * FROM songs_table ORDER BY playCount DESC")
    fun getSongsByPlayCount(): Flow<List<SongEntity>>

    @Query("UPDATE songs_table SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String)

    @Query("UPDATE songs_table SET waveform = :waveform WHERE id = :songId")
    suspend fun updateWaveform(songId: String, waveform: String)

    @Query("UPDATE songs_table SET lyrics = :lyrics WHERE id = :songId")
    suspend fun updateLyrics(songId: String, lyrics: String)

    @Query("UPDATE songs_table SET color = :color WHERE id = :songId")
    suspend fun updateSongColor(songId: String, color: Int)

    @Query("SELECT * FROM songs_table WHERE id = :songId")
    suspend fun getSongById(songId: String): SongEntity?

    @Query("SELECT * FROM songs_table WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs_table WHERE album = :albumName")
    fun getSongsByAlbum(albumName: String): Flow<List<SongEntity>>

    @Query("DELETE FROM songs_table WHERE id = :songId")
    suspend fun deleteSongById(songId: String)
}