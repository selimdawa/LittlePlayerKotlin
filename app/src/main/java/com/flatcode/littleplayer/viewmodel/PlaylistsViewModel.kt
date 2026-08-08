package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.data.entity.PlaylistEntity
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.model.Playlist
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val repository: MusicRoomRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _playlistsSortOrder = MutableStateFlow(DATA.SORT_BY_NAME)
    val playlistsSortOrder: StateFlow<String> = _playlistsSortOrder.asStateFlow()

    private val _event = MutableSharedFlow<PlaylistsEvent>()
    val event = _event.asSharedFlow()

    init {
        syncPlaylists()

        viewModelScope.launch {
            musicRepository.getSortOrder(DATA.PLAYLISTS).collect {
                _playlistsSortOrder.value = it
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            combine(
                repository.getAllPlaylistNames().flatMapLatest { names ->
                    if (names.isEmpty()) flowOf(emptyList())
                    else combine(names.map { name ->
                        repository.getSongsFromPlaylist(name).map { songs ->
                            val realSongs = songs.filter { it.songId.isNotEmpty() }
                            Playlist(name, realSongs.size, realSongs.firstOrNull()?.path)
                        }
                    }) { it.toList() }
                },
                _playlistsSortOrder
            ) { playlists, sortOrder ->
                when (sortOrder) {
                    DATA.SORT_BY_NAME -> playlists.sortedBy { it.name.lowercase() }
                    DATA.SORT_BY_SIZE -> playlists.sortedByDescending { it.songCount }
                    else -> playlists
                }
            }.collect {
                _playlists.value = it
            }
        }
    }

    fun shufflePlaylists() {
        val allPlaylists = _playlists.value
        if (allPlaylists.isNotEmpty()) {
            val randomPlaylist = allPlaylists.random()
            viewModelScope.launch {
                repository.getSongsFromPlaylistSync(randomPlaylist.name).let { entities ->
                    val songs = entities.filter { it.songId.isNotEmpty() }.map {
                        MusicFiles(
                            id = it.songId,
                            title = it.title,
                            albumId = it.albumId,
                            artist = it.artist,
                            path = it.path
                        )
                    }
                    if (songs.isNotEmpty()) {
                        musicRepository.updateCurrentPlaylist(songs.shuffled())
                        _event.emit(PlaylistsEvent.PlaySong(0))
                    }
                }
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
            repository.insertToPlaylist(
                PlaylistEntity(
                    playlistName = name,
                    songId = "",
                    title = "",
                    artist = "",
                    path = ""
                )
            )
        }
    }

    fun deletePlaylist(name: String) {
        viewModelScope.launch {
            repository.deletePlaylist(name)
        }
    }

    fun renamePlaylist(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(oldName, newName)
        }
    }

    fun updateSortOrder(category: String, sortType: String) {
        viewModelScope.launch {
            musicRepository.saveSortOrder(category, sortType)
        }
    }

    fun getPlaylistsNotContainingSong(songId: String): Flow<List<String>> {
        return combine(
            repository.getAllPlaylistNames(),
            if (songId.isEmpty()) flowOf(emptyList()) else repository.getPlaylistsContainingSong(songId)
        ) { allNames, containingNames ->
            allNames.filter { it !in containingNames }
        }
    }

    fun syncPlaylists() {
        viewModelScope.launch {
            musicRepository.syncPlaylistsWithMediaStore()
        }
    }
}

sealed class PlaylistsEvent {
    data class PlaySong(val position: Int) : PlaylistsEvent()
}
