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
            combine(
                repository.getRecentSongs().distinctUntilChanged(),
                repository.excludedFolders.distinctUntilChanged()
            ) { recents, excluded ->
                recents.asSequence()
                    .filter { song ->
                        excluded.none { excludedPath -> song.path.startsWith(excludedPath) }
                    }
                    .map { song ->
                        MusicFiles(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            album = song.album ?: DATA.UNKNOWN,
                            duration = song.duration.toString(),
                            path = song.path,
                            albumId = song.albumId,
                            cachedImagePath = song.cachedImagePath,
                            dominantColor = song.dominantColor,
                            vibrantColor = song.vibrantColor
                        )
                    }
                    .toList()
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