package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataStorageViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    init {
        updateCacheSize()
    }

    fun updateCacheSize() {
        _cacheSize.value = repository.getCacheSize()
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearArtCache()
            updateCacheSize()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun resetPaletteColors() {
        viewModelScope.launch {
            repository.resetPaletteColors()
        }
    }
}