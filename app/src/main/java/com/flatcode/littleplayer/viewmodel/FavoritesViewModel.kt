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
class FavoritesViewModel @Inject constructor(
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _favoriteSongs = MutableStateFlow<List<MusicFiles>>(emptyList())
    val favoriteSongs: StateFlow<List<MusicFiles>> = _favoriteSongs.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllFavorites(),
                repository.getAllSongs(),
                repository.getAllAlbumImages(),
                repository.excludedFolders
            ) { favorites, allSongs, images, excluded ->
                val songMap = allSongs.associateBy { it.id }
                val imageMap = images.associateBy { it.albumName }
                favorites.filter { fav ->
                    excluded.none { excludedPath -> fav.path.startsWith(excludedPath) }
                }.map { fav ->
                    val song = songMap[fav.songId]
                    MusicFiles(
                        id = fav.songId,
                        title = fav.title,
                        artist = fav.artist,
                        album = fav.album,
                        albumId = fav.albumId,
                        duration = fav.duration,
                        path = fav.path,
                        cachedImagePath = imageMap[fav.album ?: DATA.UNKNOWN]?.imagePath,
                        dominantColor = song?.dominantColor,
                        vibrantColor = song?.vibrantColor
                    )
                }
            }.collect {
                _favoriteSongs.value = it
            }
        }
    }
}