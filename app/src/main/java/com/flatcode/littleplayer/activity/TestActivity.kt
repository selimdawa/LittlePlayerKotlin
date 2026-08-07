package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.flatcode.littleplayer.databinding.ActivityTestBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar("Night Mode Test")
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.switchNightMode.setOnClickListener {
            val isChecked = binding.switchNightMode.isChecked
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            if (AppCompatDelegate.getDefaultNightMode() != mode) {
                viewModel.setDarkMode(mode)
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        binding.btnNightMode.setOnClickListener {
            binding.switchNightMode.performClick()
        }
    }

    private fun observeViewModel() {
        viewModel.darkModeFlow.collectWithLifecycle(this) { mode ->
            binding.switchNightMode.isChecked = mode == AppCompatDelegate.MODE_NIGHT_YES
        }
    }
}