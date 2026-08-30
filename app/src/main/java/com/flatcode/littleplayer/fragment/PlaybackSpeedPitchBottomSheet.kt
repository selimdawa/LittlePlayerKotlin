package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import com.flatcode.littleplayer.databinding.DialogPlaybackSpeedPitchBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class PlaybackSpeedPitchBottomSheet(
    private val mediaController: MediaController?
) : BottomSheetDialogFragment() {

    private var _binding: DialogPlaybackSpeedPitchBinding? = null
    private val binding get() = _binding!!
    private var updateJob: Job? = null

    private var currentSpeed = 1.0f
    private var currentPitch = 1.0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlaybackSpeedPitchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val params = mediaController?.playbackParameters ?: PlaybackParameters.DEFAULT
        currentSpeed = params.speed
        currentPitch = params.pitch
        
        setupSpeedSlider(currentSpeed)
        setupPitchSlider(currentPitch)

        binding.btnReset.setOnClickListener {
            currentSpeed = 1.0f
            currentPitch = 1.0f
            updateParameters()
            binding.speedSlider.value = 1.0f
            binding.pitchSlider.value = 1.0f
            updateSpeedValueText(1.0f)
            updatePitchValueText(1.0f)
        }
    }

    private fun setupSpeedSlider(initialSpeed: Float) {
        binding.speedSlider.value = initialSpeed.coerceIn(0.25f, 2.0f)
        updateSpeedValueText(binding.speedSlider.value)
        
        binding.speedSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentSpeed = value
                updateParameters()
                updateSpeedValueText(value)
            }
        }
    }

    private fun setupPitchSlider(initialPitch: Float) {
        binding.pitchSlider.value = initialPitch.coerceIn(0.5f, 2.0f)
        updatePitchValueText(binding.pitchSlider.value)

        binding.pitchSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentPitch = value
                updateParameters()
                updatePitchValueText(value)
            }
        }
    }

    private fun updateParameters() {
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            delay(150) // Increased debounce for better stability
            if (mediaController?.isCommandAvailable(androidx.media3.common.Player.COMMAND_SET_SPEED_AND_PITCH) == true) {
                mediaController?.playbackParameters = PlaybackParameters(currentSpeed, currentPitch)
            }
        }
    }

    private fun updateSpeedValueText(value: Float) {
        binding.speedValue.text = String.format(Locale.US, "%.2fx", value)
    }

    private fun updatePitchValueText(value: Float) {
        binding.pitchValue.text = String.format(Locale.US, "%.2fx", value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
