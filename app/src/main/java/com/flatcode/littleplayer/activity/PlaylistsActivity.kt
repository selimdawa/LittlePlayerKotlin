package com.flatcode.littleplayer.activity

import android.R.color.transparent
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.PlaylistAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistsBinding
import com.flatcode.littleplayer.databinding.DialogConfirmDeleteBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistNewBinding
import com.flatcode.littleplayer.databinding.DialogPlaylistOptionsBinding
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
import io.selimdawa.multicolors.MultiColorManager
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class PlaylistsActivity :
    BaseActivity<ActivityPlaylistsBinding>(ActivityPlaylistsBinding::inflate) {

    private val viewModel: PlaylistsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    override fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            binding.customToolbar.root.updatePadding(top = systemBars.top)
            insets
        }

        setupUI()
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
        val dialogBinding = DialogPlaylistOptionsBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        alertDialog.setCanceledOnTouchOutside(false)

        dialogBinding.dialogTitle.text = playlist.name

        dialogBinding.btnEdit.setOnClickListener {
            alertDialog.dismiss()
            showRenamePlaylistDialog(playlist.name)
        }

        dialogBinding.btnRemove.setOnClickListener {
            alertDialog.dismiss()
            showDeletePlaylistDialog(playlist.name)
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }

    private fun showDeletePlaylistDialog(name: String) {
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        alertDialog.setCanceledOnTouchOutside(false)

        dialogBinding.dialogMessage.text = getString(R.string.delete_playlist_message, name)

        dialogBinding.btnDelete.setOnClickListener {
            viewModel.deletePlaylist(name)
            alertDialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.show()
    }

    private fun showRenamePlaylistDialog(oldName: String) {
        val dialogBinding = DialogPlaylistNewBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        alertDialog.setCanceledOnTouchOutside(false)

        dialogBinding.dialogTitle.text = getString(R.string.rename_playlist)
        dialogBinding.editText.setText(oldName)
        dialogBinding.btnCreate.text = getString(R.string.rename)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        alertDialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            dialogBinding.editText.showKeyboard()
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

        alertDialog.setCanceledOnTouchOutside(false)

        dialogBinding.dialogTitle.text = getString(R.string.new_playlist)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        alertDialog.setOnShowListener {
            dialogBinding.editText.requestFocus()
            dialogBinding.editText.showKeyboard()
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

    override fun observeViewModel() {
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

        lifecycleScope.launch {
            MultiColorManager.currentThemeId.collect {
                MultiColorManager.applyTheme(this@PlaylistsActivity)

                // Force refresh themed icons in the list using targeted payload
                adapter?.let { a ->
                    a.notifyItemRangeChanged(0, a.itemCount, PlaylistAdapter.PAYLOAD_THEME_REFRESH)
                }
            }
        }
    }
}