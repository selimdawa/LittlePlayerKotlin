package com.flatcode.littleplayer.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.DialogSongOptionsBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.requestDeletion
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongOptionsBottomSheet(
    private val song: MusicFiles,
    private val onDeleteClick: (MusicFiles) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogSongOptionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MusicViewModel by activityViewModels()

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            confirmDeletion()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogSongOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dialogTitle.text = song.title

        binding.optionAddToPlaylist.setOnClickListener {
            // TODO: Logic for adding to playlist
            dismiss()
        }

        binding.optionShare.setOnClickListener {
            shareSong(song)
            dismiss()
        }

        binding.optionDelete.setOnClickListener {
            val songId = song.id ?: return@setOnClickListener
            requestDeletion(viewModel.getSongUri(songId), deleteLauncher) {
                confirmDeletion()
            }
        }
    }

    private fun confirmDeletion() {
        onDeleteClick(song)
        dismiss()
    }

    private fun shareSong(song: MusicFiles) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(song.path))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Song"))
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}