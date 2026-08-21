package com.flatcode.littleplayer.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object ThemeManager {
    private const val PREF_NAME = "theme_prefs"
    private val themeColorModeKey = intPreferencesKey(DATA.THEME_COLOR_MODE)
    private val themeExtractedColorKey = intPreferencesKey(DATA.THEME_EXTRACTED_COLOR)
    private val themeExtractedColorSecondKey = intPreferencesKey(DATA.THEME_EXTRACTED_COLOR_SECOND)
    private val darkModeKey = intPreferencesKey("dark_mode_preference")

    private const val KEY_MODE = DATA.THEME_COLOR_MODE
    private const val KEY_COLOR_START = DATA.THEME_EXTRACTED_COLOR
    private const val KEY_COLOR_END = DATA.THEME_EXTRACTED_COLOR_SECOND
    private const val KEY_DARK_MODE = "dark_mode_preference"

    var currentMode: Int = DATA.MODE_BASIC
        private set

    var currentColors: Pair<Int, Int>? = null
        private set

    var isReady = false
        private set

    private var prefs: SharedPreferences? = null

    /**
     * Synchronous initialization using SharedPreferences.
     * Call this as early as possible in Application.onCreate().
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs?.let {
            currentMode = it.getInt(KEY_MODE, DATA.MODE_BASIC)
            val start = it.getInt(KEY_COLOR_START, -1)
            val end = it.getInt(KEY_COLOR_END, -1)
            if ((start != -1) && (end != -1)) {
                currentColors = Pair(start, end)
            }

            val darkMode = it.getInt(KEY_DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            if (AppCompatDelegate.getDefaultNightMode() != darkMode) {
                AppCompatDelegate.setDefaultNightMode(darkMode)
            }
        }
        isReady = true
    }

    /**
     * Legacy async initialization for DataStore compatibility.
     * Can be used to migrate data if needed.
     */
    fun init(dataStore: DataStore<Preferences>) {
        runBlocking {
            val data = try {
                dataStore.data.first()
            } catch (_: Exception) {
                null
            }

            if (data != null) {
                val mode = data[themeColorModeKey] ?: DATA.MODE_BASIC
                val start = data[themeExtractedColorKey]
                val end = data[themeExtractedColorSecondKey]
                val darkMode = data[darkModeKey] ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

                // Update current state and sync to SharedPreferences if not already set
                if (prefs != null) {
                    saveMode(mode)
                    if (start != null && end != null) saveColors(start, end)
                    saveDarkMode(darkMode)
                }
            }
        }
    }

    fun updateMode(mode: Int) {
        currentMode = mode
        saveMode(mode)
    }

    fun updateColors(start: Int, end: Int) {
        currentColors = Pair(start, end)
        saveColors(start, end)
    }

    private fun saveMode(mode: Int) {
        prefs?.edit { putInt(KEY_MODE, mode) }
    }

    private fun saveColors(start: Int, end: Int) {
        prefs?.edit {
            putInt(KEY_COLOR_START, start)
            putInt(KEY_COLOR_END, end)
        }
    }

    fun saveDarkMode(mode: Int) {
        prefs?.edit { putInt(KEY_DARK_MODE, mode) }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}