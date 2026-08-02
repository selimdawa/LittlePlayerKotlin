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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MusicViewModel @Inject constructor(private val repository: MusicRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _albumFiles = MutableStateFlow<List<MusicFiles>>(emptyList())
    val albumFiles: StateFlow<List<MusicFiles>> = _albumFiles.asStateFlow()

    private val _folderFiles = MutableStateFlow<List<Folder>>(emptyList())
    val folderFiles: StateFlow<List<Folder>> = _folderFiles.asStateFlow()

    private val _artistFiles = MutableStateFlow<List<Artist>>(emptyList())
    val artistFiles: StateFlow<List<Artist>> = _artistFiles.asStateFlow()

    private val _filteredMusicFiles = MutableStateFlow<List<MusicFiles>>(emptyList())
    val filteredMusicFiles: StateFlow<List<MusicFiles>> = _filteredMusicFiles.asStateFlow()

    private val _songsSortOrder = MutableStateFlow(DATA.SORT_BY_DATE)
    val songsSortOrder: StateFlow<String> = _songsSortOrder.asStateFlow()

    private val _albumsSortOrder = MutableStateFlow(DATA.SORT_BY_NAME)
    val albumsSortOrder: StateFlow<String> = _albumsSortOrder.asStateFlow()

    private val _artistSortOrder = MutableStateFlow(DATA.SORT_BY_NAME)
    val artistSortOrder: StateFlow<String> = _artistSortOrder.asStateFlow()

    private val _folderSortOrder = MutableStateFlow(DATA.SORT_BY_NAME)
    val folderSortOrder: StateFlow<String> = _folderSortOrder.asStateFlow()

    private val _event = MutableSharedFlow<MusicEvent>()
    val event: SharedFlow<MusicEvent> = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getSortOrder(DATA.SONGS).collect { _songsSortOrder.value = it }
        }
        viewModelScope.launch {
            repository.getSortOrder(DATA.ALBUMS).collect { _albumsSortOrder.value = it }
        }
        viewModelScope.launch {
            repository.getSortOrder(DATA.ARTISTS).collect { _artistSortOrder.value = it }
        }
        viewModelScope.launch {
            repository.getSortOrder(DATA.FOLDERS).collect { _folderSortOrder.value = it }
        }

        viewModelScope.launch {
            combine(
                repository.getSongsFlow(DATA.SORT_BY_DATE), _searchQuery
            ) { songs, query -> songs }.collect { songs ->
                processAuxiliaryLists(songs)
            }
        }

        viewModelScope.launch {
            _songsSortOrder.flatMapLatest { order ->
                repository.getSongsFlow(order)
            }.collect { songs ->
                val query = _searchQuery.value
                _filteredMusicFiles.value = if (query.isEmpty()) songs else {
                    songs.filter { it.title?.lowercase()?.contains(query.lowercase()) == true }
                }
            }
        }
    }

    private fun processAuxiliaryLists(allAudio: List<MusicFiles>) {
        viewModelScope.launch {
            combine(_albumsSortOrder, _artistSortOrder, _folderSortOrder) { alSort, arSort, foSort ->
                Triple(alSort, arSort, foSort)
            }.collect { (alSort, arSort, foSort) ->
                val uniqueAlbums = ArrayList<MusicFiles>()
                val duplicates = HashSet<String>()

                for (song in allAudio) {
                    val albumName = song.album ?: DATA.UNKNOWN
                    if (!duplicates.contains(albumName)) {
                        uniqueAlbums.add(song)
                        duplicates.add(albumName)
                    }
                }

                _albumFiles.value = when (alSort) {
                    DATA.SORT_BY_NAME -> uniqueAlbums.sortedBy { it.album?.lowercase() }
                    DATA.SORT_BY_DATE -> uniqueAlbums.sortedByDescending { it.dateAdded }
                    else -> uniqueAlbums
                }

                generateFolderList(allAudio, foSort)
                generateArtistList(allAudio, arSort)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.startBackgroundArtCaching(allAudio)
        }
    }

    fun loadAudioData() {
        viewModelScope.launch {
            repository.getAllAudio(DATA.SORT_BY_DATE)
        }
    }

    private fun generateFolderList(songsList: List<MusicFiles>, sortOrder: String) {
        val foldersMap = HashMap<String, Triple<String, Int, MusicFiles?>>()

        for (song in songsList) {
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

        _folderFiles.value = when (sortOrder) {
            DATA.SORT_BY_NAME -> foldersList.sortedBy { it.name.lowercase() }
            else -> foldersList.sortedBy { it.name.lowercase() }
        }
    }

    private fun generateArtistList(songsList: List<MusicFiles>, sortOrder: String) {
        val artistsMap = HashMap<String, Pair<Int, MusicFiles?>>()

        for (song in songsList) {
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

        _artistFiles.value = when (sortOrder) {
            DATA.SORT_BY_NAME -> artistsList.sortedBy { it.name.lowercase() }
            else -> artistsList.sortedBy { it.name.lowercase() }
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
            // Use the full list for category-based shuffling to avoid being restricted by search
            val fullList = repository.getAllAudio(DATA.SORT_BY_DATE)
            val songs = when (category) {
                DATA.SONGS -> fullList.shuffled()
                DATA.ALBUMS -> {
                    val album = _albumFiles.value.randomOrNull()?.album
                    fullList.filter { it.album == album }
                }
                DATA.ARTISTS -> {
                    val artist = _artistFiles.value.randomOrNull()?.name
                    fullList.filter { it.artist == artist }
                }
                DATA.FOLDERS -> {
                    val folder = _folderFiles.value.randomOrNull()?.path
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
}

sealed class MusicEvent {
    data class SongDeleted(val song: MusicFiles) : MusicEvent()
    data class Error(val message: String) : MusicEvent()
    data class PlaySong(val position: Int) : MusicEvent()
}