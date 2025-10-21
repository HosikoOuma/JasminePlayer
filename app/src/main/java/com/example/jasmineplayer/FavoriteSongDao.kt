package com.example.jasmineplayer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favoriteSong: FavoriteSong)

    @Delete
    suspend fun delete(favoriteSong: FavoriteSong)

    @Query("SELECT * FROM favorite_songs")
    fun getAll(): Flow<List<FavoriteSong>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songId = :songId)")
    fun isFavorite(songId: Long): Flow<Boolean>
}