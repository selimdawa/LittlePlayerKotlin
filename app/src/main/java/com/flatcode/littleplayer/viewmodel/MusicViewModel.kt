package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.Folder
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _musicFiles = MutableLiveData<List<MusicFiles>>()

    private val _albumFiles = MutableLiveData<List<MusicFiles>>()
    val albumFiles: LiveData<List<MusicFiles>> get() = _albumFiles

    private val _folderFiles = MutableLiveData<List<Folder>>()
    val folderFiles: LiveData<List<Folder>> get() = _folderFiles

    private val _filteredMusicFiles = MutableLiveData<List<MusicFiles>>()
    val filteredMusicFiles: LiveData<List<MusicFiles>> get() = _filteredMusicFiles

    init {
        viewModelScope.launch {
            repository.sortOrderFlow.collect {
                loadAudioData()
            }
        }
    }

    fun loadAudioData() {
        viewModelScope.launch {
            val allAudio = repository.getAllAudio()
            val uniqueAlbums = ArrayList<MusicFiles>()
            val duplicates = HashSet<String>()

            for (song in allAudio) {
                val albumName = song.album ?: "Unknown"
                if (!duplicates.contains(albumName)) {
                    uniqueAlbums.add(song)
                    duplicates.add(albumName)
                }
            }

            _musicFiles.value = allAudio
            _filteredMusicFiles.value = allAudio
            _albumFiles.value = uniqueAlbums

            generateFolderList(allAudio)
        }
    }

    private fun generateFolderList(songsList: List<MusicFiles>) {
        val foldersMap = HashMap<String, Pair<String, Int>>()

        for (song in songsList) {
            val pathString = song.path ?: continue
            val file = File(pathString)
            val parentFile = file.parentFile
            if (parentFile != null) {
                val folderPath = parentFile.absolutePath + "/"
                val folderName = parentFile.name
                val currentData = foldersMap[folderPath]

                if (currentData == null) {
                    foldersMap[folderPath] = Pair(folderName, 1)
                } else {
                    foldersMap[folderPath] = Pair(currentData.first, currentData.second + 1)
                }
            }
        }

        val foldersList = foldersMap.map { (path, data) ->
            Folder(
                id = path,
                name = data.first,
                path = path,
                songsCount = data.second
            )
        }.sortedBy { it.name }

        _folderFiles.value = foldersList
    }

    fun filterSongs(query: String) {
        val userInput = query.lowercase()
        val allTracks = _musicFiles.value ?: return

        if (userInput.isEmpty()) {
            _filteredMusicFiles.value = allTracks
            return
        }

        val matchingTracks = allTracks.filter {
            it.title?.lowercase()?.contains(userInput) == true
        }
        _filteredMusicFiles.value = matchingTracks
    }

    fun updateSortOrder(sortType: String) {
        viewModelScope.launch {
            repository.saveSortOrder(sortType)
        }
    }
}