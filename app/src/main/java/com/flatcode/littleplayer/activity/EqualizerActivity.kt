package com.flatcode.littleplayer.activity

import android.content.ComponentName
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityEqualizerBinding
import com.flatcode.littleplayer.service.MusicService
import com.flatcode.littleplayer.utils.initToolbar
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class EqualizerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEqualizerBinding
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar("Equalizer")
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupBands()
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }

    private fun setupListeners() {
        binding.switchEq.setOnCheckedChangeListener { _, isChecked ->
            val bundle = Bundle().apply { putBoolean("ENABLED", isChecked) }
            mediaController?.sendCustomCommand(
                SessionCommand(MusicService.COMMAND_TOGGLE_EQ, Bundle.EMPTY),
                bundle
            )
        }
    }

    private fun setupBands() {
        val bands = arrayOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
        
        binding.eqContainer.removeAllViews()
        binding.eqContainer.addView(binding.switchEq)

        for (i in bands.indices) {
            val bandView = layoutInflater.inflate(R.layout.item_eq_band, binding.eqContainer, false)
            val tvLabel = bandView.findViewById<TextView>(R.id.tvBandLabel)
            val seekBar = bandView.findViewById<SeekBar>(R.id.sbBandLevel)

            tvLabel.text = bands[i]
            seekBar.max = 3000
            seekBar.progress = 1500

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val level = (progress - 1500).toShort()
                        val bundle = Bundle().apply {
                            putShort("BAND", i.toShort())
                            putShort("LEVEL", level)
                        }
                        mediaController?.sendCustomCommand(
                            SessionCommand(MusicService.COMMAND_SET_EQ_BAND, Bundle.EMPTY),
                            bundle
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