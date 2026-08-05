package com.flatcode.littleplayer.viewmodel

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.repository.MusicRoomRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val musicRepository: MusicRepository,
    private val repository: MusicRoomRepository,
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _currentSong = MutableStateFlow<MusicFiles?>(null)
    val currentSong: StateFlow<MusicFiles?> = _currentSong.asStateFlow()

    var listSongs: List<MusicFiles> = emptyList()
    var position = -1
    var uri: Uri? = null

    init {
        viewModelScope.launch {
            musicRepository.currentPlaylist.collect {
                listSongs = it
                if (it.isNotEmpty()) {
                    if (position != -1) {
                        updatePositionAndSong(position)
                    } else if (_currentSong.value != null) {
                        val index = it.indexOfFirst { s -> s.path == _currentSong.value?.path }
                        if (index != -1) {
                            updatePositionAndSong(index)
                        }
                    }
                }
            }
        }

        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                if (position == -1) {
                    val savedPos = preferences[intPreferencesKey(DATA.LAST_POSITION)] ?: -1
                    if (savedPos != -1) {
                        position = savedPos
                    }
                }

                if (_currentSong.value == null) {
                    val path = preferences[stringPreferencesKey(DATA.MUSIC_FILE)]
                    if (!path.isNullOrEmpty()) {
                        val song = MusicFiles(
                            path = path,
                            artist = preferences[stringPreferencesKey(DATA.ARTIST_NAME)],
                            title = preferences[stringPreferencesKey(DATA.SONG_NAME)],
                            albumId = preferences[stringPreferencesKey(DATA.ALBUM_ID)],
                            cachedImagePath = preferences[stringPreferencesKey(DATA.CACHED_IMAGE_PATH)],
                            lyrics = preferences[stringPreferencesKey(DATA.LYRICS)]
                        )
                        if (_currentSong.value == null) {
                            _currentSong.value = song
                        }
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val song = _currentSong.value ?: return
        val songId = song.id ?: return
        viewModelScope.launch {
            val isFav = repository.isFavorite(songId)
            if (isFav) {
                repository.deleteFavorite(
                    FavoriteEntity(
                        songId = songId,
                        title = song.safeTitle,
                        artist = song.safeArtist,
                        album = song.album,
                        albumId = song.albumId,
                        duration = song.duration,
                        lyrics = song.lyrics,
                        path = song.path ?: ""
                    )
                )
                _isFavorite.value = false
            } else {
                repository.insertFavorite(
                    FavoriteEntity(
                        songId = songId,
                        title = song.safeTitle,
                        artist = song.safeArtist,
                        album = song.album,
                        albumId = song.albumId,
                        duration = song.duration,
                        lyrics = song.lyrics,
                        path = song.path ?: ""
                    )
                )
                _isFavorite.value = true
            }
        }
    }

    fun checkFavorite(songId: String) {
        viewModelScope.launch {
            _isFavorite.value = repository.isFavorite(songId)
        }
    }

    fun updatePositionAndSong(newPosition: Int, forceUpdate: Boolean = false) {
        if (position != newPosition || forceUpdate || _currentSong.value == null) {
            position = newPosition
            if (listSongs.isNotEmpty() && (newPosition in listSongs.indices)) {
                val song = listSongs[position]
                uri = Uri.parse(song.path)
                _currentSong.value = song
                song.id?.let { songId ->
                    checkFavorite(songId)
                }
            }
        }
    }

    suspend fun getSongById(songId: String) = repository.getSongById(songId)

    fun updateWaveform(songId: String, waveform: String) {
        viewModelScope.launch {
            repository.updateWaveform(songId, waveform)
        }
    }

    fun updateLyrics(songId: String, lyrics: String) {
        viewModelScope.launch {
            repository.updateLyrics(songId, lyrics)
            if (_currentSong.value?.id == songId) {
                _currentSong.value = _currentSong.value?.copy(lyrics = lyrics)
            }
        }
    }
}