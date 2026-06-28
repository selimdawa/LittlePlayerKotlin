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
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Random
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _isShuffle = MutableLiveData(false)
    val isShuffle: LiveData<Boolean> get() = _isShuffle

    private val _isRepeat = MutableLiveData(false)
    val isRepeat: LiveData<Boolean> get() = _isRepeat

    private val _currentSong = MutableLiveData<MusicFiles?>()
    val currentSong: LiveData<MusicFiles?> get() = _currentSong

    var listSongs = ArrayList<MusicFiles>()
    var position = -1
    var uri: Uri? = null

    private val SHUFFLE_KEY = booleanPreferencesKey("SHUFFLE_MODE")
    private val REPEAT_KEY = booleanPreferencesKey("REPEAT_MODE")

    init {
        viewModelScope.launch {
            val preferences = dataStore.data.first()
            _isShuffle.value = preferences[SHUFFLE_KEY] ?: false
            _isRepeat.value = preferences[REPEAT_KEY] ?: false
        }
    }

    fun toggleShuffle() {
        val newValue = !(_isShuffle.value ?: false)
        _isShuffle.value = newValue
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SHUFFLE_KEY] = newValue
            }
        }
    }

    fun toggleRepeat() {
        val newValue = !(_isRepeat.value ?: false)
        _isRepeat.value = newValue
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[REPEAT_KEY] = newValue
            }
        }
    }

    fun updatePositionAndSong(newPosition: Int) {
        if (listSongs.isNotEmpty() && newPosition in listSongs.indices) {
            position = newPosition
            val song = listSongs[position]
            uri = Uri.parse(song.path)
            _currentSong.value = song
        }
    }

    fun calculateNextPosition(): Int {
        if (listSongs.isEmpty()) return -1
        return when (_isShuffle.value) {
            true if _isRepeat.value == false -> {
                getRandom(listSongs.size - 1)
            }

            false if _isRepeat.value == false -> {
                (position + 1) % listSongs.size
            }

            else -> {
                position
            }
        }
    }

    fun calculatePrevPosition(): Int {
        if (listSongs.isEmpty()) return -1
        return if (_isShuffle.value == true && _isRepeat.value == false) {
            getRandom(listSongs.size - 1)
        } else if (_isShuffle.value == false && _isRepeat.value == false) {
            if (position - 1 < 0) listSongs.size - 1 else position - 1
        } else {
            position
        }
    }

    private fun getRandom(max: Int): Int {
        return if (max > 0) Random().nextInt(max + 1) else 0
    }
}