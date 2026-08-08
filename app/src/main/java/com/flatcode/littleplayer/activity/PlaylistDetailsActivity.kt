package com.flatcode.littleplayer.activity

import android.R.color.transparent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistDetailsBinding
import com.flatcode.littleplayer.databinding.DialogConfirmDeleteBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistNewBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.observePlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistDetailsViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class PlaylistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding
    private val viewModel: PlaylistDetailsViewModel by viewModels()
    private val playlistsViewModel: PlaylistsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null
    private var currentPlaylistName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentPlaylistName = intent.getStringExtra("PLAYLIST_NAME") ?: "Playlist"
        setupUI()

        viewModel.loadSongs(currentPlaylistName)
        observeViewModel()
    }

    private fun setupUI() {
        initToolbar(currentPlaylistName)
        binding.customToolbar.btnMore.apply {
            isVisible = true
            setOnClickListener { showPlaylistOptionsDialog() }
        }
    }

    private fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songs ->
            if (songs.isNotEmpty()) {
                if (adapter == null) {
                    adapter = MusicAdapter(this, onItemClick = { _, position, view ->
                        musicViewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                        openPlayer(position, view)
                    }, onDeleteClick = { song ->
                        musicViewModel.deleteSong(song)
                    }, onRemoveFromPlaylistClick = { song ->
                        showRemoveSongDialog(song)
                    })
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(songs)
            }
        }

        observePlaybackSync(nowPlayerViewModel, binding.root) { adapter }
    }

    private fun showPlaylistOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist_options, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val btnEdit = dialogView.findViewById<View>(R.id.btnEdit)
        val btnRemove = dialogView.findViewById<View>(R.id.btnRemove)

        dialogTitle.text = currentPlaylistName

        btnEdit.setOnClickListener {
            alertDialog.dismiss()
            showRenamePlaylistDialog()
        }

        btnRemove.setOnClickListener {
            alertDialog.dismiss()
            showDeletePlaylistDialog()
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }

    private fun showDeletePlaylistDialog() {
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        dialogBinding.dialogMessage.text =
            getString(R.string.delete_playlist_message, currentPlaylistName)

        dialogBinding.btnDelete.setOnClickListener {
            playlistsViewModel.deletePlaylist(currentPlaylistName)
            alertDialog.dismiss()
            finish()
        }

        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }

    private fun showRenamePlaylistDialog() {
        val dialogBinding = DialogPlaylistNewBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        dialogBinding.dialogTitle.text = getString(R.string.rename_playlist)
        dialogBinding.editText.setText(currentPlaylistName)
        dialogBinding.btnCreate.text = getString(R.string.rename)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(dialogBinding.editText, InputMethodManager.SHOW_IMPLICIT)
        }

        dialogBinding.btnCreate.setOnClickListener {
            val newName = dialogBinding.editText.text.toString()
            if (newName.isNotEmpty() && newName != currentPlaylistName) {
                playlistsViewModel.renamePlaylist(currentPlaylistName, newName)
                currentPlaylistName = newName
                initToolbar(currentPlaylistName)
                viewModel.loadSongs(currentPlaylistName)
                alertDialog.dismiss()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showRemoveSongDialog(song: MusicFiles) {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_remove, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(view).create()

        val tvMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnRemove = view.findViewById<MaterialButton>(R.id.btnRemove)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        tvMessage.text = getString(R.string.remove_song_from_playlist_message, song.title)

        btnRemove.setOnClickListener {
            val playlistName = intent.getStringExtra("PLAYLIST_NAME") ?: "Playlist"
            viewModel.removeSongFromPlaylist(playlistName, song.id ?: "")
            alertDialog.dismiss()
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }
}