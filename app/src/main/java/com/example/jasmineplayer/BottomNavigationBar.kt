package com.example.jasmineplayer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BottomNavigationBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Folder, contentDescription = "Folders") },
            label = { Text("Folders") },
            selected = currentScreen == Screen.Folders,
            onClick = { onScreenSelected(Screen.Folders) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.MusicNote, contentDescription = "Tracks") },
            label = { Text("Tracks") },
            selected = currentScreen == Screen.SongList,
            onClick = { onScreenSelected(Screen.SongList) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites") },
            selected = currentScreen == Screen.Favorites,
            onClick = { onScreenSelected(Screen.Favorites) }
        )
    }
}
