package com.flatcode.littleplayer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.data.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(song: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(song: FavoriteEntity)

    @Query("SELECT * FROM favorites_table")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT * FROM favorites_table WHERE songId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToPlaylist(playlistItem: PlaylistEntity)

    @Query("DELETE FROM playlists_table WHERE playlistName = :name AND songId = :sId")
    suspend fun deleteFromPlaylist(name: String, sId: String)

    @Query("SELECT * FROM playlists_table WHERE playlistName = :name")
    fun getSongsFromPlaylist(name: String): Flow<List<PlaylistEntity>>

    @Query("SELECT DISTINCT playlistName FROM playlists_table")
    fun getAllPlaylistNames(): Flow<List<String>>
}