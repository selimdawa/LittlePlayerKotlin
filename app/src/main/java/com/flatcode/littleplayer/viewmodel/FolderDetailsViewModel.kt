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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FolderDetailsViewModel @Inject constructor(
    application: Application, private val repository: MusicRepository
) : AndroidViewModel(application) {

    private val _songs = MutableLiveData<List<MusicFiles>>()
    val songs: LiveData<List<MusicFiles>> get() = _songs

    fun filterSongsByFolder(folderPath: String?) {
        if (folderPath.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val allSongs = repository.getAllAudio()

            val targetFolder = File(folderPath).canonicalPath

            val filteredSongs = allSongs.filter { song ->
                if (!song.path.isNullOrEmpty()) {
                    val songFolder = File(song.path).parentFile?.canonicalPath
                    songFolder == targetFolder
                } else {
                    false
                }
            }

            _songs.postValue(filteredSongs)
        }
    }
}