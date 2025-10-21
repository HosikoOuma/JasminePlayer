package com.example.jasmineplayer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteSong::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteSongDao(): FavoriteSongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jasmine_player_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
