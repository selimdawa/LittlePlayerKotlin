package com.flatcode.littleplayer.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

    val darkModeFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[darkModeKey] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    fun setDarkMode(mode: Int) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[darkModeKey] = mode
            }
        }
    }

    fun rescanMedia() {
        viewModelScope.launch {
            repository.getAllAudio(DATA.SORT_BY_NAME) // Trigger a full reload
        }
    }

    fun setSleepTimer(minutes: Int) {
        // This will be handled via MediaController command in the Activity
    }
}