package com.example.jasmineplayer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(playerViewModel: PlayerViewModel, onBack: () -> Unit) {
    val playlist by playerViewModel.playlist.collectAsState()
    val currentSongIndex by playerViewModel.currentSongIndex.collectAsState()
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current

    var draggingItem by remember { mutableStateOf<MediaItem?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(currentSongIndex, playlist) { // React to playlist changes as well
        if (currentSongIndex != -1 && currentSongIndex < playlist.size) {
            listState.animateScrollToItem(currentSongIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Up Next") },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            itemsIndexed(playlist, key = { _, song -> song.mediaId }) { index, song ->
                val isCurrentlyPlaying = index == currentSongIndex
                val isBeingDragged = song == draggingItem

                val rowModifier = if (isBeingDragged) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer { // Apply visual effects for the dragged item
                            translationY = dragOffset
                            shadowElevation = 8f
                        }
                } else {
                    Modifier
                }

                Row(
                    modifier = rowModifier
                        .fillMaxWidth()
                        .background(if (isCurrentlyPlaying) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { playerViewModel.playSongFromQueue(index) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.mediaMetadata.artworkUri != null) {
                            AsyncImage(
                                model = song.mediaMetadata.artworkUri,
                                contentDescription = "Album Art",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "No Album Art",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = song.mediaMetadata.title?.toString() ?: "Unknown",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )

                    IconButton(
                        onClick = {}, // No action on simple click
                        modifier = Modifier.pointerInput(song) { // Keyed pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingItem = song
                                },
                                onDrag = { change, dragAmount ->
                                    if (song == draggingItem) {
                                        change.consume()
                                        dragOffset += dragAmount.y
                                    }
                                },
                                onDragEnd = {
                                    val fromItem = draggingItem
                                    if (fromItem != null) {
                                        val fromIndex = playlist.indexOf(fromItem)
                                        if (fromIndex != -1) {
                                            val draggedItemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == fromIndex }
                                            if (draggedItemInfo != null) {
                                                val center = draggedItemInfo.offset + draggedItemInfo.size / 2 + dragOffset.toInt()
                                                val toIndex = listState.layoutInfo.visibleItemsInfo
                                                    .filterNot { it.index == fromIndex } // Exclude the dragged item itself
                                                    .minByOrNull { abs(it.offset + it.size / 2 - center) }?.index

                                                if (toIndex != null && fromIndex != toIndex) {
                                                    playerViewModel.moveSongInQueue(fromIndex, toIndex)
                                                }
                                            }
                                        }
                                    }
                                    // Reset drag state
                                    draggingItem = null
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    // Reset drag state on cancellation
                                    draggingItem = null
                                    dragOffset = 0f
                                }
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
