package com.flatcode.littleplayer.activity

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.PlaylistAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistsBinding
import com.flatcode.littleplayer.databinding.DialogConfirmDeleteBinding
import com.flatcode.littleplayer.databinding.DialogCustomInputBinding
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
            setNavigationOnClickListener { finish() }
        }
        binding.addPlaylist.setOnClickListener { showCreatePlaylistDialog() }
        setupSwipeActions()
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val adapter = binding.recyclerView.adapter as? PlaylistAdapter ?: return
                // We need to get the playlist from the viewmodel or adapter.
                // Since PlaylistAdapter doesn't expose the list, we might need to modify it or use the viewmodel.
                // Better to get it from the viewmodel's current state.
                val playlist = viewModel.playlists.value.getOrNull(position) ?: return

                if (direction == ItemTouchHelper.RIGHT) {
                    // Delete
                    showDeletePlaylistDialog(playlist.name, position)
                } else {
                    // Edit
                    showRenamePlaylistDialog(playlist.name)
                    adapter.notifyItemChanged(position)
                }
            }

            override fun onChildDraw(
                c: android.graphics.Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val foregroundView = (vh as PlaylistAdapter.PlaylistViewHolder).binding.foregroundCard
                getDefaultUIUtil().onDraw(
                    c, rv, foregroundView, dX, dY, actionState, isCurrentlyActive
                )
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                val foregroundView = (viewHolder as PlaylistAdapter.PlaylistViewHolder).binding.foregroundCard
                ItemTouchHelper.Callback.getDefaultUIUtil().clearView(foregroundView)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerView)
    }

    private fun showDeletePlaylistDialog(name: String, position: Int) {
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

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

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun showRenamePlaylistDialog(oldName: String) {
        val dialogBinding = DialogCustomInputBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.dialogTitle.text = getString(R.string.rename_playlist)
        dialogBinding.editText.setText(oldName)
        dialogBinding.btnCreate.text = getString(R.string.rename)

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(dialogBinding.editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
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
        val dialogBinding = DialogCustomInputBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.dialogTitle.text = getString(R.string.new_playlist)

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(dialogBinding.editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
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

    private fun observeViewModel() {
        viewModel.playlists.collectWithLifecycle(this) { playlists ->
            binding.emptyState.isVisible = playlists.isEmpty()
            binding.recyclerView.adapter = PlaylistAdapter(playlists) { playlistName ->
                launchActivity<PlaylistDetailsActivity> {
                    putExtra("PLAYLIST_NAME", playlistName)
                }
            }
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }
}