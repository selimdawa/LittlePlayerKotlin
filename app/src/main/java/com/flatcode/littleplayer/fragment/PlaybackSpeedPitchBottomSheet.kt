package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import com.flatcode.littleplayer.databinding.DialogPlaybackSpeedPitchBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class PlaybackSpeedPitchBottomSheet(
    private val mediaController: MediaController?
) : BottomSheetDialogFragment() {

    private var _binding: DialogPlaybackSpeedPitchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlaybackSpeedPitchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentParams = mediaController?.playbackParameters ?: PlaybackParameters.DEFAULT
        
        setupSpeedSlider(currentParams.speed)
        setupPitchSlider(currentParams.pitch)

        binding.btnReset.setOnClickListener {
            updateParameters(1.0f, 1.0f)
            binding.speedSlider.value = 1.0f
            binding.pitchSlider.value = 1.0f
            updateSpeedValueText(1.0f)
            updatePitchValueText(1.0f)
        }
    }

    private fun setupSpeedSlider(currentSpeed: Float) {
        binding.speedSlider.value = currentSpeed.coerceIn(0.25f, 2.0f)
        updateSpeedValueText(binding.speedSlider.value)
        
        binding.speedSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                updateParameters(speed = value)
                updateSpeedValueText(value)
            }
        }
    }

    private fun setupPitchSlider(currentPitch: Float) {
        binding.pitchSlider.value = currentPitch.coerceIn(0.5f, 2.0f)
        updatePitchValueText(binding.pitchSlider.value)

        binding.pitchSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                updateParameters(pitch = value)
                updatePitchValueText(value)
            }
        }
    }

    private fun updateParameters(speed: Float? = null, pitch: Float? = null) {
        val current = mediaController?.playbackParameters ?: PlaybackParameters.DEFAULT
        val newSpeed = speed ?: current.speed
        val newPitch = pitch ?: current.pitch
        mediaController?.playbackParameters = PlaybackParameters(newSpeed, newPitch)
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
