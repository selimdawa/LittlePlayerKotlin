package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flatcode.littleplayer.databinding.ActivitySplashBinding
import com.flatcode.littleplayer.utils.CLASS
import com.flatcode.littleplayer.utils.THEME
import com.flatcode.littleplayer.utils.VOID
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val context: Context = this@SplashActivity
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        THEME.setThemeOfApp(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.loadAudioData()

        lifecycleScope.launch {
            delay(500.milliseconds)
            VOID.intent1(context, CLASS.MAIN)
            finish()
        }
    }
}