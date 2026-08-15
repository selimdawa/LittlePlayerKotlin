package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import coil.load
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityDataStorageBinding
import com.flatcode.littleplayer.databinding.ItemMusicPreviewBinding
import com.flatcode.littleplayer.databinding.ItemNowPlayerPreviewBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.SimpleBlurTransformation
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.ensureBrightColor
import com.flatcode.littleplayer.utils.extractPalette
import com.flatcode.littleplayer.utils.extractPaletteColors
import com.flatcode.littleplayer.utils.formatAsSize
import com.flatcode.littleplayer.utils.getCurrentThemeColors
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.launchActivity
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
    private var currentDominantColor: Pair<Int, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.data_storage))
        val track = getLibraryColor("mc_track")
        val tick = getLibraryColor("mc_tick")
        currentDominantColor = Pair(track, tick)
        extractPalette(R.drawable.ic_image_preview) { palette ->
            currentDominantColor = palette.extractPaletteColors(track, tick)
            updatePreview()
        }
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.switchBottomPlayerTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBottomPlayerThemeEnabled(isChecked)
        }

        binding.switchListTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setListItemThemeEnabled(isChecked)
        }

        binding.switchMarquee.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setMarqueeEnabled(isChecked)
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

        binding.btnHiddenFolders.setOnClickListener {
            launchActivity<HiddenFoldersActivity>()
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

        viewModel.listItemThemeEnabled.collectWithLifecycle(this) { enabled ->
            binding.switchListTheme.isChecked = enabled
            updatePreview()
        }

        viewModel.marqueeEnabled.collectWithLifecycle(this) { enabled ->
            binding.switchMarquee.isChecked = enabled
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
        val imageRes = R.drawable.ic_image_preview

        // Bottom Player Previews
        setupThemeItem(
            binding.itemBasic,
            getString(R.string.basic),
            imageRes,
            40,
            DATA.MODE_BASIC,
            null,
        )
        setupThemeItem(
            binding.itemPalette,
            getString(R.string.palette),
            imageRes,
            60,
            DATA.MODE_PALETTE,
            currentDominantColor
        )
        setupThemeItem(
            binding.itemWhite, getString(R.string.white), imageRes, 20, DATA.MODE_WHITE, ContextCompat.getColor(this, R.color.white)
        )

        // List Item Previews
        setupListItemThemeItem(
            binding.itemListBasic, getString(R.string.basic), imageRes, DATA.MODE_BASIC, null
        )
        setupListItemThemeItem(
            binding.itemListPalette,
            getString(R.string.palette),
            imageRes,
            DATA.MODE_PALETTE,
            currentDominantColor
        )
        setupListItemThemeItem(
            binding.itemListWhite, getString(R.string.white), imageRes, DATA.MODE_WHITE, ContextCompat.getColor(this, R.color.white)
        )
    }

    private fun setupThemeItem(
        itemBinding: ItemNowPlayerPreviewBinding,
        label: String,
        imageSource: Any?,
        progress: Int,
        mode: Int,
        colorPair: Any?,
    ) {
        itemBinding.tvThemeLabel.text = label

        itemBinding.playerContent.name.text = getString(R.string.blinding_lights)
        itemBinding.playerContent.name.isSelected = viewModel.marqueeEnabled.value
        itemBinding.playerContent.artist.text = getString(R.string.the_weeknd)
        itemBinding.playerContent.miniProgressBar.progress = progress

        val colors = getCurrentThemeColors(mode, colorPair as? Pair<Int, Int>)

        itemBinding.playerContent.bottomPlayerContainer.setGradientBackground(colors.first, colors.second)
        itemBinding.playerContent.albumArtContainer.setGradientBackground(colors.first, colors.second)
        itemBinding.playerContent.playPauseBtn.setGradientBackground(colors.first, colors.second)

        itemBinding.playerContent.albumArt.load(imageSource ?: R.drawable.ic_music) {
            crossfade(enable = true)
            placeholder(R.drawable.ic_music)
            error(R.drawable.ic_music)
            size(200, 200)
        }
    }

    private fun setupListItemThemeItem(
        itemBinding: ItemMusicPreviewBinding,
        label: String,
        imageSource: Any?,
        mode: Int,
        colorPair: Any?
    ) {
        itemBinding.tvThemeLabel.text = label
        itemBinding.musicItem.songName.text = getString(R.string.blinding_lights)

        val songDetailsText = getString(
            R.string.song_details_format,
            getString(R.string.the_weeknd),
            "After Hours"
        )
        itemBinding.musicItem.songDetails.text = songDetailsText

        val colors = getCurrentThemeColors(mode, colorPair as? Pair<Int, Int>)

        itemBinding.musicItem.songName.setTextColor(colors.first)
        itemBinding.musicItem.wave.startColor = colors.first
        itemBinding.musicItem.wave.closeColor = colors.second
        itemBinding.musicItem.wave.visibility = android.view.View.VISIBLE

        itemBinding.musicItem.image.load(imageSource ?: R.drawable.ic_music) {
            crossfade(true)
            placeholder(R.drawable.ic_music)
            error(R.drawable.ic_music)
        }

        itemBinding.musicItem.imageBlur.load(imageSource ?: R.drawable.ic_music) {
            crossfade(enable = true)
            transformations(SimpleBlurTransformation(100f))
        }
    }
}