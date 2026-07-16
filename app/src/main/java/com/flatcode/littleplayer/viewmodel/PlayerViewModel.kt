package com.flatcode.littleplayer.viewmodel

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Random
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>, private val repository: MusicRoomRepository
) : ViewModel() {

    private val _isShuffle = MutableLiveData(false)
    val isShuffle: LiveData<Boolean> get() = _isShuffle

    private val _repeatMode = MutableLiveData(0) // 0: OFF, 1: ONE, 2: ALL
    val repeatMode: LiveData<Int> get() = _repeatMode

    private val _playbackCycleMode = MutableLiveData(0) // 0: Normal, 1: One, 2: Random
    val playbackCycleMode: LiveData<Int> get() = _playbackCycleMode

    private val _isFavorite = MutableLiveData(false)
    val isFavorite: LiveData<Boolean> get() = _isFavorite

    private val _currentSong = MutableLiveData<MusicFiles?>()
    val currentSong: LiveData<MusicFiles?> get() = _currentSong

    var listSongs = ArrayList<MusicFiles>()
    var position = -1
    var uri: Uri? = null

    private val shuffleKey = booleanPreferencesKey("SHUFFLE_MODE")
    private val repeatModeKey =
        androidx.datastore.preferences.core.intPreferencesKey("REPEAT_MODE_INT")
    private val playbackCycleModeKey =
        androidx.datastore.preferences.core.intPreferencesKey("PLAYBACK_CYCLE_MODE")

    init {
        viewModelScope.launch {
            val preferences = dataStore.data.first()
            _isShuffle.value = preferences[shuffleKey] ?: false
            _repeatMode.value = preferences[repeatModeKey] ?: 0
            _playbackCycleMode.value = preferences[playbackCycleModeKey] ?: 0
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
                    com.flatcode.littleplayer.data.entity.FavoriteEntity(
                        songId,
                        song.safeTitle,
                        song.safeArtist,
                        song.album,
                        song.duration,
                        song.path ?: ""
                    )
                )
                _isFavorite.postValue(false)
            } else {
                repository.insertFavorite(
                    com.flatcode.littleplayer.data.entity.FavoriteEntity(
                        songId,
                        song.safeTitle,
                        song.safeArtist,
                        song.album,
                        song.duration,
                        song.path ?: ""
                    )
                )
                _isFavorite.postValue(true)
            }
        }
    }

    fun checkFavorite(songId: String) {
        viewModelScope.launch {
            _isFavorite.postValue(repository.isFavorite(songId))
        }
    }

    fun updatePositionAndSong(newPosition: Int) {
        if (listSongs.isNotEmpty() && newPosition in listSongs.indices) {
            position = newPosition
            val song = listSongs[position]
            uri = Uri.parse(song.path)
            _currentSong.value = song
            song.id?.let { checkFavorite(it) }
        }
    }

    private fun getRandom(max: Int): Int {
        return if (max > 0) Random().nextInt(max + 1) else 0
    }
}