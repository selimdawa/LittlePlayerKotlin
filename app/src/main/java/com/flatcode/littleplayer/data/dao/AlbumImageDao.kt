package com.flatcode.littleplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flatcode.littleplayer.data.entity.AlbumImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumImage(albumImage: AlbumImageEntity)

    @Query("SELECT * FROM album_images_table WHERE albumName = :albumName LIMIT 1")
    suspend fun getAlbumImageByName(albumName: String): AlbumImageEntity?

    @Query("SELECT * FROM album_images_table")
    fun getAllAlbumImages(): Flow<List<AlbumImageEntity>>

    @Query("SELECT * FROM album_images_table")
    suspend fun getAllAlbumImagesSync(): List<AlbumImageEntity>
}