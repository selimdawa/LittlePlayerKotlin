package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.data.entity.EqualizerEntity
import com.flatcode.littleplayer.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _equalizerSettings = MutableStateFlow<EqualizerEntity?>(null)
    val equalizerSettings: StateFlow<EqualizerEntity?> = _equalizerSettings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _equalizerSettings.value = repository.getEqualizerSettings() ?: EqualizerEntity()
        }
    }
}