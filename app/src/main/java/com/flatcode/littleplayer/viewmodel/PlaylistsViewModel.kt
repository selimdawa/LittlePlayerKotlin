package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.data.entity.PlaylistEntity
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _playlistNames = MutableStateFlow<List<String>>(emptyList())
    val playlistNames: StateFlow<List<String>> = _playlistNames.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllPlaylistNames().collect {
                _playlistNames.value = it
            }
        }
    }

    fun createPlaylist(name: String, song: MusicFiles? = null) {
        viewModelScope.launch {
            if (song != null) {
                repository.insertToPlaylist(
                    PlaylistEntity(
                        playlistName = name,
                        songId = song.id ?: "",
                        title = song.title ?: "",
                        artist = song.artist ?: DATA.UNKNOWN,
                        path = song.path ?: ""
                    )
                )
            }
        }
    }
}