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
class RecentViewModel @Inject constructor(
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _recentSongs = MutableStateFlow<List<MusicFiles>>(emptyList())
    val recentSongs: StateFlow<List<MusicFiles>> = _recentSongs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllRecent().collect { entities ->
                _recentSongs.value = entities.map {
                    MusicFiles(
                        id = it.songId,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        albumId = it.albumId,
                        duration = it.duration,
                        path = it.path
                    )
                }
            }
        }
    }
}