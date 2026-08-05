package com.flatcode.littleplayer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equalizer_table")
data class EqualizerEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val bassStrength: Short = 0,
    val virtualizerStrength: Short = 0,
    val bandLevels: String = "0,0,0,0,0", // Stored as comma-separated shorts
    val presetName: String = "Custom"
)