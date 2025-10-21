package com.example.jasmineplayer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favoritesViewModel: FavoritesViewModel = viewModel(),
    playerViewModel: PlayerViewModel, // Added PlayerViewModel
    onSongClick: (Song) -> Unit
) {
    val favoriteSongs by favoritesViewModel.favoriteSongs.collectAsState()
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsState()

    LazyColumn(modifier = modifier) {
        items(favoriteSongs) { song ->
            val isCurrentlyPlaying = song.id.toString() == currentMediaItem?.mediaId
            SongListItem(
                song = song,
                isCurrentlyPlaying = isCurrentlyPlaying,
                onClick = { onSongClick(song) })
        }
    }
}
