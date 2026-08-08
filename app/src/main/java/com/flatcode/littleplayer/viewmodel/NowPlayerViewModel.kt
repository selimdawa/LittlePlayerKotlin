package com.flatcode.littleplayer.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    private val _themeColorMode = MutableStateFlow(DATA.MODE_BASIC)
    val themeColorMode: StateFlow<Int> = _themeColorMode.asStateFlow()

    private val _bottomPlayerThemeEnabled = MutableStateFlow(false)
    val bottomPlayerThemeEnabled: StateFlow<Boolean> = _bottomPlayerThemeEnabled.asStateFlow()

    private val musicFileKey = stringPreferencesKey(DATA.MUSIC_FILE)
    private val artistNameKey = stringPreferencesKey(DATA.ARTIST_NAME)
    private val songNameKey = stringPreferencesKey(DATA.SONG_NAME)
    private val songIdKey = stringPreferencesKey(DATA.SONG_ID)
    private val albumIdKey = stringPreferencesKey(DATA.ALBUM_ID)
    private val cachedImagePathKey = stringPreferencesKey(DATA.CACHED_IMAGE_PATH)
    private val themeExtractedColorKey = intPreferencesKey(DATA.THEME_EXTRACTED_COLOR)
    private val bottomPlayerThemeKey = booleanPreferencesKey(DATA.BOTTOM_PLAYER_THEME)
    private val themeColorModeKey = intPreferencesKey(DATA.THEME_COLOR_MODE)

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val queue = repository.loadCurrentQueue()
            if (queue.isNotEmpty() && repository.currentPlaylist.value.isEmpty()) {
                repository.updateCurrentPlaylist(queue, saveToRoom = false)
            }

            val playbackState = repository.getPlaybackStateSync()
            if (playbackState != null && !playbackState.currentSongId.isNullOrEmpty()) {
                val songInQueue = queue.find { it.id == playbackState.currentSongId }
                if (songInQueue != null) {
                    _currentPlayingSong.value = songInQueue
                }
            }
            
            // Collect everything in one place to avoid multiple suspending collectors
            dataStore.data.collect { preferences ->
                // 1. Theme Settings
                _themeColorMode.value = preferences[themeColorModeKey] ?: DATA.MODE_BASIC
                _bottomPlayerThemeEnabled.value = preferences[bottomPlayerThemeKey] ?: false
                _currentThemeColor.value = preferences[themeExtractedColorKey]

                // 2. Song Recovery (if not already loaded from playback state)
                if (_currentPlayingSong.value == null) {
                    val path = preferences[musicFileKey]
                    if (!path.isNullOrEmpty()) {
                        _currentPlayingSong.value = MusicFiles(
                            path = path,
                            artist = preferences[artistNameKey] ?: DATA.UNKNOWN,
                            title = preferences[songNameKey] ?: DATA.UNKNOWN,
                            id = preferences[songIdKey],
                            albumId = preferences[albumIdKey],
                            cachedImagePath = preferences[cachedImagePathKey]
                        )
                    }
                }
            }
        }
    }

    fun updatePlaybackState(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updateThemeColor(color: Int) {
        _currentThemeColor.value = color
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[themeExtractedColorKey] = color
            }
        }
    }

    fun setThemeColorMode(mode: Int) {
        _themeColorMode.value = mode
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[themeColorModeKey] = mode
            }
        }
    }

    fun setBottomPlayerThemeEnabled(enabled: Boolean) {
        _bottomPlayerThemeEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[bottomPlayerThemeKey] = enabled
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
                preferences[songIdKey] = song.id ?: ""
                preferences[albumIdKey] = song.albumId ?: ""
                preferences[cachedImagePathKey] = song.cachedImagePath ?: ""
            }
        }
    }
}