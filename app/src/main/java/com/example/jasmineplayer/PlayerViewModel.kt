package com.example.jasmineplayer

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private var mediaController: MediaController? = null
    private var originalPlaylistBeforeShuffle: List<MediaItem>? = null
    private val favoritesRepository = FavoritesRepository(app)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _currentSongMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentSong: StateFlow<MediaMetadata?> = _currentSongMetadata.asStateFlow()

    val currentSongTitle: StateFlow<String> = _currentSongMetadata.map {
        it?.title?.toString() ?: "Unknown Title"
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, "Unknown Title")

    val currentSongArtist: StateFlow<String> = _currentSongMetadata.map {
        it?.artist?.toString() ?: "Unknown Artist"
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, "Unknown Artist")

    val currentSongArtworkUri: StateFlow<Uri?> = _currentSongMetadata.map {
        it?.artworkUri
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)

    val isCurrentSongFavorite: StateFlow<Boolean> = _currentMediaItem.flatMapLatest { item ->
        item?.mediaId?.toLongOrNull()?.let { favoritesRepository.isFavorite(it) } ?: flowOf(false)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, false)

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    // This state now reflects our custom shuffle mode
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(-1)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics: StateFlow<String?> = _currentLyrics.asStateFlow()

    init {
        val sessionToken = SessionToken(app, ComponentName(app, PlayerService::class.java))
        val controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                // The controller's shuffle mode must be OFF because we manage it manually.
                mediaController?.shuffleModeEnabled = false
                mediaController?.addListener(playerListener)
                updateState()
            },
            MoreExecutors.directExecutor()
        )

        viewModelScope.launch {
            while (true) {
                _currentPosition.value = mediaController?.currentPosition ?: 0L
                delay(200)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            // Whenever the player state changes, we update our UI state
            updateState()
        }
    }

    private fun updateState() {
        mediaController?.let {
            val newPlaylist = mutableListOf<MediaItem>()
            for (i in 0 until it.mediaItemCount) {
                newPlaylist.add(it.getMediaItemAt(i))
            }
            _playlist.value = newPlaylist

            _isPlaying.value = it.isPlaying
            _currentMediaItem.value = it.currentMediaItem
            _currentSongMetadata.value = it.mediaMetadata
            _totalDuration.value = it.duration.coerceAtLeast(0L)
            _repeatMode.value = it.repeatMode
            _currentSongIndex.value = it.currentMediaItemIndex
        }
    }

    fun play(songs: List<Song>, startIndex: Int) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.displayName)
                        .setArtist(song.artist)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                )
                .build()
        }
        mediaController?.setMediaItems(mediaItems, startIndex, 0)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun playSongFromUri(uri: Uri) {
        viewModelScope.launch {
            val mediaItem = createMediaItemFromUri(uri)
            mediaController?.setMediaItem(mediaItem)
            mediaController?.prepare()
            mediaController?.play()
        }
    }

    private suspend fun createMediaItemFromUri(uri: Uri): MediaItem {
        return withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                val app = getApplication<Application>()

                val fileExtension = run {
                    val mimeType = app.contentResolver.getType(uri)
                    var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    if (ext.isNullOrBlank()) {
                        ext = uri.lastPathSegment?.substringAfterLast('.', "")
                    }
                    if (!ext.isNullOrBlank()) ".$ext" else ".mp3"
                }

                tempFile = File.createTempFile("temp_audio_intent_", fileExtension, app.cacheDir)

                app.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IOException("Could not open song file (InputStream was null).")

                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tag

                val title = tag?.getFirst(FieldKey.TITLE)
                val artist = tag?.getFirst(FieldKey.ARTIST)
                val artworkData = tag?.firstArtwork?.binaryData

                var artworkUri: Uri? = null
                if (artworkData != null) {
                    val artworkFile = File.createTempFile("artwork_", ".jpg", app.cacheDir)
                    artworkFile.deleteOnExit() // Try to clean up album art file
                    FileOutputStream(artworkFile).use { it.write(artworkData) }
                    artworkUri = Uri.fromFile(artworkFile)
                }

                val metadataBuilder = MediaMetadata.Builder()
                val finalTitle = if (!title.isNullOrBlank()) {
                    title
                } else {
                    uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Unknown Title"
                }
                metadataBuilder.setTitle(finalTitle)
                metadataBuilder.setArtist(if (artist.isNullOrBlank()) "Unknown Artist" else artist)
                metadataBuilder.setArtworkUri(artworkUri)

                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(uri.toString())
                    .setMediaMetadata(metadataBuilder.build())
                    .build()

            } catch (e: Exception) {
                // Fallback for any error during metadata extraction
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(uri.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Unknown Title")
                            .setArtist("Unknown Artist")
                            .build()
                    )
                    .build()
            } finally {
                tempFile?.delete()
            }
        }
    }

    fun playSongFromQueue(index: Int) {
        mediaController?.seekTo(index, 0L)
        mediaController?.play()
    }

    fun moveSongInQueue(fromIndex: Int, toIndex: Int) {
        // Just tell the controller to move the item. The listener will handle the UI update.
        mediaController?.moveMediaItem(fromIndex, toIndex)
    }

    fun toggleFavorite() {
        val songId = _currentMediaItem.value?.mediaId?.toLongOrNull() ?: return
        viewModelScope.launch {
            if (isCurrentSongFavorite.value) {
                favoritesRepository.removeFromFavorites(songId)
            } else {
                favoritesRepository.addToFavorites(songId)
            }
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun playNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun playPrevious() {
        if ((mediaController?.currentPosition ?: 0) > 3000) {
            mediaController?.seekTo(0)
        } else {
            mediaController?.seekToPreviousMediaItem()
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun toggleShuffleMode() {
        val shouldEnable = !_isShuffleEnabled.value
        _isShuffleEnabled.value = shouldEnable

        mediaController?.let { controller ->
            val currentPosition = controller.currentPosition

            if (shouldEnable) {
                val currentPlaylist = (0 until controller.mediaItemCount).map { controller.getMediaItemAt(it) }
                val currentIndex = controller.currentMediaItemIndex

                if (currentIndex != -1 && currentPlaylist.isNotEmpty()) {
                    originalPlaylistBeforeShuffle = currentPlaylist

                    val currentItem = currentPlaylist[currentIndex]
                    val otherItems = currentPlaylist.toMutableList().apply {
                        removeAt(currentIndex)
                    }
                    otherItems.shuffle()

                    val newPlaylist = listOf(currentItem) + otherItems

                    controller.setMediaItems(newPlaylist, 0, currentPosition)
                }
            } else {
                originalPlaylistBeforeShuffle?.let { originalPlaylist ->
                    val currentMediaId = controller.currentMediaItem?.mediaId
                    val newIndex = originalPlaylist.indexOfFirst { it.mediaId == currentMediaId }

                    controller.setMediaItems(originalPlaylist, if (newIndex != -1) newIndex else 0, currentPosition)
                    originalPlaylistBeforeShuffle = null
                }
            }
            controller.prepare()
            controller.play()
        }
    }

    fun toggleRepeatMode() {
        mediaController?.repeatMode = when (mediaController?.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun loadLyrics() {
        viewModelScope.launch {
            val mediaItem = _currentMediaItem.value ?: return@launch
            _currentLyrics.value = "Loading..."
            val lyrics = withContext(Dispatchers.IO) {
                var tempFile: File? = null
                try {
                    val uri = mediaItem.localConfiguration?.uri ?: return@withContext "No URI found"
                    val app = getApplication<Application>()

                    val fileExtension = run {
                        val mimeType = app.contentResolver.getType(uri)
                        var ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        if (ext.isNullOrBlank()) {
                            ext = uri.lastPathSegment?.substringAfterLast('.', "")
                        }
                        if (!ext.isNullOrBlank()) ".$ext" else ".mp3"
                    }

                    tempFile = File.createTempFile("temp_audio_", fileExtension, app.cacheDir)

                    app.contentResolver.openInputStream(uri)?.use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: return@withContext "Could not open song file (InputStream was null)."

                    val audioFile = AudioFileIO.read(tempFile)
                    val tag = audioFile.tag ?: return@withContext "Could not read any tags from the file."

                    val extractedLyrics = tag.getFirst(FieldKey.LYRICS)

                    if (extractedLyrics.isNullOrBlank()) {
                        "No embedded lyrics found in the file."
                    } else {
                        extractedLyrics
                    }
                } catch (e: Exception) {
                    "Error reading lyrics: ${e.javaClass.simpleName} - ${e.message}"
                } finally {
                    tempFile?.delete()
                }
            }
            _currentLyrics.value = lyrics
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}
