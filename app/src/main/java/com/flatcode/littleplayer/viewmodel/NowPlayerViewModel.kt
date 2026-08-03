package com.flatcode.littleplayer.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.repository.MusicRepository
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
    private val dataStore: DataStore<Preferences>, private val repository: MusicRepository
) : ViewModel() {

    private val _currentPlayingSong = MutableStateFlow<MusicFiles?>(null)
    val currentPlayingSong: StateFlow<MusicFiles?> = _currentPlayingSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentThemeColor = MutableStateFlow<Int?>(null)
    val currentThemeColor: StateFlow<Int?> = _currentThemeColor.asStateFlow()

    private val _isPaletteMode = MutableStateFlow(false)
    val isPaletteMode: StateFlow<Boolean> = _isPaletteMode.asStateFlow()

    private val musicFileKey = stringPreferencesKey(DATA.MUSIC_FILE)
    private val artistNameKey = stringPreferencesKey(DATA.ARTIST_NAME)
    private val songNameKey = stringPreferencesKey(DATA.SONG_NAME)
    private val albumIdKey = stringPreferencesKey(DATA.ALBUM_ID)
    private val cachedImagePathKey = stringPreferencesKey(DATA.CACHED_IMAGE_PATH)
    private val paletteModeKey = booleanPreferencesKey(DATA.PALETTE_MODE)

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val queue = repository.loadCurrentQueue()
            if (queue.isNotEmpty()) {
                repository.updateCurrentPlaylist(queue)
            }

            dataStore.data.collect { preferences ->
                val path = preferences[musicFileKey]
                if (!path.isNullOrEmpty()) {
                    val song = MusicFiles(
                        path = path,
                        artist = preferences[artistNameKey] ?: DATA.UNKNOWN,
                        title = preferences[songNameKey] ?: DATA.UNKNOWN,
                        albumId = preferences[albumIdKey],
                        cachedImagePath = preferences[cachedImagePathKey]
                    )
                    _currentPlayingSong.value = song
                }
                _isPaletteMode.value = preferences[paletteModeKey] ?: false
            }
        }
    }

    fun updatePlaybackState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updateThemeColor(color: Int) {
        _currentThemeColor.value = color
    }

    fun setPaletteMode(active: Boolean) {
        _isPaletteMode.value = active
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[paletteModeKey] = active
            }
        }
    }

    suspend fun getCurrentQueue(): List<MusicFiles> {
        return repository.loadCurrentQueue()
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