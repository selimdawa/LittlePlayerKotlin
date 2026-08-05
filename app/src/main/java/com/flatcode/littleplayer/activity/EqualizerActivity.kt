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
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.atan2

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
                updatePresetSelectionUI(
                    selectedPresetName, nowPlayerViewModel.currentThemeColor.value ?: Color.GRAY
                )
            }
        }

        nowPlayerViewModel.currentThemeColor.collectWithLifecycle(this) { color ->
            color?.let {
                val csl = ColorStateList.valueOf(it)
                binding.switchEq.thumbTintList = csl
                binding.bassKnob.setIndicatorColor(it)
                binding.virtualizerKnob.setIndicatorColor(it)

                bandSeekBars.forEach { sb ->
                    sb.progressTintList = csl
                    sb.thumbTintList = csl
                }
                updatePresetSelectionUI(selectedPresetName, it)
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

        binding.bassKnob.setOnTouchListener { v, event ->
            handleKnobTouch(v as CircularProgressIndicator, event) { progress ->
                sendBassStrength(progress * 10)
            }
            true
        }

        binding.virtualizerKnob.setOnTouchListener { v, event ->
            handleKnobTouch(v as CircularProgressIndicator, event) { progress ->
                sendVirtualizerStrength(progress * 10)
            }
            true
        }

        binding.btnPresetCustom.setOnClickListener { selectPreset("Custom") }
        binding.btnPresetPop.setOnClickListener { selectPreset("Pop") }
        binding.btnPresetRock.setOnClickListener { selectPreset("Rock") }
        binding.btnPresetJazz.setOnClickListener { selectPreset("Jazz") }
        binding.btnPresetClassical.setOnClickListener { selectPreset("Classical") }
        binding.btnPresetDance.setOnClickListener { selectPreset("Dance") }
        binding.btnPresetFlat.setOnClickListener { selectPreset("Flat") }
    }

    private fun handleKnobTouch(
        knob: CircularProgressIndicator, event: MotionEvent, onProgressChanged: (Int) -> Unit
    ) {
        val x = event.x - (knob.width / 2)
        val y = (knob.height / 2) - event.y
        val angle = Math.toDegrees(atan2(x.toDouble(), y.toDouble())).toFloat()

        val normalizedAngle = if (angle < 0) angle + 360 else angle
        val progress = (normalizedAngle / 3.6f).toInt().coerceIn(0, 100)

        knob.progress = progress
        if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_UP) {
            onProgressChanged(progress)
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
        val themeColor = nowPlayerViewModel.currentThemeColor.value ?: Color.GRAY
        selectedPresetName = name
        updatePresetSelectionUI(name, themeColor)

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
    }

    private fun updatePresetSelectionUI(selected: String?, themeColor: Int) {
        val containerBg = getLibraryColor("colorSurfaceContainerHigh")

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

        when (selected) {
            "Custom" -> {
                binding.cardCustom.setCardBackgroundColor(themeColor); binding.textCustom.setTextColor(
                    Color.WHITE
                )
            }

            "Pop" -> {
                binding.cardPop.setCardBackgroundColor(themeColor); binding.textPop.setTextColor(
                    Color.WHITE
                )
            }

            "Rock" -> {
                binding.cardRock.setCardBackgroundColor(themeColor); binding.textRock.setTextColor(
                    Color.WHITE
                )
            }

            "Jazz" -> {
                binding.cardJazz.setCardBackgroundColor(themeColor); binding.textJazz.setTextColor(
                    Color.WHITE
                )
            }

            "Classical" -> {
                binding.cardClassical.setCardBackgroundColor(themeColor); binding.textClassical.setTextColor(
                    Color.WHITE
                )
            }

            "Dance" -> {
                binding.cardDance.setCardBackgroundColor(themeColor); binding.textDance.setTextColor(
                    Color.WHITE
                )
            }

            "Flat" -> {
                binding.cardFlat.setCardBackgroundColor(themeColor); binding.textFlat.setTextColor(
                    Color.WHITE
                )
            }
        }
    }

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

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val dbLevel = (progress - 1500) / 100
                    tvLevel.text =
                        getString(R.string.db_format, if (dbLevel > 0) "+" else "", dbLevel)

                    if (fromUser) {
                        selectedPresetName = "Custom"
                        updatePresetSelectionUI(
                            "Custom", nowPlayerViewModel.currentThemeColor.value ?: Color.GRAY
                        )
                        val bundlePreset = Bundle().apply { putString("PRESET", "Custom") }
                        mediaController?.sendCustomCommand(
                            SessionCommand(MusicService.COMMAND_SET_PRESET, Bundle.EMPTY),
                            bundlePreset
                        )
                        val level = (progress - 1500).toShort()
                        val bundle = Bundle().apply {
                            putShort("BAND", i.toShort())
                            putShort("LEVEL", level)
                        }
                        mediaController?.sendCustomCommand(
                            SessionCommand(MusicService.COMMAND_SET_EQ_BAND, Bundle.EMPTY), bundle
                        )
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            binding.eqContainer.addView(bandView)
        }
    }
}