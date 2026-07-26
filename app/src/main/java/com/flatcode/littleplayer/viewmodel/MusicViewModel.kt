package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.Artist
import com.flatcode.littleplayer.model.Folder
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import com.linc.amplituda.Amplituda
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(private val repository: MusicRepository) : ViewModel() {

    private val _musicFiles = MutableStateFlow<List<MusicFiles>>(emptyList())

    private val _albumFiles = MutableStateFlow<List<MusicFiles>>(emptyList())
    val albumFiles: StateFlow<List<MusicFiles>> = _albumFiles.asStateFlow()

    private val _folderFiles = MutableStateFlow<List<Folder>>(emptyList())
    val folderFiles: StateFlow<List<Folder>> = _folderFiles.asStateFlow()

    private val _artistFiles = MutableStateFlow<List<Artist>>(emptyList())
    val artistFiles: StateFlow<List<Artist>> = _artistFiles.asStateFlow()

    private val _filteredMusicFiles = MutableStateFlow<List<MusicFiles>>(emptyList())
    val filteredMusicFiles: StateFlow<List<MusicFiles>> = _filteredMusicFiles.asStateFlow()

    private val _sortOrder = MutableStateFlow("")
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    val currentPlaylist: StateFlow<List<MusicFiles>> = repository.currentPlaylist

    init {
        viewModelScope.launch {
            repository.sortOrderFlow.collect { order ->
                _sortOrder.value = order
                loadAudioData()
            }
        }
    }

    fun loadAudioData() {
        viewModelScope.launch {
            val allAudio = repository.getAllAudio()

            withContext(Dispatchers.Default) {
                val uniqueAlbums = ArrayList<MusicFiles>()
                val duplicates = HashSet<String>()

                for (song in allAudio) {
                    val albumName = song.album ?: "Unknown"
                    if (!duplicates.contains(albumName)) {
                        uniqueAlbums.add(song)
                        duplicates.add(albumName)
                    }
                }

                _musicFiles.value = allAudio
                _filteredMusicFiles.value = allAudio
                _albumFiles.value = uniqueAlbums

                generateFolderList(allAudio)
                generateArtistList(allAudio)
            }

            // startBackgroundWaveformAnalysis(allAudio)
        }
    }

    private fun startBackgroundWaveformAnalysis(songs: List<MusicFiles>) {
        viewModelScope.launch(Dispatchers.IO) {
            val amplituda = Amplituda(repository.context)
            songs.forEach { song ->
                if (song.waveform == null && song.path != null && song.id != null) {
                    try {
                        val result = amplituda.processAudio(song.path).get()
                        val amplitudesArray = result.amplitudesAsList().toIntArray()
                        if (amplitudesArray.isNotEmpty()) {
                            val waveformString = amplitudesArray.joinToString(",")
                            repository.updateWaveform(song.id, waveformString)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun generateFolderList(songsList: List<MusicFiles>) {
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
        }.sortedBy { it.name }

        _folderFiles.value = foldersList
    }

    private fun generateArtistList(songsList: List<MusicFiles>) {
        val artistsMap = HashMap<String, Pair<Int, MusicFiles?>>()

        for (song in songsList) {
            val artistName = song.artist ?: "Unknown"
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
        }.sortedBy { it.name }

        _artistFiles.value = artistsList
    }

    fun filterSongs(query: String) {
        val userInput = query.lowercase()
        val allTracks = _musicFiles.value ?: return

        if (userInput.isEmpty()) {
            _filteredMusicFiles.value = allTracks
            return
        }

        val matchingTracks = allTracks.filter {
            it.title?.lowercase()?.contains(userInput) == true
        }
        _filteredMusicFiles.value = matchingTracks
    }

    fun updateSortOrder(sortType: String) {
        viewModelScope.launch {
            repository.saveSortOrder(sortType)
        }
    }

    fun updateCurrentPlaylist(songs: List<MusicFiles>) {
        repository.updateCurrentPlaylist(songs)
    }
}