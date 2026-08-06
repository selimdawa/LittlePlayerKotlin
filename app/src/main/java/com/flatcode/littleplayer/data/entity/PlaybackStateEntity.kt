package com.flatcode.littleplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state_table")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1,
    val currentSongId: String? = null,
    val lastPosition: Int = -1,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: Int = 0, // Player.REPEAT_MODE_OFF = 0, Player.REPEAT_MODE_ONE = 1, Player.REPEAT_MODE_ALL = 2
    val lastProgress: Long = 0L
)
