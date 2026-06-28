package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _musicFiles = MutableLiveData<List<MusicFiles>>()

    private val _albumFiles = MutableLiveData<List<MusicFiles>>()
    val albumFiles: LiveData<List<MusicFiles>> get() = _albumFiles

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
        }
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