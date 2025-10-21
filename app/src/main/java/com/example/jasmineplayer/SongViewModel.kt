package com.example.jasmineplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SongViewModel(app: Application) : AndroidViewModel(app) {

    private val songRepository = SongRepository.getInstance(app)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()

    private val _songsLoaded = MutableStateFlow(false)
    val songsLoaded = _songsLoaded.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.DATE_ADDED)
    val sortType = _sortType.asStateFlow()

    private val _sortAscending = MutableStateFlow(false)
    val sortAscending = _sortAscending.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)

    init {
        loadSongs()
    }

    fun changeSortType(sortType: SortType) {
        _sortType.update { sortType }
        loadSongs()
    }

    fun toggleSortOrder() {
        _sortAscending.update { !it }
        loadSongs()
    }

    fun loadSongs(folderPath: String? = null, searchQuery: String? = null) {
        _songsLoaded.value = false
        _searchQuery.update { searchQuery }
        viewModelScope.launch {
            val songList = songRepository.getSongs(_sortType.value, _sortAscending.value, folderPath, _searchQuery.value)
            _songs.value = songList
            _songsLoaded.value = true
        }
    }
}
