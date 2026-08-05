package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.DialogCustomInputBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistSelectionBinding
import com.flatcode.littleplayer.databinding.ItemPlaylistSmallBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.collectWithLifecycle
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

        val adapter = PlaylistSmallAdapter { playlistName ->
            viewModel.addToPlaylist(playlistName, song)
            dismiss()
        }
        binding.recyclerView.adapter = adapter

        viewModel.playlistNames.collectWithLifecycle(viewLifecycleOwner) { names ->
            adapter.submitList(names)
        }

        binding.btnCreateNewPlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val context = requireContext()
        val dialogBinding = DialogCustomInputBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

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

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

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

    private class PlaylistSmallAdapter(
        private val onItemSelected: (String) -> Unit
    ) : RecyclerView.Adapter<PlaylistSmallAdapter.ViewHolder>() {

        private var items = emptyList<String>()

        fun submitList(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemPlaylistSmallBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val name = items[position]
            holder.binding.name.text = name
            holder.binding.root.setOnClickListener { onItemSelected(name) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(val binding: ItemPlaylistSmallBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}