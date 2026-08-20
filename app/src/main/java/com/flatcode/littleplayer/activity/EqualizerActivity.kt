package com.flatcode.littleplayer.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityEqualizerBinding
import com.flatcode.littleplayer.databinding.ItemEqBandVerticalBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.viewmodel.EqualizerViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import io.selimdawa.multicolors.MultiColorManager
import kotlinx.coroutines.launch
import kotlin.math.abs

@UnstableApi
@AndroidEntryPoint
class EqualizerActivity :
    BaseActivity<ActivityEqualizerBinding>(ActivityEqualizerBinding::inflate) {

    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private val equalizerViewModel: EqualizerViewModel by viewModels()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var selectedPresetName: String = DATA.PRESET_CUSTOM

    private val bands = arrayOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    private val bandSeekBars = mutableListOf<SeekBar>()

    private val presets = mapOf(
        DATA.PRESET_FLAT to shortArrayOf(0, 0, 0, 0, 0),
        DATA.PRESET_POP to shortArrayOf(-100, 200, 500, 100, -200),
        DATA.PRESET_ROCK to shortArrayOf(400, 300, -100, 300, 500),
        DATA.PRESET_JAZZ to shortArrayOf(400, 200, -200, 200, 500),
        DATA.PRESET_CLASSICAL to shortArrayOf(500, 300, -200, 400, 400),
        DATA.PRESET_DANCE to shortArrayOf(600, 0, 200, 400, 100),
    )

    private data class PresetUI(
        val name: String,
        val card: androidx.cardview.widget.CardView,
        val text: android.widget.TextView,
        val icon: android.widget.ImageView
    )

    private val presetUIs by lazy {
        listOf(
            PresetUI(DATA.PRESET_CUSTOM, binding.cardCustom, binding.textCustom, binding.icCustom),
            PresetUI(DATA.PRESET_POP, binding.cardPop, binding.textPop, binding.icPop),
            PresetUI(DATA.PRESET_ROCK, binding.cardRock, binding.textRock, binding.icRock),
            PresetUI(DATA.PRESET_JAZZ, binding.cardJazz, binding.textJazz, binding.icJazz),
            PresetUI(
                DATA.PRESET_CLASSICAL,
                binding.cardClassical,
                binding.textClassical,
                binding.icClassical
            ),
            PresetUI(DATA.PRESET_DANCE, binding.cardDance, binding.textDance, binding.icDance),
            PresetUI(DATA.PRESET_FLAT, binding.cardFlat, binding.textFlat, binding.icFlat)
        )
    }

    override fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            // Push the root content above the navigation bar
            v.updatePadding(bottom = systemBars.bottom)

            // Push the topBar below the status bar and camera cutouts using Margin
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top + resources.getDimensionPixelSize(R.dimen.spacing_8)
            }
            insets
        }

        binding.back.setOnClickListener { finish() }
        setupListeners()
    }

    override fun observeViewModel() {
        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }

        equalizerViewModel.equalizerSettings.collectWithLifecycle(this) { settings ->
            settings?.let {
                binding.switchEq.isChecked = it.enabled
                binding.bassKnob.progress = it.bassStrength.toInt() / 10
                binding.virtualizerKnob.progress = it.virtualizerStrength.toInt() / 10

                selectedPresetName = it.presetName
                val levels =
                    it.bandLevels.split(",").map { level -> level.toShort() }.toShortArray()
                setupBands(levels)
                updatePresetSelectionUI(selectedPresetName, getLibraryColor("mc_track"))
            }
        }

        lifecycleScope.launch {
            MultiColorManager.currentThemeId.collect { _ ->
                MultiColorManager.applyTheme(this@EqualizerActivity)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.switchEq.setOnCheckedChangeListener { _, isChecked ->
            val bundle = Bundle().apply { putBoolean("ENABLED", isChecked) }
            mediaController?.sendCustomCommand(
                SessionCommand(MusicService.COMMAND_TOGGLE_EQ, Bundle.EMPTY), bundle
            )
        }

        val trackColor = getLibraryColor("mc_track")
        val containerBg = getLibraryColor("colorSurfaceContainerHigh")

        binding.bassKnob.apply {
            this.trackColor = containerBg
            this.progressColor = trackColor
            this.knobColor = ContextCompat.getColor(this@EqualizerActivity, R.color.white)
            this.indicatorColor = trackColor
            onProgressChanged = { progress, fromUser ->
                if (fromUser) {
                    sendBassStrength(progress * 10)
                    mediaController?.sendCustomCommand(
                        SessionCommand(MusicService.COMMAND_SAVE_EQ_SETTINGS, Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                }
            }
        }

        binding.virtualizerKnob.apply {
            this.trackColor = containerBg
            this.progressColor = trackColor
            this.knobColor = ContextCompat.getColor(this@EqualizerActivity, R.color.white)
            this.indicatorColor = trackColor
            onProgressChanged = { progress, fromUser ->
                if (fromUser) {
                    sendVirtualizerStrength(progress * 10)
                    mediaController?.sendCustomCommand(
                        SessionCommand(MusicService.COMMAND_SAVE_EQ_SETTINGS, Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                }
            }
        }

        presetUIs.forEach { ui ->
            ui.card.setOnClickListener { selectPreset(ui.name) }
        }
    }

    private fun sendBassStrength(strength: Int) {
        val bundle = Bundle().apply { putShort("STRENGTH", strength.toShort()) }
        mediaController?.sendCustomCommand(
            SessionCommand(MusicService.COMMAND_SET_BASS, Bundle.EMPTY), bundle
        )
    }

    private fun sendVirtualizerStrength(strength: Int) {
        val bundle = Bundle().apply { putShort("STRENGTH", strength.toShort()) }
        mediaController?.sendCustomCommand(
            SessionCommand(MusicService.COMMAND_SET_VIRTUALIZER, Bundle.EMPTY), bundle
        )
    }

    private fun selectPreset(name: String) {
        val trackColor = getLibraryColor("mc_track")
        selectedPresetName = name
        updatePresetSelectionUI(name, trackColor)

        val bundlePreset = Bundle().apply { putString("PRESET", name) }
        mediaController?.sendCustomCommand(
            SessionCommand(MusicService.COMMAND_SET_PRESET, Bundle.EMPTY), bundlePreset
        )

        presets[name]?.let { levels ->
            for (i in levels.indices) {
                val progress = levels[i] + 1500
                bandSeekBars[i].progress = progress

                val bundle = Bundle().apply {
                    putShort("BAND", i.toShort())
                    putShort("LEVEL", levels[i])
                }
                mediaController?.sendCustomCommand(
                    SessionCommand(MusicService.COMMAND_SET_EQ_BAND, Bundle.EMPTY), bundle
                )
            }
        }
        mediaController?.sendCustomCommand(
            SessionCommand(MusicService.COMMAND_SAVE_EQ_SETTINGS, Bundle.EMPTY), Bundle.EMPTY
        )
    }

    private fun updatePresetSelectionUI(selected: String?, themeColor: Int) {
        val containerBg = getLibraryColor("colorSurfaceContainerHigh")
        val trackColor = getLibraryColor("mc_track")
        val errorColor = getLibraryColor("colorError")
        val trackCsl = ColorStateList.valueOf(trackColor)
        val whiteCsl = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        val grayColor = ContextCompat.getColor(this, R.color.gray)

        presetUIs.forEach { ui ->
            val isSelected = ui.name == selected
            ui.card.setCardBackgroundColor(if (isSelected) themeColor else containerBg)
            ui.text.setTextColor(if (isSelected) errorColor else grayColor)
            ui.icon.imageTintList = if (isSelected) whiteCsl else trackCsl
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBands(initialLevels: ShortArray? = null) {
        binding.eqContainer.removeAllViews()
        bandSeekBars.clear()

        for (i in bands.indices) {
            val bandBinding =
                ItemEqBandVerticalBinding.inflate(layoutInflater, binding.eqContainer, false)
            val bandView = bandBinding.root
            val tvLabel = bandBinding.tvBandLabel
            val tvLevel = bandBinding.tvBandLevel
            val seekBar = bandBinding.sbBandLevel

            tvLabel.text = bands[i]
            seekBar.max = 3000

            val initialLevel = initialLevels?.getOrNull(i)?.toInt() ?: 0
            seekBar.progress = initialLevel + 1500

            val dbLevel = initialLevel / 100
            tvLevel.text = getString(R.string.db_format, if (dbLevel > 0) "+" else "", dbLevel)

            bandSeekBars.add(seekBar)

            seekBar.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    v.performClick()
                }
                false
            }

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                var lastSentLevel: Int = -1

                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val dbLevel = (progress - 1500) / 100
                    tvLevel.text =
                        getString(R.string.db_format, if (dbLevel > 0) "+" else "", dbLevel)

                    if (fromUser) {
                        val currentLevel = progress - 1500
                        if (abs(currentLevel - lastSentLevel) > 10) {
                            lastSentLevel = currentLevel
                            val bundle = Bundle().apply {
                                putShort("BAND", i.toShort())
                                putShort("LEVEL", currentLevel.toShort())
                            }
                            mediaController?.sendCustomCommand(
                                SessionCommand(MusicService.COMMAND_SET_EQ_BAND, Bundle.EMPTY),
                                bundle
                            )
                        }
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {
                    if (selectedPresetName != DATA.PRESET_CUSTOM) {
                        selectedPresetName = DATA.PRESET_CUSTOM
                        updatePresetSelectionUI(DATA.PRESET_CUSTOM, getLibraryColor("mc_track"))
                        val bundlePreset =
                            Bundle().apply { putString("PRESET", DATA.PRESET_CUSTOM) }
                        mediaController?.sendCustomCommand(
                            SessionCommand(MusicService.COMMAND_SET_PRESET, Bundle.EMPTY),
                            bundlePreset
                        )
                    }
                }

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    sb?.let {
                        val finalLevel = it.progress - 1500
                        val bundle = Bundle().apply {
                            putShort("BAND", i.toShort())
                            putShort("LEVEL", finalLevel.toShort())
                        }
                        mediaController?.sendCustomCommand(
                            SessionCommand(MusicService.COMMAND_SET_EQ_BAND, Bundle.EMPTY), bundle
                        )
                        mediaController?.sendCustomCommand(
                            SessionCommand(MusicService.COMMAND_SAVE_EQ_SETTINGS, Bundle.EMPTY),
                            Bundle.EMPTY
                        )
                    }
                }
            })
            binding.eqContainer.addView(bandView)
        }
    }
}