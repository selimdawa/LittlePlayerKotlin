package com.flatcode.littleplayer.fragment

import android.R.color.transparent
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.InfoEditActivity
import com.flatcode.littleplayer.databinding.DialogConfirmRemoveBinding
import com.flatcode.littleplayer.databinding.DialogSongOptionsBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.gone
import com.flatcode.littleplayer.utils.requestDeletion
import com.flatcode.littleplayer.utils.visible
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.R as MaterialR
import androidx.appcompat.R as AppCompatR

class SongOptionsBottomSheet(
    private val song: MusicFiles,
    private val onDeleteClick: (MusicFiles) -> Unit,
    private val onRemoveFromPlaylistClick: ((MusicFiles) -> Unit)? = null,
    private val removeLabel: String? = null
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

        binding.optionEditTags.setOnClickListener {
            val intent = Intent(requireContext(), InfoEditActivity::class.java).apply {
                putExtra(DATA.SONG, song)
            }
            startActivity(intent)
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
            removeLabel?.let { binding.optionRemoveText.text = it }
            binding.optionRemove.setOnClickListener {
                showRemoveFromPlaylistConfirmation()
            }
        } else {
            binding.optionRemove.gone()
        }
    }

    private fun showRemoveFromPlaylistConfirmation() {
        val dialogBinding = DialogConfirmRemoveBinding.inflate(layoutInflater)
        val alertDialog =
            MaterialAlertDialogBuilder(requireContext()).setView(dialogBinding.root).create()

        alertDialog.setCanceledOnTouchOutside(false)

        if (removeLabel != null) {
            dialogBinding.dialogTitle.text = removeLabel
        }

        dialogBinding.dialogMessage.text =
            getString(R.string.remove_song_from_playlist_message, song.title)

        // Force colors to ?attr/colorError
        val errorColor = requireContext().getLibraryColor(AppCompatR.attr.colorError)
        dialogBinding.dialogTitle.setTextColor(errorColor)
        dialogBinding.dialogMessage.setTextColor(errorColor)
        dialogBinding.btnCancel.setTextColor(errorColor)
        dialogBinding.btnRemove.setTextColor(errorColor)

        // Text style for button
        dialogBinding.btnRemove.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        dialogBinding.btnRemove.rippleColor = ColorStateList.valueOf(errorColor).withAlpha(30)

        dialogBinding.btnRemove.setOnClickListener {
            onRemoveFromPlaylistClick?.invoke(song)
            alertDialog.dismiss()
            dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
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
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_song)))
    }

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(false)
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