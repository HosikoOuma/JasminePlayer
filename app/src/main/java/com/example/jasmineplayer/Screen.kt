package com.example.jasmineplayer

sealed class Screen(val route: String, val title: String) {
    object Folders : Screen("folders", "Folders")
    object SongList : Screen("song_list", "Tracks") {
        fun createRoute(folderPath: String): String {
            val encodedPath = java.net.URLEncoder.encode(folderPath, java.nio.charset.StandardCharsets.UTF_8.toString())
            return "song_list?folderPath=$encodedPath"
        }
    }
    object Favorites : Screen("favorites", "Favorites")
    object Player : Screen("player", "Player")

    companion object {
        fun fromRoute(route: String?): Screen {
            return when {
                route?.startsWith("song_list") == true -> SongList
                route == "folders" -> Folders
                route == "favorites" -> Favorites
                route == "player" -> Player
                else -> SongList
            }
        }
    }
}
