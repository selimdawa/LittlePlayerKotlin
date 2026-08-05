package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.data.entity.PlaylistEntity
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.model.Playlist
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _playlistNames = MutableStateFlow<List<String>>(emptyList())
    val playlistNames: StateFlow<List<String>> = _playlistNames.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllPlaylistNames().collect {
                _playlistNames.value = it
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            repository.getAllPlaylistNames().flatMapLatest { names ->
                if (names.isEmpty()) flowOf(emptyList())
                else combine(names.map { name ->
                    repository.getSongsFromPlaylist(name).map { songs ->
                        Playlist(name, songs.size, songs.firstOrNull()?.path)
                    }
                }) { it.toList() }
            }.collect {
                _playlists.value = it
            }
        }
    }

    fun addToPlaylist(playlistName: String, song: MusicFiles) {
        viewModelScope.launch {
            repository.insertToPlaylist(
                PlaylistEntity(
                    playlistName = playlistName,
                    songId = song.id ?: "",
                    title = song.title ?: "",
                    artist = song.artist ?: DATA.UNKNOWN,
                    path = song.path ?: ""
                )
            )
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            // Room will handle distinct names via the @Insert REPLACE or just adding one item
            // Here we just insert a dummy or a first song if needed, but usually we just want the name to exist.
            // For now, let's keep it simple.
        }
    }
}