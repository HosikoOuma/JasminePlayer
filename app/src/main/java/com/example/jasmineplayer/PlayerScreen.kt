package com.example.jasmineplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel = viewModel(),
    onDismiss: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentSongTitle by playerViewModel.currentSongTitle.collectAsState()
    val currentSongArtist by playerViewModel.currentSongArtist.collectAsState()
    val currentSongArtworkUri by playerViewModel.currentSongArtworkUri.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val totalDuration by playerViewModel.totalDuration.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()
    val isShuffleEnabled by playerViewModel.isShuffleEnabled.collectAsState()
    val isFavorite by playerViewModel.isCurrentSongFavorite.collectAsState()

    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp.value
    val dismissThreshold = screenHeight / 4

    var wasPlayingBeforeSeeking by remember { mutableStateOf(false) }

    val artworkSize by animateDpAsState(targetValue = if (isPlaying) 320.dp else 280.dp, label = "")
    val buttonCornerRadius by animateDpAsState(targetValue = if (isPlaying) 48.dp else 24.dp, label = "PlayPauseButtonShape")
    val haptic = LocalHapticFeedback.current

    fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .alpha(alpha.value)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetY.value > dismissThreshold) {
                                onDismiss()
                            } else {
                                launch { offsetY.animateTo(0f, tween(300)) }
                                launch { alpha.animateTo(1f, tween(300)) }
                            }
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        coroutineScope.launch {
                            val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                            offsetY.snapTo(newOffset)
                            val newAlpha = (1f - (newOffset / screenHeight)).coerceIn(0.5f, 1f)
                            alpha.snapTo(newAlpha)
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = currentSongArtworkUri,
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Loading Album Art",
                                modifier = Modifier.size(150.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    error = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "No Album Art",
                                modifier = Modifier.size(150.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
            }
            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentSongTitle, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(currentSongArtist, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerViewModel.toggleFavorite()
                }) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            WaveProgressBar(
                progress = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                isAnimating = isPlaying,
                onSeekStart = {
                    wasPlayingBeforeSeeking = isPlaying
                    if (isPlaying) {
                        playerViewModel.pause()
                    }
                },
                onSeek = { progress ->
                    playerViewModel.seekTo((progress * totalDuration).toLong())
                },
                onSeekFinished = {
                    if (wasPlayingBeforeSeeking) {
                        playerViewModel.resume()
                    }
                },
                waveColor = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(formatTime(currentPosition))
                Spacer(modifier = Modifier.weight(1f))
                Text(formatTime(totalDuration))
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerViewModel.playPrevious()
                }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp))
                }
                LargeFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isPlaying) {
                            playerViewModel.pause()
                        } else {
                            playerViewModel.resume()
                        }
                    },
                    shape = RoundedCornerShape(buttonCornerRadius),
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerViewModel.playNext()
                }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerViewModel.toggleShuffleMode()
                }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowQueue()
                }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Queue")
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerViewModel.loadLyrics()
                    onShowLyrics()
                }) {
                    Icon(Icons.Default.Notes, contentDescription = "Show Lyrics")
                }
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerViewModel.toggleRepeatMode()
                }) {
                    Icon(
                        imageVector = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Icons.Default.Repeat
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}
