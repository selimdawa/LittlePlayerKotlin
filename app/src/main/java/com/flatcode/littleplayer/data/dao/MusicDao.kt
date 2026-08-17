package com.flatcode.littleplayer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flatcode.littleplayer.data.entity.CurrentQueueEntity
import com.flatcode.littleplayer.data.entity.EqualizerEntity
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.data.entity.PlaybackStateEntity
import com.flatcode.littleplayer.data.entity.PlaylistEntity
import com.flatcode.littleplayer.data.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(song: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(song: FavoriteEntity)

    @Query("DELETE FROM favorites_table WHERE songId = :id")
    suspend fun deleteFavoriteById(id: String)

    @Query("SELECT * FROM favorites_table")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT * FROM favorites_table WHERE songId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToPlaylist(playlistItem: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToPlaylist(playlistItems: List<PlaylistEntity>)

    @Query("DELETE FROM playlists_table WHERE playlistName = :name AND songId = :sId")
    suspend fun deleteFromPlaylist(name: String, sId: String)

    @Query("SELECT * FROM playlists_table WHERE playlistName = :name")
    fun getSongsFromPlaylist(name: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists_table WHERE playlistName = :name")
    suspend fun getSongsFromPlaylistSync(name: String): List<PlaylistEntity>

    @Query("DELETE FROM playlists_table WHERE playlistName = :name")
    suspend fun deletePlaylist(name: String)

    @Query("UPDATE playlists_table SET playlistName = :newName WHERE playlistName = :oldName")
    suspend fun renamePlaylist(oldName: String, newName: String)

    @Query("SELECT DISTINCT playlistName FROM playlists_table")
    fun getAllPlaylistNames(): Flow<List<String>>

    @Query("SELECT DISTINCT playlistName FROM playlists_table")
    suspend fun getAllPlaylistNamesSync(): List<String>

    @Query("SELECT playlistName FROM playlists_table WHERE songId = :songId")
    fun getPlaylistsContainingSong(songId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(song: RecentEntity)

    @Query("SELECT * FROM recent_table ORDER BY timestamp DESC")
    fun getAllRecent(): Flow<List<RecentEntity>>

    @Query("DELETE FROM recent_table")
    suspend fun clearRecent()

    @Query("DELETE FROM recent_table WHERE songId NOT IN (SELECT songId FROM recent_table ORDER BY timestamp DESC LIMIT 20)")
    suspend fun trimRecent()

    @Query("DELETE FROM current_queue_table")
    suspend fun clearQueue()

    @Query("UPDATE current_queue_table SET cachedImagePath = NULL")
    suspend fun clearQueueCachedImagePaths()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(queue: List<CurrentQueueEntity>)

    @Query("SELECT * FROM current_queue_table ORDER BY orderIndex ASC")
    suspend fun getQueue(): List<CurrentQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEqualizerSettings(equalizerEntity: EqualizerEntity)

    @Query("SELECT * FROM equalizer_table WHERE id = 1")
    fun getEqualizerSettings(): Flow<EqualizerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlaybackState(state: PlaybackStateEntity)

    @Query("SELECT * FROM playback_state_table WHERE id = 1")
    fun getPlaybackState(): Flow<PlaybackStateEntity?>

    @Query("SELECT * FROM playback_state_table WHERE id = 1")
    suspend fun getPlaybackStateSync(): PlaybackStateEntity?
}