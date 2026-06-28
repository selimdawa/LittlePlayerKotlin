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
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _currentPlayingSong = MutableLiveData<MusicFiles?>()
    val currentPlayingSong: LiveData<MusicFiles?> get() = _currentPlayingSong

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val MUSIC_FILE_KEY = stringPreferencesKey("STORED_MUSIC")
    private val ARTIST_NAME_KEY = stringPreferencesKey("ARTIST NAME")
    private val SONG_NAME_KEY = stringPreferencesKey("SONG NAME")

    fun updatePlaybackState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun saveAndBroadcastNextSong(song: MusicFiles) {
        _currentPlayingSong.value = song

        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[MUSIC_FILE_KEY] = song.path ?: ""
                preferences[ARTIST_NAME_KEY] = song.artist ?: "Unknown"
                preferences[SONG_NAME_KEY] = song.title ?: "Unknown"
            }
        }

        MainActivity.SHOW_MINI_PLAYER = true
        MainActivity.PATH_TO_FRAG = song.path
        MainActivity.ARTIST_TO_FRAG = song.artist
        MainActivity.SONG_NAME_TO_FRAG = song.title
    }
}