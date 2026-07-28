package com.flatcode.littleplayer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flatcode.littleplayer.data.dao.AlbumImageDao
import com.flatcode.littleplayer.data.dao.MusicDao
import com.flatcode.littleplayer.data.dao.SongDao
import com.flatcode.littleplayer.data.entity.AlbumImageEntity
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.data.entity.PlaylistEntity
import com.flatcode.littleplayer.data.entity.RecentEntity
import com.flatcode.littleplayer.data.entity.SongEntity

@Database(
    entities = [SongEntity::class, AlbumImageEntity::class, FavoriteEntity::class, PlaylistEntity::class, RecentEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumImageDao(): AlbumImageDao
    abstract fun musicDao(): MusicDao
}