package com.flatcode.littleplayer.activity

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
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
    private var currentDominantColor: Int = Color.GRAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.data_storage))
        extractPaletteColor()
        setupListeners()
        observeViewModel()
    }

    private fun extractPaletteColor() {
        val request = ImageRequest.Builder(this).data(R.drawable.image_1).allowHardware(false)
            .target { result ->
                val bitmap = (result as? BitmapDrawable)?.bitmap
                bitmap?.let {
                    Palette.from(it).generate { palette ->
                        currentDominantColor =
                            palette?.getVibrantColor(Color.GRAY) ?: palette?.getLightVibrantColor(
                                Color.GRAY
                            ) ?: palette?.getDominantColor(Color.GRAY) ?: Color.GRAY
                        updatePreview(viewModel.themeColorMode.value)
                    }
                }
            }.build()
        imageLoader.enqueue(request)
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
            updatePreview(viewModel.themeColorMode.value)
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
        val imageRes = R.drawable.image_1

        // Song 1: Basic (Default Gradient)
        setupThemeItem(
            binding.itemBasic,
            getString(R.string.basic),
            imageRes,
            40,
            DATA.MODE_BASIC,
            null,
            mode == DATA.MODE_BASIC
        )

        // Song 2: Palette (Solid Color from Image)
        setupThemeItem(
            binding.itemPalette,
            getString(R.string.palette),
            imageRes,
            60,
            DATA.MODE_PALETTE,
            currentDominantColor,
            mode == DATA.MODE_PALETTE
        )

        // Song 3: White (Solid White)
        setupThemeItem(
            binding.itemWhite,
            getString(R.string.white),
            imageRes,
            20,
            DATA.MODE_WHITE,
            Color.WHITE,
            mode == DATA.MODE_WHITE
        )
    }

    private fun setupThemeItem(
        itemBinding: ItemThemePreviewBinding,
        label: String,
        imageSource: Any?,
        progress: Int,
        mode: Int,
        targetColor: Int?,
        isSelected: Boolean
    ) {
        itemBinding.tvThemeLabel.text = label

        itemBinding.name.text = "Blinding Lights"
        @Suppress("SpellCheckingInspection") itemBinding.artist.text = "The Weeknd"
        itemBinding.miniProgressBar.progress = progress

        val track = getLibraryColor("mc_track")
        val tick = getLibraryColor("mc_tick")

        // 1. تلوين الحاوية الأساسية (LayerDrawable)
        val background = itemBinding.bottomPlayerContainer.background.mutate() as? LayerDrawable
        val bgShape = background?.getDrawable(0) as? GradientDrawable

        // 2. تلوين إطار الصورة (LayerDrawable)
        val albumArtBG =
            (itemBinding.albumArtContainer.background.mutate() as? LayerDrawable)?.getDrawable(0) as? GradientDrawable

        // 3. تلوين زر التشغيل (GradientDrawable)
        val playBtnBG = itemBinding.playPauseBtn.background.mutate() as? GradientDrawable

        val colorToApply = when (mode) {
            DATA.MODE_PALETTE -> targetColor
            DATA.MODE_WHITE -> Color.WHITE
            else -> null
        }

        if (colorToApply != null) {
            val colors = intArrayOf(colorToApply, colorToApply)
            bgShape?.colors = colors
            albumArtBG?.colors = colors
            playBtnBG?.colors = colors
        } else {
            val colors = intArrayOf(track, tick)
            bgShape?.colors = colors
            albumArtBG?.colors = colors
            playBtnBG?.colors = colors
        }

        itemBinding.albumArt.load(imageSource ?: R.drawable.ic_music) {
            crossfade(true)
            placeholder(R.drawable.ic_music)
            error(R.drawable.ic_music)
            size(200, 200)
        }
    }
}