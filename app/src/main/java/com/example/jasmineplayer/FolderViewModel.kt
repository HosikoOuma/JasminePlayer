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

    // Keep track of the original unfiltered and unsorted list
    private var originalFolders = listOf<Folder>()

    init {
        loadFolders()
    }

    fun changeSortType(sortType: FolderSortType) {
        _sortType.update { sortType }
        applySortingAndFiltering()
    }

    fun toggleSortOrder() {
        _sortAscending.update { !it }
        applySortingAndFiltering()
    }

    fun loadFolders(searchQuery: String? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val folderMap = mutableMapOf<String, Pair<String, Int>>() // BUCKET_ID -> (Path, Count)
                val folderNames = mutableMapOf<String, String>() // BUCKET_ID -> Name

                val projection = arrayOf(
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.BUCKET_ID,
                    MediaStore.Audio.Media.BUCKET_DISPLAY_NAME
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

                getApplication<Application>().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    null
                )?.use { cursor ->
                    val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_ID)
                    val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val bucketId = cursor.getString(bucketIdColumn)
                        val path = cursor.getString(dataColumn)
                        val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"

                        if (bucketId != null && path != null) {
                            val folder = folderMap[bucketId]
                            if (folder == null) {
                                File(path).parent?.let { parentPath ->
                                    folderMap[bucketId] = Pair(parentPath, 1)
                                    folderNames[bucketId] = bucketName
                                }
                            } else {
                                folderMap[bucketId] = folder.copy(second = folder.second + 1)
                            }
                        }
                    }
                }

                originalFolders = folderMap.map { (bucketId, folderData) ->
                    val name = folderNames[bucketId] ?: "Unknown"
                    Folder(name, folderData.first, folderData.second)
                }
            }
            applySortingAndFiltering(searchQuery)
        }
    }

    private fun applySortingAndFiltering(searchQuery: String? = null) {
        val filteredList = if (searchQuery.isNullOrEmpty()) {
            originalFolders
        } else {
            originalFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        val sortedList = when (_sortType.value) {
            FolderSortType.NAME -> {
                if (_sortAscending.value) {
                    filteredList.sortedBy { it.name }
                } else {
                    filteredList.sortedByDescending { it.name }
                }
            }
            FolderSortType.SONG_COUNT -> {
                if (_sortAscending.value) {
                    filteredList.sortedBy { it.songCount }
                } else {
                    filteredList.sortedByDescending { it.songCount }
                }
            }
        }
        _folders.value = sortedList
    }
}
