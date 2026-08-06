package com.flatcode.littleplayer.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityEqualizerBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.viewmodel.EqualizerViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class EqualizerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEqualizerBinding
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private val equalizerViewModel: EqualizerViewModel by viewModels()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var selectedPresetName: String = "Custom"

    private val bands = arrayOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    private val bandSeekBars = mutableListOf<SeekBar>()

    private val presets = mapOf(
        "Flat" to shortArrayOf(0, 0, 0, 0, 0),
        "Pop" to shortArrayOf(-100, 200, 500, 100, -200),
        "Rock" to shortArrayOf(400, 300, -100, 300, 500),
        "Jazz" to shortArrayOf(400, 200, -200, 200, 500),
        "Classical" to shortArrayOf(500, 300, -200, 400, 400),
        "Dance" to shortArrayOf(600, 0, 200, 400, 100),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener { finish() }
        setupListeners()
        observeViewModel()
    }

    private fun observeViewModel() {
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
            this.knobColor = Color.WHITE
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
            this.knobColor = Color.WHITE
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

        binding.btnPresetCustom.setOnClickListener { selectPreset("Custom") }
        binding.btnPresetPop.setOnClickListener { selectPreset("Pop") }
        binding.btnPresetRock.setOnClickListener { selectPreset("Rock") }
        binding.btnPresetJazz.setOnClickListener { selectPreset("Jazz") }
        binding.btnPresetClassical.setOnClickListener { selectPreset("Classical") }
        binding.btnPresetDance.setOnClickListener { selectPreset("Dance") }
        binding.btnPresetFlat.setOnClickListener { selectPreset("Flat") }
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
        val trackCsl = ColorStateList.valueOf(trackColor)
        val whiteCsl = ColorStateList.valueOf(Color.WHITE)

        binding.cardCustom.setCardBackgroundColor(containerBg)
        binding.cardPop.setCardBackgroundColor(containerBg)
        binding.cardRock.setCardBackgroundColor(containerBg)
        binding.cardJazz.setCardBackgroundColor(containerBg)
        binding.cardClassical.setCardBackgroundColor(containerBg)
        binding.cardDance.setCardBackgroundColor(containerBg)
        binding.cardFlat.setCardBackgroundColor(containerBg)

        binding.textCustom.setTextColor(Color.GRAY)
        binding.textPop.setTextColor(Color.GRAY)
        binding.textRock.setTextColor(Color.GRAY)
        binding.textJazz.setTextColor(Color.GRAY)
        binding.textClassical.setTextColor(Color.GRAY)
        binding.textDance.setTextColor(Color.GRAY)
        binding.textFlat.setTextColor(Color.GRAY)

        binding.icCustom.imageTintList = trackCsl
        binding.icPop.imageTintList = trackCsl
        binding.icRock.imageTintList = trackCsl
        binding.icJazz.imageTintList = trackCsl
        binding.icClassical.imageTintList = trackCsl
        binding.icDance.imageTintList = trackCsl
        binding.icFlat.imageTintList = trackCsl

        when (selected) {
            "Custom" -> {
                binding.cardCustom.setCardBackgroundColor(themeColor)
                binding.textCustom.setTextColor(Color.WHITE)
                binding.icCustom.imageTintList = whiteCsl
            }

            "Pop" -> {
                binding.cardPop.setCardBackgroundColor(themeColor)
                binding.textPop.setTextColor(Color.WHITE)
                binding.icPop.imageTintList = whiteCsl
            }

            "Rock" -> {
                binding.cardRock.setCardBackgroundColor(themeColor)
                binding.textRock.setTextColor(Color.WHITE)
                binding.icRock.imageTintList = whiteCsl
            }

            "Jazz" -> {
                binding.cardJazz.setCardBackgroundColor(themeColor)
                binding.textJazz.setTextColor(Color.WHITE)
                binding.icJazz.imageTintList = whiteCsl
            }

            "Classical" -> {
                binding.cardClassical.setCardBackgroundColor(themeColor)
                binding.textClassical.setTextColor(Color.WHITE)
                binding.icClassical.imageTintList = whiteCsl
            }

            "Dance" -> {
                binding.cardDance.setCardBackgroundColor(themeColor)
                binding.textDance.setTextColor(Color.WHITE)
                binding.icDance.imageTintList = whiteCsl
            }

            "Flat" -> {
                binding.cardFlat.setCardBackgroundColor(themeColor)
                binding.textFlat.setTextColor(Color.WHITE)
                binding.icFlat.imageTintList = whiteCsl
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBands(initialLevels: ShortArray? = null) {
        binding.eqContainer.removeAllViews()
        bandSeekBars.clear()

        for (i in bands.indices) {
            val bandView =
                layoutInflater.inflate(R.layout.item_eq_band_vertical, binding.eqContainer, false)
            val tvLabel = bandView.findViewById<TextView>(R.id.tvBandLabel)
            val tvLevel = bandView.findViewById<TextView>(R.id.tvBandLevel)
            val seekBar = bandView.findViewById<SeekBar>(R.id.sbBandLevel)

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
                        if (kotlin.math.abs(currentLevel - lastSentLevel) > 10) {
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
                    if (selectedPresetName != "Custom") {
                        selectedPresetName = "Custom"
                        updatePresetSelectionUI("Custom", getLibraryColor("mc_track"))
                        val bundlePreset = Bundle().apply { putString("PRESET", "Custom") }
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