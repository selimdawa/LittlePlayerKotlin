package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AlbumDetailsUiState(
    val songs: List<MusicFiles> = emptyList(),
    val imagePath: String? = null,
    val firstSongId: String? = null,
    val firstSongPath: String? = null,
    val firstSongAlbumId: String? = null
)

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val repository: MusicRepository, private val roomRepository: MusicRoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailsUiState())
    val uiState: StateFlow<AlbumDetailsUiState> = _uiState.asStateFlow()

    fun filterSongsByAlbum(albumName: String?) {
        if (albumName == null) return

        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                val cachedImage = roomRepository.getAlbumImageByName(albumName)?.imagePath
                val allSongs = repository.getAllAudio(DATA.SORT_BY_NAME)

                val filteredList = allSongs.filter { it.album == albumName }
                val firstSong = filteredList.firstOrNull()

                AlbumDetailsUiState(
                    songs = filteredList,
                    imagePath = cachedImage,
                    firstSongId = firstSong?.id,
                    firstSongPath = firstSong?.path,
                    firstSongAlbumId = firstSong?.albumId
                )
            }
            _uiState.value = state
        }
    }

    fun updateCurrentPlaylist(songs: List<MusicFiles>) {
        repository.updateCurrentPlaylist(songs)
    }
}
