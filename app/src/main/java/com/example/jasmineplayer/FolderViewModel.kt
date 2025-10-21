package com.example.jasmineplayer

import android.app.Application
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _sortType = MutableStateFlow(FolderSortType.NAME)
    val sortType: StateFlow<FolderSortType> = _sortType.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)

    init {
        loadFolders()
    }

    fun changeSortType(sortType: FolderSortType) {
        _sortType.update { sortType }
        loadFolders()
    }

    fun toggleSortOrder() {
        _sortAscending.update { !it }
        loadFolders()
    }

    fun loadFolders(searchQuery: String? = null) {
        _searchQuery.value = searchQuery
        viewModelScope.launch {
            val folderList = withContext(Dispatchers.IO) {
                val folders = mutableMapOf<String, Folder>()
                val projection = arrayOf(
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = MediaStore.Audio.Media.DATA

                getApplication<Application>().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val path = cursor.getString(dataColumn)
                        val file = File(path)
                        val parentFolder = file.parentFile
                        if (parentFolder != null) {
                            val folderPath = parentFolder.path
                            val folderName = parentFolder.name

                            if (_searchQuery.value.isNullOrEmpty() || folderName.contains(_searchQuery.value!!, true)) {
                                val folder = folders[folderPath]
                                if (folder == null) {
                                    folders[folderPath] = Folder(folderName, folderPath, 1)
                                } else {
                                    folders[folderPath] = folder.copy(songCount = folder.songCount + 1)
                                }
                            }
                        }
                    }
                }
                folders.values.toList()
            }
            _folders.value = sortFolders(folderList)
        }
    }

    private fun sortFolders(folders: List<Folder>): List<Folder> {
        return when (_sortType.value) {
            FolderSortType.NAME -> {
                if (_sortAscending.value) {
                    folders.sortedBy { it.name }
                } else {
                    folders.sortedByDescending { it.name }
                }
            }
            FolderSortType.SONG_COUNT -> {
                if (_sortAscending.value) {
                    folders.sortedBy { it.songCount }
                } else {
                    folders.sortedByDescending { it.songCount }
                }
            }
        }
    }
}
