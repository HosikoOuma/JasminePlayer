package com.example.jasmineplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun SongListScreen(
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel, // Added PlayerViewModel
    onSongClick: (Song) -> Unit
) {
    val songs by songViewModel.songs.collectAsState()
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsState()

    val listState = rememberLazyListState()

    Row {
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(songs) { song ->
                val isCurrentlyPlaying = song.id.toString() == currentMediaItem?.mediaId
                SongListItem(
                    song = song,
                    isCurrentlyPlaying = isCurrentlyPlaying, // Pass the flag down
                    onClick = { onSongClick(song) }
                )
            }
        }
        CustomVerticalScrollbar(listState = listState)
    }
}

@Composable
fun SongListItem(song: Song, isCurrentlyPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = song.albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier.fillMaxSize(),
                error = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No Album Art",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                },
                loading = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Loading",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = song.displayName,
                color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
            Text(
                text = song.artist,
                color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        }
    }
}
