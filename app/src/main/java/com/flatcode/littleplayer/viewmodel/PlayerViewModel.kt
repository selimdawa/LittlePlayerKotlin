package com.flatcode.littleplayer.viewmodel

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.data.entity.FavoriteEntity
import com.flatcode.littleplayer.data.entity.RecentEntity
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.repository.MusicRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Random
import javax.inject.Inject
import androidx.datastore.preferences.core.intPreferencesKey

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val musicRepository: MusicRepository,
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0: OFF, 1: ONE, 2: ALL
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackCycleMode = MutableStateFlow(0) // 0: Normal, 1: One, 2: Random
    val playbackCycleMode: StateFlow<Int> = _playbackCycleMode.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _currentSong = MutableStateFlow<MusicFiles?>(null)
    val currentSong: StateFlow<MusicFiles?> = _currentSong.asStateFlow()

    var listSongs: List<MusicFiles> = emptyList()
    var position = -1
    var uri: Uri? = null

    private val shuffleKey = booleanPreferencesKey("SHUFFLE_MODE")
    private val repeatModeKey = intPreferencesKey("REPEAT_MODE_INT")
    private val playbackCycleModeKey = intPreferencesKey("PLAYBACK_CYCLE_MODE")

    init {
        viewModelScope.launch {
            val preferences = dataStore.data.first()
            _isShuffle.value = preferences[shuffleKey] ?: false
            _repeatMode.value = preferences[repeatModeKey] ?: 0
            _playbackCycleMode.value = preferences[playbackCycleModeKey] ?: 0
        }

        viewModelScope.launch {
            musicRepository.currentPlaylist.collect {
                listSongs = it
            }
        }
    }

    fun toggleShuffle() {
        val newValue = !(_isShuffle.value ?: false)
        _isShuffle.value = newValue
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[shuffleKey] = newValue
            }
        }
    }

    fun togglePlaybackCycle() {
        val current = _playbackCycleMode.value ?: 0
        val next = (current + 1) % 3
        updatePlaybackCycleMode(next)
    }

    fun updatePlaybackCycleFromController(repeatMode: Int, shuffleEnabled: Boolean) {
        val mode = when {
            shuffleEnabled -> 2
            repeatMode == 1 -> 1 // REPEAT_MODE_ONE
            else -> 0
        }
        if (_playbackCycleMode.value != mode) {
            _playbackCycleMode.value = mode
            savePlaybackCycleMode(mode)
        }
    }

    private fun updatePlaybackCycleMode(mode: Int) {
        _playbackCycleMode.value = mode
        savePlaybackCycleMode(mode)
    }

    private fun savePlaybackCycleMode(mode: Int) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[playbackCycleModeKey] = mode
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

    fun updatePositionAndSong(newPosition: Int) {
        if (listSongs.isNotEmpty() && newPosition in listSongs.indices) {
            position = newPosition
            val song = listSongs[position]
            uri = Uri.parse(song.path)
            _currentSong.value = song
            song.id?.let { songId ->
                checkFavorite(songId)
                viewModelScope.launch {
                    repository.insertRecent(
                        RecentEntity(
                            songId = songId,
                            title = song.title ?: "Unknown",
                            artist = song.artist,
                            album = song.album,
                            albumId = song.albumId,
                            duration = song.duration,
                            path = song.path ?: ""
                        )
                    )
                }
            }
        }
    }

    private fun getRandom(max: Int): Int {
        return if (max > 0) Random().nextInt(max + 1) else 0
    }

    suspend fun getSongById(songId: String) = repository.getSongById(songId)

    fun updateWaveform(songId: String, waveform: String) {
        viewModelScope.launch {
            repository.updateWaveform(songId, waveform)
        }
    }
}