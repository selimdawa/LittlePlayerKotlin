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
                repository.getAllFavorites(), repository.getAllAlbumImages()
            ) { favorites, images ->
                val imageMap = images.associateBy { it.albumName }
                favorites.map {
                    MusicFiles(
                        id = it.songId,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        albumId = it.albumId,
                        duration = it.duration,
                        path = it.path,
                        cachedImagePath = imageMap[it.album ?: DATA.UNKNOWN]?.imagePath
                    )
                }
            }.collect {
                _favoriteSongs.value = it
            }
        }
    }
}