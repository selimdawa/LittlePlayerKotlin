package com.flatcode.littleplayer.activity

import android.R.color.transparent
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistDetailsBinding
import com.flatcode.littleplayer.databinding.DialogConfirmDeleteBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistNewBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistOptionsBinding
import com.flatcode.littleplayer.fragment.SortSongsBottomSheet
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.utils.showKeyboard
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistDetailsViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class PlaylistDetailsActivity :
    BaseActivity<ActivityPlaylistDetailsBinding>(ActivityPlaylistDetailsBinding::inflate) {

    private val viewModel: PlaylistDetailsViewModel by viewModels()
    private val playlistsViewModel: PlaylistsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null
    private var currentPlaylistName: String = ""

    override fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            binding.customToolbar.root.updatePadding(top = systemBars.top)
            insets
        }

        currentPlaylistName =
            intent.getStringExtra(DATA.PLAYLIST_NAME) ?: getString(R.string.playlist)
        setupUI()

        viewModel.loadSongs(currentPlaylistName)
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

    override fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songs ->
            if (adapter == null) {
                adapter = MusicAdapter(this, onItemClick = { _, position, view ->
                    musicViewModel.updatePlaylistAndPlay(
                        adapter?.currentList ?: emptyList(), position, fromUserClick = true
                    )
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

        musicViewModel.event.collectWithLifecycle(this) { event ->
            if (event is MusicEvent.PlaySong && !event.fromUserClick) {
                openPlayer(event.position)
            }
        }
    }

    private fun showPlaylistOptionsDialog() {
        val dialogBinding = DialogPlaylistOptionsBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        alertDialog.setCanceledOnTouchOutside(false)

        dialogBinding.dialogTitle.text = currentPlaylistName

        dialogBinding.btnEdit.setOnClickListener {
            alertDialog.dismiss()
            showRenamePlaylistDialog()
        }

        dialogBinding.btnRemove.setOnClickListener {
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
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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