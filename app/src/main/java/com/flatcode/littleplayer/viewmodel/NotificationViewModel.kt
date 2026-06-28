package com.flatcode.littleplayer.viewmodel

import android.app.NotificationManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationManager: NotificationManager
) : ViewModel() {

    fun playMusic() {
    }

    fun pauseMusic() {
    }
}