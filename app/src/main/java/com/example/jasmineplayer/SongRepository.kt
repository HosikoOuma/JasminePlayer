package com.example.jasmineplayer

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongRepository(private val application: Application) {

    suspend fun getSongs(
        sortType: SortType,
        sortAscending: Boolean,
        folderPath: String? = null,
        searchQuery: String? = null
    ): List<Song> {
        return withContext(Dispatchers.IO) {
            val songList = mutableListOf<Song>()
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED
            )

            val selectionClauses = mutableListOf<String>()
            selectionClauses.add("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            selectionClauses.add("${MediaStore.Audio.Media.DURATION} >= ?")

            val selectionArgs = mutableListOf<String>()
            selectionArgs.add("5000")

            folderPath?.let {
                selectionClauses.add("${MediaStore.Audio.Media.DATA} LIKE ?")
                selectionArgs.add("$it%")
            }

            if (!searchQuery.isNullOrEmpty()) {
                selectionClauses.add("${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?")
                selectionArgs.add("%$searchQuery%")
            }

            val selection = selectionClauses.joinToString(separator = " AND ")

            val sortOrderColumn = when (sortType) {
                SortType.NAME -> MediaStore.Audio.Media.DISPLAY_NAME
                SortType.DATE_ADDED -> MediaStore.Audio.Media.DATE_ADDED
                SortType.DATE_MODIFIED -> MediaStore.Audio.Media.DATE_MODIFIED
            }
            val sortOrderDirection = if (sortAscending) "ASC" else "DESC"
            val sortOrder = "$sortOrderColumn $sortOrderDirection"

            val query = application.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs.toTypedArray(),
                sortOrder
            )

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val artist = cursor.getString(artistColumn)
                    val album = cursor.getString(albumColumn)
                    val duration = cursor.getInt(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    songList.add(Song(id, contentUri, name, artist, album, duration, albumArtUri, null, dateAdded, dateModified))
                }
            }
            songList
        }
    }

    suspend fun getSongsByIds(ids: Set<Long>, searchQuery: String? = null): List<Song> {
        if (ids.isEmpty()) return emptyList()

        return withContext(Dispatchers.IO) {
            val songList = mutableListOf<Song>()
            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED
            )

            val selectionClauses = mutableListOf<String>()
            selectionClauses.add("${MediaStore.Audio.Media._ID} IN (${Array(ids.size) { "?" }.joinToString()})")
            val selectionArgs = ids.map { it.toString() }.toMutableList()

            if (!searchQuery.isNullOrEmpty()) {
                selectionClauses.add("${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?")
                selectionArgs.add("%$searchQuery%")
            }

            val selection = selectionClauses.joinToString(separator = " AND ")

            val query = application.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs.toTypedArray(),
                null // No specific sort order, will be sorted later
            )

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val artist = cursor.getString(artistColumn)
                    val album = cursor.getString(albumColumn)
                    val duration = cursor.getInt(durationColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                    songList.add(Song(id, contentUri, name, artist, album, duration, albumArtUri, null, dateAdded, dateModified))
                }
            }
            // We need to preserve the order from the favorites list if needed, but for now, just return the list.
            // If you need a specific order, you might need to sort them here based on the original `ids` list.
            songList
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SongRepository? = null

        fun getInstance(application: Application): SongRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SongRepository(application)
                INSTANCE = instance
                instance
            }
        }
    }
}
