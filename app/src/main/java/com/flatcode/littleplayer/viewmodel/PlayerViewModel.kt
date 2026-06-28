package com.flatcode.littleplayer.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flatcode.littleplayer.model.MusicFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Random
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _isShuffle = MutableLiveData(false)
    val isShuffle: LiveData<Boolean> get() = _isShuffle

    private val _isRepeat = MutableLiveData(false)
    val isRepeat: LiveData<Boolean> get() = _isRepeat

    private val _currentSong = MutableLiveData<MusicFiles?>()
    val currentSong: LiveData<MusicFiles?> get() = _currentSong

    var listSongs = ArrayList<MusicFiles>()
    var position = -1
    var uri: Uri? = null

    fun toggleShuffle() {
        _isShuffle.value = !(_isShuffle.value ?: false)
    }

    fun toggleRepeat() {
        _isRepeat.value = !(_isRepeat.value ?: false)
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