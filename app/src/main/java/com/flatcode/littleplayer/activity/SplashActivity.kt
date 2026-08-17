package com.flatcode.littleplayer.activity

import android.annotation.SuppressLint
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.flatcode.littleplayer.databinding.ActivitySplashBinding
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {

    private val viewModel: MusicViewModel by viewModels()

    override fun setupViews() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        viewModel.loadAudioData()

        lifecycleScope.launch {
            delay(500.milliseconds)
            launchActivity<MainActivity>()
            finish()
        }
    }
}