package com.example.jasmineplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flow

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val favoritesRepository = FavoritesRepository(application)
    private val songRepository = SongRepository.getInstance(application)

    private val _sortType = MutableStateFlow(SortType.DATE_ADDED)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _sortAscending = MutableStateFlow(false)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)

    val favoriteSongs: StateFlow<List<Song>> = combine(
        favoritesRepository.getAllFavoriteIds(),
        _sortType,
        _sortAscending,
        _searchQuery
    ) { favoriteEntities, sortType, ascending, searchQuery ->
        Triple(favoriteEntities.map { it.songId }.toSet(), sortType, ascending)
    }.flatMapLatest { (ids, type, ascending) ->
        flow {
            val songs = if (ids.isNotEmpty()) songRepository.getSongsByIds(ids, _searchQuery.value) else emptyList()
            emit(sortSongs(songs, type, ascending))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun changeSortType(sortType: SortType) {
        _sortType.update { sortType }
    }

    fun toggleSortOrder() {
        _sortAscending.update { !it }
    }

    fun loadFavorites(searchQuery: String? = null) {
        _searchQuery.update { searchQuery }
    }

    private fun sortSongs(songs: List<Song>, sortType: SortType, ascending: Boolean): List<Song> {
        return when (sortType) {
            SortType.NAME -> if (ascending) songs.sortedBy { it.name } else songs.sortedByDescending { it.name }
            SortType.DATE_ADDED -> if (ascending) songs.sortedBy { it.dateAdded } else songs.sortedByDescending { it.dateAdded }
            SortType.DATE_MODIFIED -> if (ascending) songs.sortedBy { it.dateModified } else songs.sortedByDescending { it.dateModified }
        }
    }
}