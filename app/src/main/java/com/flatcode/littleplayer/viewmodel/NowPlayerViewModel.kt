package com.flatcode.littleplayer.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.flatcode.littleplayer.activity.MainActivity
import com.flatcode.littleplayer.model.MusicFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NowPlayerViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _currentPlayingSong = MutableLiveData<MusicFiles?>()
    val currentPlayingSong: LiveData<MusicFiles?> get() = _currentPlayingSong

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    fun updatePlaybackState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun saveAndBroadcastNextSong(song: MusicFiles) {
        _currentPlayingSong.value = song

        val app = getApplication<Application>()
        val preferences = app.getSharedPreferences("LAST_PLAYED", Context.MODE_PRIVATE)
        preferences.edit().apply {
            putString("STORED_MUSIC", song.path)
            putString("ARTIST NAME", song.artist)
            putString("SONG NAME", song.title)
            apply()
        }

        MainActivity.SHOW_MINI_PLAYER = true
        MainActivity.PATH_TO_FRAG = song.path
        MainActivity.ARTIST_TO_FRAG = song.artist
        MainActivity.SONG_NAME_TO_FRAG = song.title
    }
}