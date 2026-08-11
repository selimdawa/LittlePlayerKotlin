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
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistDetailsBinding
import com.flatcode.littleplayer.databinding.DialogConfirmDeleteBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistNewBinding
import com.flatcode.littleplayer.fragment.SortSongsBottomSheet
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.utils.showKeyboard
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistDetailsEvent
import com.flatcode.littleplayer.viewmodel.PlaylistDetailsViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistsViewModel
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

        currentPlaylistName = intent.getStringExtra("PLAYLIST_NAME") ?: getString(R.string.playlist)
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
        binding.toolbarItems.btnShuffle.setOnClickListener {
            viewModel.shuffleSongs()
        }
        binding.toolbarItems.btnShufflePlayback.setOnClickListener {
            viewModel.shuffleSongs()
        }
        binding.toolbarItems.btnFilterSort.apply {
            isVisible = true
            setOnClickListener {
                val bottomSheet = SortSongsBottomSheet(
                    DATA.PLAYLIST_DETAILS, viewModel.songsSortOrder.value
                ) { category, sortType ->
                    viewModel.updateSortOrder(category, sortType)
                }
                bottomSheet.show(supportFragmentManager, "SortSongsBottomSheet")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songs ->
            if (adapter == null) {
                adapter = MusicAdapter(this, onItemClick = { _, position, view ->
                    musicViewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                    openPlayer(position, view)
                }, onDeleteClick = { song ->
                    musicViewModel.deleteSong(song)
                }, onRemoveFromPlaylistClick = { song ->
                    viewModel.removeSongFromPlaylist(currentPlaylistName, song.id ?: "")
                }).apply {
                    bindToPlaybackSync(
                        this@PlaylistDetailsActivity, nowPlayerViewModel, binding.root
                    )
                }
                binding.recyclerView.adapter = adapter
            }
            adapter?.submitList(songs)
        }

        viewModel.event.collectWithLifecycle(this) { event ->
            when (event) {
                is PlaylistDetailsEvent.PlaySong -> {
                    openPlayer(event.position)
                }
            }
        }
    }

    private fun showPlaylistOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_playlist_options, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        alertDialog.setCanceledOnTouchOutside(false)

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

        alertDialog.setCanceledOnTouchOutside(false)

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

        alertDialog.setCanceledOnTouchOutside(false)

        dialogBinding.dialogTitle.text = getString(R.string.rename_playlist)
        dialogBinding.editText.setText(currentPlaylistName)
        dialogBinding.btnCreate.text = getString(R.string.rename)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        alertDialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            dialogBinding.editText.showKeyboard()
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
}