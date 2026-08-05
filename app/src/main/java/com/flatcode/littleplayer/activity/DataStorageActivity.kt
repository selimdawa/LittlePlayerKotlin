package com.flatcode.littleplayer.activity

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityDataStorageBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class DataStorageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataStorageBinding
    private val viewModel: NowPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.data_storage))
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.switchBottomPlayerTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBottomPlayerThemeEnabled(isChecked)
        }

        binding.btnBasic.setOnClickListener { updatePreview(DATA.MODE_BASIC) }
        binding.btnPalette.setOnClickListener { updatePreview(DATA.MODE_PALETTE) }
        binding.btnWhite.setOnClickListener { updatePreview(DATA.MODE_WHITE) }
    }

    private fun observeViewModel() {
        viewModel.bottomPlayerThemeEnabled.collectWithLifecycle(this) { enabled ->
            binding.switchBottomPlayerTheme.isChecked = enabled
        }

        viewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }

        viewModel.themeColorMode.collectWithLifecycle(this) { mode ->
            updatePreview(mode)
        }
    }

    private fun updatePreview(mode: Int) {
        val background = binding.previewPlayPause.background.mutate()
        if (background is GradientDrawable) {
            when (mode) {
                DATA.MODE_BASIC -> {
                    background.colors = intArrayOf(getLibraryColor("mc_track"), getLibraryColor("mc_tick"))
                }
                DATA.MODE_PALETTE -> {
                    background.colors = intArrayOf(Color.parseColor("#8A47EB"), Color.parseColor("#8A47EB"))
                }
                DATA.MODE_WHITE -> {
                    background.colors = intArrayOf(Color.WHITE, Color.WHITE)
                }
            }
            binding.previewPlayPause.background = background
        }

        // Highlight selected dot
        binding.dotBasic.strokeWidth = if (mode == DATA.MODE_BASIC) 4 else 1
        binding.dotPalette.strokeWidth = if (mode == DATA.MODE_PALETTE) 4 else 1
        binding.dotWhite.strokeWidth = if (mode == DATA.MODE_WHITE) 4 else 1
    }
}