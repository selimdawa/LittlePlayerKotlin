package com.flatcode.littleplayer.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.Artist
import com.flatcode.littleplayer.model.Folder
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MusicViewModel @Inject constructor(private val repository: MusicRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _songsSortOrder = MutableStateFlow(DATA.SORT_BY_DATE)
    val songsSortOrder: StateFlow<String> = _songsSortOrder.asStateFlow()

    private val _albumsSortOrder = MutableStateFlow(DATA.SORT_BY_NAME)
    val albumsSortOrder: StateFlow<String> = _albumsSortOrder.asStateFlow()

    private val _event = MutableSharedFlow<MusicEvent>()
    val event: SharedFlow<MusicEvent> = _event.asSharedFlow()

    private val allSongs = _songsSortOrder.flatMapLatest { order ->
        repository.getSongsFlow(order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMusicFiles: StateFlow<List<MusicFiles>> = combine(
        allSongs, _searchQuery
    ) { songs: List<MusicFiles>, query: String ->
        if (query.isEmpty()) songs else {
            songs.filter { it.title?.lowercase()?.contains(query.lowercase()) == true }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumFiles: StateFlow<List<MusicFiles>> = combine(
        allSongs, _albumsSortOrder, _searchQuery
    ) { songs: List<MusicFiles>, sortOrder: String, query: String ->
        val albumMap = mutableMapOf<String, Pair<Int, MusicFiles>>()

        for (song in songs) {
            val albumName = song.album ?: DATA.UNKNOWN
            val current = albumMap[albumName]
            if (current == null) {
                albumMap[albumName] = Pair(1, song)
            } else {
                albumMap[albumName] = Pair(current.first + 1, current.second)
            }
        }

        val uniqueAlbums = albumMap.map { (name, pair) ->
            pair.second.copy(songsCount = pair.first)
        }

        val filtered = if (query.isEmpty()) uniqueAlbums else {
            uniqueAlbums.filter { it.album?.lowercase()?.contains(query.lowercase()) == true }
        }

        when (sortOrder) {
            DATA.SORT_BY_NAME -> filtered.sortedBy { it.album?.lowercase() }
            DATA.SORT_BY_DATE -> filtered.sortedByDescending { it.dateAdded }
            DATA.SORT_BY_SONG_COUNT -> filtered.sortedByDescending { it.songsCount }
            else -> filtered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderFiles: StateFlow<List<Folder>> = combine(
        allSongs, _searchQuery
    ) { songs: List<MusicFiles>, query: String ->
        val foldersMap = HashMap<String, Triple<String, Int, MusicFiles?>>()

        for (song in songs) {
            val pathString = song.path ?: continue
            val file = File(pathString)
            val parentFile = file.parentFile
            if (parentFile != null) {
                val folderPath = parentFile.absolutePath + "/"
                val folderName = parentFile.name
                val currentData = foldersMap[folderPath]

                if (currentData == null) {
                    foldersMap[folderPath] = Triple(folderName, 1, song)
                } else {
                    foldersMap[folderPath] =
                        Triple(currentData.first, currentData.second + 1, currentData.third)
                }
            }
        }

        val foldersList = foldersMap.map { (path, data) ->
            Folder(
                id = path,
                name = data.first,
                path = path,
                songsCount = data.second,
                sampleSongId = data.third?.id,
                sampleSongPath = data.third?.path
            )
        }

        val sorted = foldersList.sortedBy { it.name.lowercase() }
        if (query.isEmpty()) sorted else {
            sorted.filter { it.name.lowercase().contains(query.lowercase()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistFiles: StateFlow<List<Artist>> = combine(
        allSongs, _searchQuery
    ) { songs: List<MusicFiles>, query: String ->
        val artistsMap = HashMap<String, Pair<Int, MusicFiles?>>()

        for (song in songs) {
            val artistName = song.artist ?: DATA.UNKNOWN
            val currentData = artistsMap[artistName]
            if (currentData == null) {
                artistsMap[artistName] = Pair(1, song)
            } else {
                artistsMap[artistName] = Pair(currentData.first + 1, currentData.second)
            }
        }

        val artistsList = artistsMap.map { (name, data) ->
            Artist(
                name = name,
                songsCount = data.first,
                sampleSongId = data.second?.id,
                sampleSongPath = data.second?.path
            )
        }

        val sorted = artistsList.sortedBy { it.name.lowercase() }
        if (query.isEmpty()) sorted else {
            sorted.filter { it.name.lowercase().contains(query.lowercase()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getSortOrder(DATA.SONGS).collect { _songsSortOrder.value = it }
        }
        viewModelScope.launch {
            repository.getSortOrder(DATA.ALBUMS).collect { _albumsSortOrder.value = it }
        }

        // Start background art caching when songs are loaded
        viewModelScope.launch {
            allSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.startBackgroundArtCaching(songs)
                    }
                }
            }
        }
    }

    fun loadAudioData() {
        viewModelScope.launch {
            repository.getAllAudio(DATA.SORT_BY_DATE)
        }
    }

    fun filterSongs(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(category: String, sortType: String) {
        viewModelScope.launch {
            repository.saveSortOrder(category, sortType)
        }
    }

    fun smartShuffle(category: String) {
        viewModelScope.launch {
            val fullList = repository.getAllAudio(DATA.SORT_BY_DATE)
            val songs = when (category) {
                DATA.SONGS -> fullList.shuffled()
                DATA.ALBUMS -> {
                    val album = albumFiles.value.randomOrNull()?.album
                    fullList.filter { it.album == album }
                }

                DATA.ARTISTS -> {
                    val artist = artistFiles.value.randomOrNull()?.name
                    fullList.filter { it.artist == artist }
                }

                DATA.FOLDERS -> {
                    val folder = folderFiles.value.randomOrNull()?.path
                    fullList.filter { it.path?.startsWith(folder ?: "") == true }
                }

                else -> emptyList()
            }

            if (songs.isNotEmpty()) {
                updateCurrentPlaylist(songs)
                _event.emit(MusicEvent.PlaySong(0))
            }
        }
    }

    fun updateCurrentPlaylist(songs: List<MusicFiles>) {
        repository.updateCurrentPlaylist(songs)
    }

    fun deleteSong(song: MusicFiles) {
        viewModelScope.launch {
            song.id?.let {
                repository.deleteFromDatabase(it)
                _event.emit(MusicEvent.SongDeleted(song))
            }
        }
    }

    fun getSongUri(songId: String): Uri = repository.getSongUri(songId)

    fun updateMetadata(songId: String, title: String, artist: String, album: String?) {
        viewModelScope.launch {
            repository.updateMetadata(songId, title, artist, album)
        }
    }

    fun addExcludedFolder(path: String) {
        viewModelScope.launch {
            repository.addExcludedFolder(path)
        }
    }

    fun removeExcludedFolder(path: String) {
        viewModelScope.launch {
            repository.removeExcludedFolder(path)
        }
    }

    val excludedFolders: StateFlow<Set<String>> = repository.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
}

sealed class MusicEvent {
    data class SongDeleted(val song: MusicFiles) : MusicEvent()
    data class Error(val message: String) : MusicEvent()
    data class PlaySong(val position: Int) : MusicEvent()
}