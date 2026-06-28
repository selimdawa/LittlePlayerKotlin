package com.flatcode.littleplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)

    private val _musicFiles = MutableLiveData<List<MusicFiles>>()

    private val _albumFiles = MutableLiveData<List<MusicFiles>>()
    val albumFiles: LiveData<List<MusicFiles>> get() = _albumFiles

    private val _filteredMusicFiles = MutableLiveData<List<MusicFiles>>()
    val filteredMusicFiles: LiveData<List<MusicFiles>> get() = _filteredMusicFiles

    fun loadAudioData() {
        val allAudio = repository.getAllAudio()
        _musicFiles.value = allAudio
        _filteredMusicFiles.value = allAudio

        val uniqueAlbums = ArrayList<MusicFiles>()
        val duplicates = HashSet<String>()
        for (song in allAudio) {
            val albumName = song.album ?: "Unknown"
            if (!duplicates.contains(albumName)) {
                uniqueAlbums.add(song)
                duplicates.add(albumName)
            }
        }
        _albumFiles.value = uniqueAlbums
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
        repository.saveSortOrder(sortType)
    }
}