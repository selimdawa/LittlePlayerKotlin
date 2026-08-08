package com.flatcode.littleplayer.fragment

import android.R.color.transparent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.DialogPlaylistNewBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistSelectionBinding
import com.flatcode.littleplayer.databinding.ItemPlaylistSmallBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.showKeyboard
import com.flatcode.littleplayer.viewmodel.PlaylistsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.R as MaterialR

class PlaylistSelectionBottomSheet(
    private val song: MusicFiles
) : BottomSheetDialogFragment() {

    private var _binding: DialogPlaylistSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlaylistsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlaylistSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.syncPlaylists()

        val adapter = PlaylistSmallAdapter { playlistName ->
            viewModel.addToPlaylist(playlistName, song)
            dismiss()
        }
        binding.recyclerView.adapter = adapter

        viewModel.getPlaylistsNotContainingSong(song.id ?: "")
            .collectWithLifecycle(viewLifecycleOwner) { names ->
                adapter.submitList(names)
                binding.emptyState.isVisible = names.isEmpty()
                binding.recyclerView.isVisible = names.isNotEmpty()
            }

        binding.btnCreateNewPlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val context = requireContext()
        val dialogBinding = DialogPlaylistNewBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context).setView(dialogBinding.root).create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawableResource(transparent)

        dialogBinding.btnCreate.setOnClickListener {
            val name = dialogBinding.editText.text.toString().trim()
            if (name.isNotEmpty()) {
                viewModel.addToPlaylist(name, song)
                dialog.dismiss()
                dismiss()
            } else {
                dialogBinding.inputLayout.error = getString(R.string.playlist_name)
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            dialogBinding.editText.showKeyboard()
        }

        dialog.show()
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

    private class PlaylistSmallAdapter(
        private val onItemSelected: (String) -> Unit
    ) : ListAdapter<String, PlaylistSmallAdapter.ViewHolder>(StringDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPlaylistSmallBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val name = getItem(position)
            holder.binding.name.text = name
            holder.binding.root.setOnClickListener { onItemSelected(name) }
        }

        class ViewHolder(val binding: ItemPlaylistSmallBinding) :
            RecyclerView.ViewHolder(binding.root)
    }

    private class StringDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem
    }
}