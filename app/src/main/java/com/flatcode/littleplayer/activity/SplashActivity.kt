package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.flatcode.littleplayer.unit.THEME
import com.flatcode.littleplayer.unit.CLASS
import com.flatcode.littleplayer.unit.VOID
import com.flatcode.littleplayer.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val context: Context = this@SplashActivity

    private val timePerSecond = 2
    private val timeFinal = TIME_PER_MILLIS * timePerSecond
    private val handler = Handler(Looper.getMainLooper())

    private val launchRunnable = Runnable { launch() }

    override fun onCreate(savedInstanceState: Bundle?) {
        THEME.setThemeOfApp(context)
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler.postDelayed(launchRunnable, timeFinal.toLong())
    }

    private fun launch() {
        VOID.intent1(context, CLASS.MAIN)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(launchRunnable)
    }

    companion object {
        const val TIME_PER_MILLIS = 1000
    }
}