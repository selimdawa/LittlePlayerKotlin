package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
            combine(
                repository.getAllRecent(),
                repository.getAllSongs(),
                repository.getAllAlbumImages(),
                repository.excludedFolders
            ) { recents, allSongs, images, excluded ->
                val songMap = allSongs.associateBy { it.id }
                val imageMap = images.associateBy { it.albumName }
                recents.filter { recent ->
                    excluded.none { excludedPath -> recent.path.startsWith(excludedPath) }
                }.take(20).map { recent ->
                    val song = songMap[recent.songId]
                    MusicFiles(
                        id = recent.songId,
                        title = recent.title,
                        artist = recent.artist,
                        album = recent.album,
                        albumId = recent.albumId,
                        duration = recent.duration,
                        path = recent.path,
                        cachedImagePath = imageMap[recent.album ?: DATA.UNKNOWN]?.imagePath,
                        dominantColor = song?.dominantColor,
                        vibrantColor = song?.vibrantColor
                    )
                }
            }.collect {
                _recentSongs.value = it
            }
        }
    }

    fun removeFromRecent(song: MusicFiles) {
        viewModelScope.launch {
            song.id?.let {
                repository.deleteRecentById(it)
            }
        }
    }

    fun clearRecent() {
        viewModelScope.launch {
            repository.clearRecent()
        }
    }
}