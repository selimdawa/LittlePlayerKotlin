package com.flatcode.littleplayer.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayerViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _currentPlayingSong = MutableStateFlow<MusicFiles?>(null)
    val currentPlayingSong: StateFlow<MusicFiles?> = _currentPlayingSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val musicFileKey = stringPreferencesKey("STORED_MUSIC")
    private val artistNameKey = stringPreferencesKey("ARTIST NAME")
    private val songNameKey = stringPreferencesKey("SONG NAME")
    private val albumIdKey = stringPreferencesKey("ALBUM ID")
    private val cachedImagePathKey = stringPreferencesKey("CACHED_IMAGE_PATH")

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
                        title = preferences[songNameKey],
                        albumId = preferences[albumIdKey],
                        cachedImagePath = preferences[cachedImagePathKey]
                    )
                    _currentPlayingSong.value = song
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
                preferences[artistNameKey] = song.artist ?: DATA.UNKNOWN
                preferences[songNameKey] = song.title ?: DATA.UNKNOWN
                preferences[albumIdKey] = song.albumId ?: ""
                preferences[cachedImagePathKey] = song.cachedImagePath ?: ""
            }
        }
    }
}