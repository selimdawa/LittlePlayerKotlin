package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FolderDetailsViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<MusicFiles>>(emptyList())
    val songs: StateFlow<List<MusicFiles>> = _songs.asStateFlow()

    fun filterSongsByFolder(folderPath: String?) {
        if (folderPath.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val allSongs = repository.getAllAudio(DATA.SORT_BY_NAME)

            val targetFolder = File(folderPath).canonicalPath

            val filteredSongs = allSongs.filter { song ->
                if (!song.path.isNullOrEmpty()) {
                    val songFolder = File(song.path).parentFile?.canonicalPath
                    songFolder == targetFolder
                } else {
                    false
                }
            }

            _songs.value = filteredSongs
        }
    }

    fun updateCurrentPlaylist(songs: List<MusicFiles>) {
        repository.updateCurrentPlaylist(songs)
    }
}