package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<MusicFiles>>(emptyList())
    val songs: StateFlow<List<MusicFiles>> = _songs.asStateFlow()

    fun loadSongs(playlistName: String) {
        viewModelScope.launch {
            repository.getSongsFromPlaylist(playlistName).collect { entities ->
                _songs.value = entities.filter { it.songId.isNotEmpty() }.map {
                    MusicFiles(
                        id = it.songId,
                        title = it.title,
                        albumId = it.albumId,
                        artist = it.artist,
                        path = it.path,
                        color = it.color
                    )
                }
            }
        }
    }

    fun updateSongColor(songId: String, color: Int) {
        viewModelScope.launch {
            repository.updateSongColor(songId, color)
        }
    }
}