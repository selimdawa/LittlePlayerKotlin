package com.flatcode.littleplayer.fragment

import android.app.Activity
import android.R.color.transparent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.media3.session.MediaController
import androidx.media3.cast.MediaRouteButtonFactory
import com.flatcode.littleplayer.databinding.DialogPlayerOptionsBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.activity.InfoEditActivity
import com.flatcode.littleplayer.activity.EqualizerActivity
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.utils.requestDeletion
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.flatcode.littleplayer.R
import com.google.android.material.R as MaterialR
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PlayerOptionsBottomSheet(
    private val song: MusicFiles?,
    private val mediaController: MediaController?,
    private val onDeleteClick: (MusicFiles) -> Unit = {},
    private val onCastClick: () -> Unit,
) : BottomSheetDialogFragment() {

    private var _binding: DialogPlayerOptionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MusicViewModel by activityViewModels()

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            song?.let { confirmDeletion(it) }
        }
    }

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
            val intent = Intent(requireContext(), InfoEditActivity::class.java).apply {
                putExtra(DATA.SONG, song)
            }
            startActivity(intent)
            dismiss()
        }

        binding.optionAddToPlaylist.setOnClickListener {
            song?.let {
                val playlistSheet = PlaylistSelectionBottomSheet(it)
                playlistSheet.show(parentFragmentManager, "PlaylistSelectionBottomSheet")
            }
            dismiss()
        }

        binding.optionDelete.setOnClickListener {
            val songId = song?.id ?: return@setOnClickListener
            requestDeletion(viewModel.getSongUri(songId), deleteLauncher) {
                confirmDeletion(song)
            }
        }

        binding.optionEqualizer.setOnClickListener {
            val intent = Intent(requireContext(), EqualizerActivity::class.java)
            startActivity(intent)
            dismiss()
        }
    }

    private fun confirmDeletion(song: MusicFiles) {
        onDeleteClick(song)
        dismiss()
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