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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.util.ArrayList

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val _albumSongs = MutableLiveData<ArrayList<MusicFiles>>()
    val albumSongs: LiveData<ArrayList<MusicFiles>> get() = _albumSongs

    fun filterSongsByAlbum(albumName: String?) {
        viewModelScope.launch {
            val filteredList = withContext(Dispatchers.IO) {
                val list = ArrayList<MusicFiles>()
                val allSongs = repository.getAllAudio() ?: emptyList()

                if (albumName != null) {
                    for (song in allSongs) {
                        if (albumName == song.album) {
                            list.add(song)
                        }
                    }
                }
                list
            }
            _albumSongs.value = filteredList
        }
    }
}