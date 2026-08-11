package com.flatcode.littleplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flatcode.littleplayer.data.entity.EqualizerEntity
import com.flatcode.littleplayer.repository.MusicRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val repository: MusicRoomRepository
) : ViewModel() {

    private val _equalizerSettings = MutableStateFlow<EqualizerEntity?>(null)
    val equalizerSettings: StateFlow<EqualizerEntity?> = _equalizerSettings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getEqualizerSettings().collect { settings ->
                _equalizerSettings.value = settings ?: EqualizerEntity()
            }
        }
    }
}