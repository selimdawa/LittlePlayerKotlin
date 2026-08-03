package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.DialogPlaylistSelectionBinding
import com.flatcode.littleplayer.databinding.ItemPlaylistSmallBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.viewmodel.PlaylistsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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
        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 24, 48, 24)
        val editText = EditText(context)
        editText.layoutParams = params
        editText.hint = "Playlist Name"
        container.addView(editText)

        AlertDialog.Builder(context).setTitle("New Playlist").setView(container)
            .setPositiveButton("Create") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotEmpty()) {
                    viewModel.addToPlaylist(name, song)
                    dismiss()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

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