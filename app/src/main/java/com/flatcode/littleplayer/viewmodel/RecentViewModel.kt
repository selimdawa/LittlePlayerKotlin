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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecentViewModel @Inject constructor(
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _recentSongs = MutableStateFlow<List<MusicFiles>>(emptyList())
    val recentSongs: StateFlow<List<MusicFiles>> = _recentSongs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllRecent()
                .distinctUntilChanged()
                .flatMapLatest { recents ->
                    val recentIds = recents.map { it.songId }
                    combine(
                        repository.getSongsByIds(recentIds).distinctUntilChanged(),
                        repository.getAllAlbumImages().distinctUntilChanged(),
                        repository.excludedFolders.distinctUntilChanged()
                    ) { allSongs, images, excluded ->
                        val filteredSongMap = allSongs.associateBy { it.id }
                        val imageMap = images.associateBy { it.albumName }

                        recents.asSequence()
                            .filter { recent ->
                                excluded.none { excludedPath -> recent.path.startsWith(excludedPath) }
                            }
                            .take(20)
                            .map { recent ->
                                val song = filteredSongMap[recent.songId]
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
                            .toList()
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect {
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