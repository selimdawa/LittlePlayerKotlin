package com.flatcode.littleplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import java.util.ArrayList

class AlbumDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val _albumSongs = MutableLiveData<ArrayList<MusicFiles>>()
    val albumSongs: LiveData<ArrayList<MusicFiles>> get() = _albumSongs

    fun filterSongsByAlbum(albumName: String?) {
        val filteredList = ArrayList<MusicFiles>()
        val allSongs = repository.getAllAudio()

        if (albumName != null) {
            for (song in allSongs) {
                if (albumName == song.album) {
                    filteredList.add(song)
                }
            }
        }
        _albumSongs.value = filteredList
    }
}