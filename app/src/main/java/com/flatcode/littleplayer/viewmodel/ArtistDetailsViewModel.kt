package com.flatcode.littleplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    application: Application, private val repository: MusicRepository
) : AndroidViewModel(application) {

    private val _songs = MutableLiveData<List<MusicFiles>>()
    val songs: LiveData<List<MusicFiles>> get() = _songs

    fun filterSongsByArtist(artistName: String?) {
        if (artistName.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val allSongs = repository.getAllAudio()
            val filteredSongs = allSongs.filter {
                it.artist?.equals(artistName, ignoreCase = true) == true
            }
            _songs.postValue(filteredSongs)
        }
    }
}