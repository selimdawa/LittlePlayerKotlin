package com.flatcode.littleplayer.fragment

import android.R.color.transparent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.session.MediaController
import androidx.media3.cast.MediaRouteButtonFactory
import com.flatcode.littleplayer.databinding.DialogPlayerOptionsBinding
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.activity.EditInfoActivity
import com.flatcode.littleplayer.utils.DATA
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.flatcode.littleplayer.R
import com.google.android.material.R as MaterialR
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PlayerOptionsBottomSheet(
    private val song: MusicFiles?,
    private val mediaController: MediaController?,
    private val onCastClick: () -> Unit,
) : BottomSheetDialogFragment() {

    private var _binding: DialogPlayerOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlayerOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        song?.let {
            binding.title.text = it.title
        }

        binding.optionSpeed.setOnClickListener {
            val speedSheet = PlaybackSpeedPitchBottomSheet(mediaController)
            speedSheet.show(parentFragmentManager, "PlaybackSpeedPitch")
            dismiss()
        }

        binding.optionSleep.setOnClickListener {
            val sleepSheet = SleepTimerBottomSheet { _, status ->
                binding.tvSleepTimerStatus.text = status
            }
            sleepSheet.show(parentFragmentManager, "SleepTimer")
        }

        binding.optionShare.setOnClickListener {
            song?.let { shareSong(it) }
            dismiss()
        }

        binding.optionCast.setOnClickListener {
            onCastClick()
            dismiss()
        }

        MediaRouteButtonFactory.setUpMediaRouteButton(
            requireContext(),
            binding.dialogMediaRouteButton
        )

        binding.optionEdit.setOnClickListener {
            val intent = Intent(requireContext(), EditInfoActivity::class.java).apply {
                putExtra(DATA.SONG, song)
            }
            startActivity(intent)
            dismiss()
        }
    }

    private fun shareSong(song: MusicFiles) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(song.path))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_song)))
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            findViewById<View>(MaterialR.id.design_bottom_sheet)?.setBackgroundResource(
                transparent
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}