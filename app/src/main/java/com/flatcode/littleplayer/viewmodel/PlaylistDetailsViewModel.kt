package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    private val repository: MusicRoomRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<MusicFiles>>(emptyList())
    val songs: StateFlow<List<MusicFiles>> = _songs.asStateFlow()

    private val _songsSortOrder = MutableStateFlow(DATA.SORT_BY_DATE)
    val songsSortOrder: StateFlow<String> = _songsSortOrder.asStateFlow()

    private val _playlistName = MutableStateFlow("")

    init {
        viewModelScope.launch {
            musicRepository.getSortOrder(DATA.PLAYLIST_DETAILS).collect {
                _songsSortOrder.value = it
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            _playlistName.flatMapLatest { name ->
                if (name.isEmpty()) flowOf(emptyList())
                else combine(
                    repository.getSongsFromPlaylist(name),
                    _songsSortOrder
                ) { entities, sortOrder ->
                    val musicFiles = entities.filter { it.songId.isNotEmpty() }.map { entity ->
                        val dbSong = repository.getSongById(entity.songId)
                        MusicFiles(
                            id = entity.songId,
                            title = dbSong?.title ?: entity.title,
                            albumId = dbSong?.albumId ?: entity.albumId,
                            artist = dbSong?.artist ?: entity.artist,
                            path = dbSong?.path ?: entity.path,
                            dateAdded = dbSong?.dateAdded ?: 0L,
                            size = dbSong?.size ?: 0L,
                            playCount = dbSong?.playCount ?: 0,
                            year = dbSong?.year ?: 0
                        )
                    }

                    when (sortOrder) {
                        DATA.SORT_BY_NAME -> musicFiles.sortedBy { it.title?.lowercase() }
                        DATA.SORT_BY_DATE -> musicFiles.sortedByDescending { it.dateAdded }
                        DATA.SORT_BY_PLAY_COUNT -> musicFiles.sortedByDescending { it.playCount }
                        DATA.SORT_BY_SIZE -> musicFiles.sortedByDescending { it.size }
                        DATA.SORT_BY_RELEASE_DATE -> musicFiles.sortedByDescending { it.year }
                        else -> musicFiles
                    }
                }
            }.collect {
                _songs.value = it
            }
        }
    }

    fun loadSongs(playlistName: String) {
        _playlistName.value = playlistName
    }

    fun shuffleSongs() {
        val currentSongs = _songs.value
        if (currentSongs.isNotEmpty()) {
            val shuffled = currentSongs.shuffled()
            musicRepository.updateCurrentPlaylist(shuffled)
            viewModelScope.launch {
                _event.emit(PlaylistDetailsEvent.PlaySong(0))
            }
        }
    }

    fun updateSortOrder(category: String, sortType: String) {
        viewModelScope.launch {
            musicRepository.saveSortOrder(category, sortType)
        }
    }

    fun removeSongFromPlaylist(playlistName: String, songId: String) {
        viewModelScope.launch {
            repository.deleteFromPlaylist(playlistName, songId)
        }
    }

    private val _event = MutableSharedFlow<PlaylistDetailsEvent>()
    val event = _event.asSharedFlow()
}

sealed class PlaylistDetailsEvent {
    data class PlaySong(val position: Int) : PlaylistDetailsEvent()
}