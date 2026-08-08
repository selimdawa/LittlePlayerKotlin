package com.flatcode.littleplayer.activity

import android.R.color.transparent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.PlaylistAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistsBinding
import com.flatcode.littleplayer.databinding.DialogConfirmDeleteBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistNewBinding
import com.flatcode.littleplayer.model.Playlist
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistsBinding
    private val viewModel: PlaylistsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.customToolbar.root.apply {
            title = getString(R.string.playlists)
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        binding.customToolbar.btnMore.apply {
            isVisible = true
            setOnClickListener {
                // Show global playlist options if needed, or maybe just leave it for now
            }
        }
        binding.toolbarItems.btnAdd.apply {
            isVisible = true
            setOnClickListener { showCreatePlaylistDialog() }
        }
    }

    private fun showPlaylistOptionsDialog(playlist: Playlist, position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist_options, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val btnEdit = dialogView.findViewById<View>(R.id.btnEdit)
        val btnRemove = dialogView.findViewById<View>(R.id.btnRemove)

        dialogTitle.text = playlist.name

        btnEdit.setOnClickListener {
            alertDialog.dismiss()
            showRenamePlaylistDialog(playlist.name)
        }

        btnRemove.setOnClickListener {
            alertDialog.dismiss()
            showDeletePlaylistDialog(playlist.name, position)
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }

    private fun showDeletePlaylistDialog(name: String, position: Int) {
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        dialogBinding.dialogMessage.text = getString(R.string.delete_playlist_message, name)

        dialogBinding.btnDelete.setOnClickListener {
            viewModel.deletePlaylist(name)
            alertDialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.setOnDismissListener {
            binding.recyclerView.adapter?.notifyItemChanged(position)
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }

    private fun showRenamePlaylistDialog(oldName: String) {
        val dialogBinding = DialogPlaylistNewBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        dialogBinding.dialogTitle.text = getString(R.string.rename_playlist)
        dialogBinding.editText.setText(oldName)
        dialogBinding.btnCreate.text = getString(R.string.rename)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(
                dialogBinding.editText, InputMethodManager.SHOW_IMPLICIT
            )
        }

        dialogBinding.btnCreate.setOnClickListener {
            val newName = dialogBinding.editText.text.toString()
            if (newName.isNotEmpty() && newName != oldName) {
                viewModel.renamePlaylist(oldName, newName)
                alertDialog.dismiss()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showCreatePlaylistDialog() {
        val dialogBinding = DialogPlaylistNewBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        dialogBinding.dialogTitle.text = getString(R.string.new_playlist)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(
                dialogBinding.editText, InputMethodManager.SHOW_IMPLICIT
            )
        }

        dialogBinding.btnCreate.setOnClickListener {
            val name = dialogBinding.editText.text.toString()
            if (name.isNotEmpty()) {
                viewModel.createPlaylist(name)
                alertDialog.dismiss()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private var adapter: PlaylistAdapter? = null

    private fun observeViewModel() {
        viewModel.playlists.collectWithLifecycle(this) { playlists ->
            binding.emptyState.isVisible = playlists.isEmpty()
            if (adapter == null) {
                adapter = PlaylistAdapter(onItemClick = { playlistName ->
                    launchActivity<PlaylistDetailsActivity> {
                        putExtra("PLAYLIST_NAME", playlistName)
                    }
                }, onMoreClick = { playlist, position ->
                    showPlaylistOptionsDialog(playlist, position)
                })
                binding.recyclerView.adapter = adapter
            }
            adapter?.submitList(playlists)
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }
}