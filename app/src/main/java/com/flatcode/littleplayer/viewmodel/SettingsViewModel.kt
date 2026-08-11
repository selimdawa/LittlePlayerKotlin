package com.flatcode.littleplayer.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.repository.MusicRepository
import com.flatcode.littleplayer.utils.DATA
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>, private val repository: MusicRepository
) : ViewModel() {

    private val darkModeKey = intPreferencesKey("dark_mode_preference")
    private val showSongToastKey = booleanPreferencesKey(DATA.SHOW_SONG_TOAST)
    private val doubleClickActionKey =
        androidx.datastore.preferences.core.stringPreferencesKey(DATA.HEADSET_DOUBLE_CLICK_ACTION)
    private val tripleClickActionKey =
        androidx.datastore.preferences.core.stringPreferencesKey(DATA.HEADSET_TRIPLE_CLICK_ACTION)

    val darkModeFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[darkModeKey] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    val showSongToastFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[showSongToastKey] ?: false
    }

    val doubleClickActionFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[doubleClickActionKey] ?: DATA.ACTION_NEXT_TRACK
    }

    val tripleClickActionFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[tripleClickActionKey] ?: DATA.ACTION_PREV_TRACK
    }

    fun setDarkMode(mode: Int) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[darkModeKey] = mode
            }
        }
    }

    fun setShowSongToast(show: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[showSongToastKey] = show
            }
        }
    }

    fun setHeadsetDoubleClickAction(action: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[doubleClickActionKey] = action
            }
        }
    }

    fun setHeadsetTripleClickAction(action: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[tripleClickActionKey] = action
            }
        }
    }

    fun rescanMedia() {
        viewModelScope.launch {
            repository.getAllAudio(DATA.SORT_BY_NAME) // Trigger a full reload
        }
    }
}
