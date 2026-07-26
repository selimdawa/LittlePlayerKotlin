package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<MusicFiles>>(emptyList())
    val songs: StateFlow<List<MusicFiles>> = _songs.asStateFlow()

    fun filterSongsByArtist(artistName: String?) {
        if (artistName.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val allSongs = repository.getAllAudio()
            val filteredSongs = allSongs.filter {
                it.artist?.equals(artistName, ignoreCase = true) == true
            }
            _songs.value = filteredSongs
        }
    }

    fun updateCurrentPlaylist(songs: List<MusicFiles>) {
        repository.updateCurrentPlaylist(songs)
    }
}