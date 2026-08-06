package com.flatcode.littleplayer.activity

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.core.graphics.toColorInt
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityDataStorageBinding
import com.flatcode.littleplayer.databinding.ItemThemePreviewBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.snackbar
import com.flatcode.littleplayer.viewmodel.DataStorageViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@UnstableApi
@AndroidEntryPoint
class DataStorageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataStorageBinding
    private val viewModel: NowPlayerViewModel by viewModels()
    private val dataViewModel: DataStorageViewModel by viewModels()

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

        binding.itemBasic.cardPreview.setOnClickListener {
            viewModel.setThemeColorMode(DATA.MODE_BASIC)
        }
        binding.itemPalette.cardPreview.setOnClickListener {
            viewModel.setThemeColorMode(DATA.MODE_PALETTE)
        }
        binding.itemWhite.cardPreview.setOnClickListener {
            viewModel.setThemeColorMode(DATA.MODE_WHITE)
        }

        binding.btnClearCache.setOnClickListener {
            dataViewModel.clearCache()
            binding.root.snackbar(getString(R.string.art_cache_cleared))
        }

        binding.btnClearHistory.setOnClickListener {
            dataViewModel.clearHistory()
            binding.root.snackbar(getString(R.string.history_cleared))
        }
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

        dataViewModel.cacheSize.collectWithLifecycle(this) { size ->
            val sizeMb = size.toDouble() / (1024 * 1024)
            binding.tvCacheSize.text = String.format(Locale.getDefault(), "%.2f MB", sizeMb)
        }
    }

    private fun updatePreview(mode: Int) {
        // Basic
        setupThemeItem(
            binding.itemBasic,
            getString(R.string.basic),
            intArrayOf(getLibraryColor("mc_track"), getLibraryColor("mc_tick")),
            mode == DATA.MODE_BASIC,
        )

        // Palette
        val paletteColor = "#8A47EB".toColorInt()
        setupThemeItem(
            binding.itemPalette,
            getString(R.string.palette),
            intArrayOf(paletteColor, paletteColor),
            mode == DATA.MODE_PALETTE,
        )

        // White
        setupThemeItem(
            binding.itemWhite,
            getString(R.string.white),
            intArrayOf(Color.WHITE, Color.WHITE),
            mode == DATA.MODE_WHITE,
        )
    }

    private fun setupThemeItem(
        itemBinding: ItemThemePreviewBinding,
        label: String,
        colors: IntArray,
        isSelected: Boolean,
    ) {
        itemBinding.tvThemeLabel.text = label
        itemBinding.tvThemeLabel.setTextColor(if (isSelected) getLibraryColor("mc_track") else Color.GRAY)
        itemBinding.cardPreview.strokeWidth = if (isSelected) 4 else 0
        itemBinding.cardPreview.strokeColor = getLibraryColor("mc_track")

        val background = itemBinding.previewContainer.background.mutate()
        when (background) {
            is LayerDrawable -> {
                val gradientDrawable = background.getDrawable(0) as GradientDrawable
                gradientDrawable.colors = colors
            }

            is GradientDrawable -> {
                background.colors = colors
            }
        }

        val primaryColor = colors[0]
        itemBinding.previewProgressBar.progressTintList = ColorStateList.valueOf(primaryColor)
        itemBinding.previewPlayPauseView.foregroundTintList = ColorStateList.valueOf(primaryColor)
        itemBinding.previewNextBtn.imageTintList = ColorStateList.valueOf(primaryColor)
        itemBinding.tvSongName.setTextColor(primaryColor)

        val playPauseBg = itemBinding.previewPlayPauseBtn.background.mutate()
        if (playPauseBg is GradientDrawable) {
            playPauseBg.colors = colors
        }
    }
}