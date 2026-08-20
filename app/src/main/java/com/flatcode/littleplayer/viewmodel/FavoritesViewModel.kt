package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _favoriteSongs = MutableStateFlow<List<MusicFiles>>(emptyList())
    val favoriteSongs: StateFlow<List<MusicFiles>> = _favoriteSongs.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getFavoriteSongs().distinctUntilChanged(),
                repository.excludedFolders.distinctUntilChanged()
            ) { favorites, excluded ->
                favorites.asSequence().filter { song ->
                        excluded.none { excludedPath -> song.path.startsWith(excludedPath) }
                    }.map { song ->
                        MusicFiles(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            album = MusicFiles.getCleanedAlbum(song.album, song.path),
                            duration = song.duration.toString(),
                            path = song.path,
                            albumId = song.albumId,
                            cachedImagePath = song.cachedImagePath,
                            dominantColor = song.dominantColor,
                            vibrantColor = song.vibrantColor
                        )
                    }.toList()
            }.flowOn(Dispatchers.Default).collect {
                    _favoriteSongs.value = it
                }
        }
    }
}