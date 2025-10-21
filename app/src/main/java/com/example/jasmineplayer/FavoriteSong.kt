package com.example.jasmineplayer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSong(
    @PrimaryKey val songId: Long
)
