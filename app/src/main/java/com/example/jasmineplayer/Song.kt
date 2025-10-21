package com.example.jasmineplayer

import android.net.Uri

data class Song(
    val id: Long,
    val uri: Uri,
    val name: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val albumArtUri: Uri?,
    val lyrics: String? = null,
    val dateAdded: Long, // Added for sorting
    val dateModified: Long // Added for sorting
) {
    val displayName: String
        get() {
            val extensions = listOf(".mp3", ".flac", ".ogg", ".m4a", ".opus")
            for (ext in extensions) {
                if (name.endsWith(ext, ignoreCase = true)) {
                    return name.substringBeforeLast(ext)
                }
            }
            return name
        }
}
