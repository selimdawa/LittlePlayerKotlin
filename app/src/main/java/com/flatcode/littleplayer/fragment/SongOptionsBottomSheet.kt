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
import com.flatcode.littleplayer.databinding.DialogSongOptionsBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.gone
import com.flatcode.littleplayer.utils.requestDeletion
import com.flatcode.littleplayer.utils.visible
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.flatcode.littleplayer.R
import com.google.android.material.R as MaterialR

class SongOptionsBottomSheet(
    private val song: MusicFiles,
    private val onDeleteClick: (MusicFiles) -> Unit,
    private val onRemoveFromPlaylistClick: ((MusicFiles) -> Unit)? = null
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

        binding.title.text = song.title

        binding.optionAddToPlaylist.setOnClickListener {
            val playlistSheet = PlaylistSelectionBottomSheet(song)
            playlistSheet.show(parentFragmentManager, "PlaylistSelectionBottomSheet")
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

        if (onRemoveFromPlaylistClick != null) {
            binding.optionRemove.visible()
            binding.optionRemove.setOnClickListener {
                showRemoveFromPlaylistConfirmation()
            }
        } else {
            binding.optionRemove.gone()
        }
    }

    private fun showRemoveFromPlaylistConfirmation() {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_remove, null)
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()

        val tvMessage = view.findViewById<android.widget.TextView>(R.id.dialogMessage)
        val btnRemove = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemove)
        val btnCancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

        tvMessage.text = getString(R.string.remove_song_from_playlist_message, song.title)

        btnRemove.setOnClickListener {
            onRemoveFromPlaylistClick?.invoke(song)
            alertDialog.dismiss()
            dismiss()
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            findViewById<View>(MaterialR.id.design_bottom_sheet)?.setBackgroundResource(
                android.R.color.transparent
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}