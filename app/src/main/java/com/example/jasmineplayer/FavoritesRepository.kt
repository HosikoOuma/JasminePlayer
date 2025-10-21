package com.example.jasmineplayer

import android.app.Application
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(application: Application) {

    private val favoriteSongDao = AppDatabase.getDatabase(application).favoriteSongDao()

    fun isFavorite(songId: Long): Flow<Boolean> {
        return favoriteSongDao.isFavorite(songId)
    }

    suspend fun addToFavorites(songId: Long) {
        favoriteSongDao.insert(FavoriteSong(songId))
    }

    suspend fun removeFromFavorites(songId: Long) {
        favoriteSongDao.delete(FavoriteSong(songId))
    }

    fun getAllFavoriteIds(): Flow<List<FavoriteSong>> {
        return favoriteSongDao.getAll()
    }
}
