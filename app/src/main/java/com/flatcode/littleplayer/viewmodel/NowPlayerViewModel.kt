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
import com.flatcode.littleplayer.repository.MusicRoomRepository
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
    private val repository: MusicRepository,
    private val roomRepository: MusicRoomRepository
) : ViewModel() {

    private val _currentPlayingSong = MutableStateFlow<MusicFiles?>(null)
    val currentPlayingSong: StateFlow<MusicFiles?> = _currentPlayingSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(value = false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentThemeColor = MutableStateFlow<Int?>(null)
    val currentThemeColor: StateFlow<Int?> = _currentThemeColor.asStateFlow()

    private val _themeColorMode = MutableStateFlow(DATA.MODE_BASIC)
    val themeColorMode: StateFlow<Int> = _themeColorMode.asStateFlow()

    private val _bottomPlayerThemeEnabled = MutableStateFlow(true)
    val bottomPlayerThemeEnabled: StateFlow<Boolean> = _bottomPlayerThemeEnabled.asStateFlow()

    private val _listItemThemeEnabled = MutableStateFlow(true)
    val listItemThemeEnabled: StateFlow<Boolean> = _listItemThemeEnabled.asStateFlow()

    private val _marqueeEnabled = MutableStateFlow(true)
    val marqueeEnabled: StateFlow<Boolean> = _marqueeEnabled.asStateFlow()

    private val musicFileKey = stringPreferencesKey(DATA.MUSIC_FILE)
    private val artistNameKey = stringPreferencesKey(DATA.ARTIST_NAME)
    private val songNameKey = stringPreferencesKey(DATA.SONG_NAME)
    private val albumKey = stringPreferencesKey(DATA.ALBUM)
    private val songIdKey = stringPreferencesKey(DATA.SONG_ID)
    private val albumIdKey = stringPreferencesKey(DATA.ALBUM_ID)
    private val cachedImagePathKey = stringPreferencesKey(DATA.CACHED_IMAGE_PATH)
    private val themeExtractedColorKey = intPreferencesKey(DATA.THEME_EXTRACTED_COLOR)
    private val bottomPlayerThemeKey = booleanPreferencesKey(DATA.BOTTOM_PLAYER_THEME)
    private val listItemThemeKey = booleanPreferencesKey(DATA.LIST_ITEM_THEME)
    private val themeColorModeKey = intPreferencesKey(DATA.THEME_COLOR_MODE)
    private val marqueeEnabledKey = booleanPreferencesKey(DATA.MARQUEE_ENABLED)

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val queue = repository.loadCurrentQueue()
            if (queue.isNotEmpty() && repository.currentPlaylist.value.isEmpty()) {
                repository.updateCurrentPlaylist(queue, saveToRoom = false)
            }

            val playbackState = roomRepository.getPlaybackStateSync()
            if ((playbackState != null) && (!playbackState.currentSongId.isNullOrEmpty())) {
                _currentPlayingSong.value = queue.find { it.id == playbackState.currentSongId }
            }
            
            dataStore.data.collect { preferences ->
                _themeColorMode.value = preferences[themeColorModeKey] ?: DATA.MODE_BASIC
                _bottomPlayerThemeEnabled.value = preferences[bottomPlayerThemeKey] ?: true
                _listItemThemeEnabled.value = preferences[listItemThemeKey] ?: true
                _marqueeEnabled.value = preferences[marqueeEnabledKey] ?: true
                _currentThemeColor.value = preferences[themeExtractedColorKey]

                if (_currentPlayingSong.value == null) {
                    val path = preferences[musicFileKey]
                    if (!path.isNullOrEmpty()) {
                        _currentPlayingSong.value = MusicFiles(
                            path = path,
                            artist = preferences[artistNameKey] ?: DATA.UNKNOWN,
                            title = preferences[songNameKey] ?: DATA.UNKNOWN,
                            album = preferences[albumKey] ?: DATA.UNKNOWN,
                            duration = preferences[stringPreferencesKey(DATA.DURATION)],
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

    fun setListItemThemeEnabled(enabled: Boolean) {
        _listItemThemeEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[listItemThemeKey] = enabled
            }
        }
    }

    fun setMarqueeEnabled(enabled: Boolean) {
        _marqueeEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[marqueeEnabledKey] = enabled
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
                preferences[albumKey] = song.album ?: DATA.UNKNOWN
                preferences[stringPreferencesKey(DATA.DURATION)] = song.duration ?: ""
                preferences[songIdKey] = song.id ?: ""
                preferences[albumIdKey] = song.albumId ?: ""
                preferences[cachedImagePathKey] = song.cachedImagePath ?: ""
            }
        }
    }
}