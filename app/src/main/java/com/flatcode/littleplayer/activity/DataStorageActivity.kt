package com.flatcode.littleplayer.activity

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import coil.load
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityDataStorageBinding
import com.flatcode.littleplayer.databinding.ItemThemePreviewBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.ensureBrightColor
import com.flatcode.littleplayer.utils.extractPalette
import com.flatcode.littleplayer.utils.extractVibrantColor
import com.flatcode.littleplayer.utils.formatAsSize
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.setGradientBackground
import com.flatcode.littleplayer.utils.setSolidBackground
import com.flatcode.littleplayer.utils.snackbar
import com.flatcode.littleplayer.viewmodel.DataStorageViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class DataStorageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataStorageBinding
    private val viewModel: NowPlayerViewModel by viewModels()
    private val dataViewModel: DataStorageViewModel by viewModels()
    private var currentDominantColor: Int = Color.GRAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.data_storage))
        extractPalette(R.drawable.image_1) { palette ->
            currentDominantColor = palette.extractVibrantColor()
            updatePreview()
        }
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.switchBottomPlayerTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBottomPlayerThemeEnabled(isChecked)
        }

        binding.itemBasic.root.setOnClickListener {
            viewModel.setThemeColorMode(DATA.MODE_BASIC)
        }
        binding.itemPalette.root.setOnClickListener {
            viewModel.setThemeColorMode(DATA.MODE_PALETTE)
        }
        binding.itemWhite.root.setOnClickListener {
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
            updatePreview()
        }

        viewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }

        viewModel.themeColorMode.collectWithLifecycle(this) {
            updatePreview()
        }

        dataViewModel.cacheSize.collectWithLifecycle(this) { size ->
            binding.tvCacheSize.text = size.formatAsSize()
        }
    }

    private fun updatePreview() {
        val imageRes = R.drawable.image_1

        // Song 1: Basic (Default Gradient)
        setupThemeItem(
            binding.itemBasic, getString(R.string.basic), imageRes, 40, DATA.MODE_BASIC, null
        )

        // Song 2: Palette (Solid Color from Image)
        setupThemeItem(
            binding.itemPalette,
            getString(R.string.palette),
            imageRes,
            60,
            DATA.MODE_PALETTE,
            currentDominantColor
        )

        // Song 3: White (Solid White)
        setupThemeItem(
            binding.itemWhite, getString(R.string.white), imageRes, 20, DATA.MODE_WHITE, Color.WHITE
        )
    }

    private fun setupThemeItem(
        itemBinding: ItemThemePreviewBinding,
        label: String,
        imageSource: Any?,
        progress: Int,
        mode: Int,
        targetColor: Int?
    ) {
        itemBinding.tvThemeLabel.text = label

        itemBinding.name.text = getString(R.string.blinding_lights)
        itemBinding.artist.text = getString(R.string.the_weeknd)
        itemBinding.miniProgressBar.progress = progress

        val track = getLibraryColor("mc_track")
        val tick = getLibraryColor("mc_tick")

        val colorToApply = when (mode) {
            DATA.MODE_PALETTE -> targetColor?.ensureBrightColor()
            DATA.MODE_WHITE -> Color.WHITE
            else -> null
        }

        if (colorToApply != null) {
            itemBinding.bottomPlayerContainer.setSolidBackground(colorToApply)
            itemBinding.albumArtContainer.setSolidBackground(colorToApply)
            itemBinding.playPauseBtn.setSolidBackground(colorToApply)
        } else {
            itemBinding.bottomPlayerContainer.setGradientBackground(track, tick)
            itemBinding.albumArtContainer.setGradientBackground(track, tick)
            itemBinding.playPauseBtn.setGradientBackground(track, tick)
        }

        itemBinding.albumArt.load(imageSource ?: R.drawable.ic_music) {
            crossfade(true)
            placeholder(R.drawable.ic_music)
            error(R.drawable.ic_music)
            size(200, 200)
        }
    }
}