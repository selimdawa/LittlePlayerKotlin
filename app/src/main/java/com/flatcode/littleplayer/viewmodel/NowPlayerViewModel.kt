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
import com.flatcode.littleplayer.utils.ThemeManager
import com.flatcode.littleplayer.utils.getLibraryColor
import io.selimdawa.multicolors.R as MultiColorR
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
    private val roomRepository: MusicRoomRepository,
) : ViewModel() {

    private val _currentPlayingSong = MutableStateFlow<MusicFiles?>(null)
    val currentPlayingSong: StateFlow<MusicFiles?> = _currentPlayingSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(value = false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentThemeColor = MutableStateFlow<Pair<Int, Int>?>(null)
    val currentThemeColor: StateFlow<Pair<Int, Int>?> = _currentThemeColor.asStateFlow()

    private val _themeColorMode = MutableStateFlow(DATA.MODE_BASIC)
    val themeColorMode: StateFlow<Int> = _themeColorMode.asStateFlow()

    private val _bottomPlayerThemeEnabled = MutableStateFlow(value = true)
    val bottomPlayerThemeEnabled: StateFlow<Boolean> = _bottomPlayerThemeEnabled.asStateFlow()

    private val _listItemThemeEnabled = MutableStateFlow(true)
    val listItemThemeEnabled: StateFlow<Boolean> = _listItemThemeEnabled.asStateFlow()

    private val _marqueeEnabled = MutableStateFlow(true)
    val marqueeEnabled: StateFlow<Boolean> = _marqueeEnabled.asStateFlow()

    private val _initialProgress = MutableStateFlow(0)
    val initialProgress: StateFlow<Int> = _initialProgress.asStateFlow()

    private val musicFileKey = stringPreferencesKey(DATA.MUSIC_FILE)
    private val artistNameKey = stringPreferencesKey(DATA.ARTIST_NAME)
    private val songNameKey = stringPreferencesKey(DATA.SONG_NAME)
    private val albumKey = stringPreferencesKey(DATA.ALBUM)
    private val songIdKey = stringPreferencesKey(DATA.SONG_ID)
    private val albumIdKey = stringPreferencesKey(DATA.ALBUM_ID)
    private val cachedImagePathKey = stringPreferencesKey(DATA.CACHED_IMAGE_PATH)
    private val themeExtractedColorKey = intPreferencesKey(DATA.THEME_EXTRACTED_COLOR)
    private val themeExtractedColorSecondKey = intPreferencesKey(DATA.THEME_EXTRACTED_COLOR_SECOND)
    private val bottomPlayerThemeKey = booleanPreferencesKey(DATA.BOTTOM_PLAYER_THEME)
    private val listItemThemeKey = booleanPreferencesKey(DATA.LIST_ITEM_THEME)
    private val themeColorModeKey = intPreferencesKey(DATA.THEME_COLOR_MODE)
    private val marqueeEnabledKey = booleanPreferencesKey(DATA.MARQUEE_ENABLED)
    private val colorSongIdKey = stringPreferencesKey(DATA.COLOR_SONG_ID)

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
                val song = queue.find { it.id == playbackState.currentSongId }
                _currentPlayingSong.value = song
                
                // Calculate initial progress
                if (song != null) {
                    val durationMs = song.duration?.toLongOrNull() ?: 0L
                    if (durationMs > 0) {
                        val progress = ((playbackState.lastProgress * 100) / durationMs).toInt()
                        _initialProgress.value = progress.coerceIn(0, 100)
                    }
                }
            }
            
            dataStore.data.collect { preferences ->
                _themeColorMode.value = preferences[themeColorModeKey] ?: DATA.MODE_BASIC
                _bottomPlayerThemeEnabled.value = preferences[bottomPlayerThemeKey] ?: true
                _listItemThemeEnabled.value = preferences[listItemThemeKey] ?: true
                _marqueeEnabled.value = preferences[marqueeEnabledKey] ?: true
                _colorSongId.value = preferences[colorSongIdKey]

                val colorStart = preferences[themeExtractedColorKey]
                val colorEnd = preferences[themeExtractedColorSecondKey]
                if (colorStart != null && colorEnd != null) {
                    _currentThemeColor.value = Pair(colorStart, colorEnd)
                } else if (colorStart != null) {
                    _currentThemeColor.value = Pair(colorStart, colorStart)
                }

                if (_currentPlayingSong.value == null) {
                    val path = preferences[musicFileKey]
                    if (!path.isNullOrEmpty()) {
                        val rawAlbum = preferences[albumKey] ?: DATA.UNKNOWN
                        _currentPlayingSong.value = MusicFiles(
                            path = path,
                            artist = preferences[artistNameKey] ?: DATA.UNKNOWN,
                            title = preferences[songNameKey] ?: DATA.UNKNOWN,
                            album = MusicFiles.getCleanedAlbum(rawAlbum, path),
                            duration = preferences[stringPreferencesKey(DATA.DURATION)],
                            id = preferences[songIdKey],
                            albumId = preferences[albumIdKey],
                            cachedImagePath = preferences[cachedImagePathKey],
                            cachedBlurPath = preferences[stringPreferencesKey("cached_blur_path")],
                            dominantColor = preferences[themeExtractedColorKey],
                            vibrantColor = preferences[themeExtractedColorSecondKey]
                        )
                    }
                }
            }
        }
    }

    fun updatePlaybackState(playing: Boolean) {
        _isPlaying.value = playing
    }

    private val _colorSongId = MutableStateFlow<String?>(null)

    fun updateThemeColor(songId: String?, start: Int, end: Int) {
        if (songId != null && _colorSongId.value == songId && _currentThemeColor.value != null) {
            return // Already have colors for this song
        }
        _colorSongId.value = songId
        _currentThemeColor.value = Pair(start, end)
        ThemeManager.updateColors(start, end)
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[themeExtractedColorKey] = start
                preferences[themeExtractedColorSecondKey] = end
                songId?.let { preferences[colorSongIdKey] = it }
            }
        }
    }

    fun setThemeColorMode(mode: Int) {
        _themeColorMode.value = mode
        ThemeManager.updateMode(mode)
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

    fun getCurrentSongFromRepository(id: String): MusicFiles? {
        return repository.currentPlaylist.value.find { it.id == id }
    }

    fun saveAndBroadcastNextSong(song: MusicFiles) {
        _currentPlayingSong.value = song

        // If song has colors, update theme immediately
        if (song.vibrantColor != null && song.dominantColor != null) {
            // Even if we have colors, if they are (0,0), it's a dynamic song
            if (song.vibrantColor == 0 && song.dominantColor == 0) {
                updateThemeColor(song.id, 0, 0)
            } else {
                updateThemeColor(song.id, song.vibrantColor, song.dominantColor)
            }
        } else {
            // Trigger background extraction if missing
            viewModelScope.launch(Dispatchers.IO) {
                val track = repository.context.getLibraryColor(MultiColorR.attr.mc_track)
                val tick = repository.context.getLibraryColor(MultiColorR.attr.mc_tick)
                song.id?.let { id ->
                    song.path?.let { path ->
                        repository.extractColorsForSong(id, path, song.albumId, track, tick)
                        // After extraction, the repository updates Room. 
                        // We should probably fetch the updated song or just rely on the next update.
                        // For immediate feedback:
                        val updatedSong = roomRepository.getSongById(id)
                        if (updatedSong?.vibrantColor != null && updatedSong.dominantColor != null) {
                            launch(Dispatchers.Main) {
                                updateThemeColor(id, updatedSong.vibrantColor, updatedSong.dominantColor)
                            }
                        }
                    }
                }
            }
        }

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
                
                song.vibrantColor?.let { preferences[themeExtractedColorKey] = it }
                song.dominantColor?.let { preferences[themeExtractedColorSecondKey] = it }
                song.id?.let { preferences[colorSongIdKey] = it }
            }
        }
    }
}