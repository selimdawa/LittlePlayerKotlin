package com.flatcode.littleplayer.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.activity.MainActivity
import com.flatcode.littleplayer.model.MusicFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayerViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _currentPlayingSong = MutableLiveData<MusicFiles?>()
    val currentPlayingSong: LiveData<MusicFiles?> get() = _currentPlayingSong

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val musicFileKey = stringPreferencesKey("STORED_MUSIC")
    private val artistNameKey = stringPreferencesKey("ARTIST NAME")
    private val songNameKey = stringPreferencesKey("SONG NAME")

    init {
        loadLastPlayedSong()
    }

    private fun loadLastPlayedSong() {
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                val path = preferences[musicFileKey]
                if (!path.isNullOrEmpty()) {
                    val song = MusicFiles(
                        path = path,
                        artist = preferences[artistNameKey],
                        title = preferences[songNameKey]
                    )
                    _currentPlayingSong.postValue(song)
                }
            }
        }
    }

    fun updatePlaybackState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun saveAndBroadcastNextSong(song: MusicFiles) {
        _currentPlayingSong.value = song

        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[musicFileKey] = song.path ?: ""
                preferences[artistNameKey] = song.artist ?: "Unknown"
                preferences[songNameKey] = song.title ?: "Unknown"
            }
        }

        MainActivity.SHOW_MINI_PLAYER = true
        MainActivity.PATH_TO_FRAG = song.path
        MainActivity.ARTIST_TO_FRAG = song.artist
        MainActivity.SONG_NAME_TO_FRAG = song.title
    }
}