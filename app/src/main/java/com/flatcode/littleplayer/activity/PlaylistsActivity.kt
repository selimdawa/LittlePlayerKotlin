package com.flatcode.littleplayer.activity

import android.R.color.transparent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.PlaylistAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistsBinding
import com.flatcode.littleplayer.fragment.SortSongsBottomSheet
import com.flatcode.littleplayer.model.Playlist
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.utils.showKeyboard
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistsEvent
import com.flatcode.littleplayer.viewmodel.PlaylistsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
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
            setOnClickListener {}

        }
        binding.toolbarItems.btnAdd.apply {
            isVisible = true
            setOnClickListener { showCreatePlaylistDialog() }
        }
        binding.toolbarItems.btnShuffle.setOnClickListener {
            viewModel.shufflePlaylists()
        }
        binding.toolbarItems.btnShufflePlayback.setOnClickListener {
            viewModel.shufflePlaylists()
        }
        binding.toolbarItems.btnFilterSort.apply {
            isVisible = true
            setOnClickListener {
                val bottomSheet = SortSongsBottomSheet(
                    DATA.PLAYLISTS, viewModel.playlistsSortOrder.value
                ) { category, sortType ->
                    viewModel.updateSortOrder(category, sortType)
                }
                bottomSheet.show(supportFragmentManager, "SortSongsBottomSheet")
            }
        }
    }

    private fun showPlaylistOptionsDialog(playlist: Playlist) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist_options, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        alertDialog.setCanceledOnTouchOutside(false)

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
            showDeletePlaylistDialog(playlist.name)
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }

    private fun showDeletePlaylistDialog(name: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        alertDialog.setCanceledOnTouchOutside(false)

        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val btnDelete =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)
        val btnCancel =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

        dialogMessage.text = getString(R.string.delete_playlist_message, name)

        btnDelete.setOnClickListener {
            viewModel.deletePlaylist(name)
            alertDialog.dismiss()
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }

    private fun showRenamePlaylistDialog(oldName: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist_new, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        alertDialog.setCanceledOnTouchOutside(false)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val editText =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editText)
        val btnCreate =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCreate)
        val btnCancel =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

        dialogTitle.text = getString(R.string.rename_playlist)
        editText.setText(oldName)
        btnCreate.text = getString(R.string.rename)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        alertDialog.setOnShowListener {
            editText.requestFocus()
            editText.showKeyboard()
        }

        btnCreate.setOnClickListener {
            val newName = editText.text.toString()
            if (newName.isNotEmpty() && newName != oldName) {
                viewModel.renamePlaylist(oldName, newName)
                alertDialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showCreatePlaylistDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist_new, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        alertDialog.setCanceledOnTouchOutside(false)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val editText =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editText)
        val btnCreate =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCreate)
        val btnCancel =
            dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)

        dialogTitle.text = getString(R.string.new_playlist)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        alertDialog.setOnShowListener {
            editText.requestFocus()
            editText.showKeyboard()
        }

        btnCreate.setOnClickListener {
            val name = editText.text.toString()
            if (name.isNotEmpty()) {
                viewModel.createPlaylist(name)
                alertDialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
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
                }, onMoreClick = { playlist, _ ->
                    showPlaylistOptionsDialog(playlist)
                })
                binding.recyclerView.adapter = adapter
            }
            adapter?.submitList(playlists)
        }

        viewModel.event.collectWithLifecycle(this) { event ->
            when (event) {
                is PlaylistsEvent.PlaySong -> {
                    openPlayer(event.position)
                }
            }
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }
}