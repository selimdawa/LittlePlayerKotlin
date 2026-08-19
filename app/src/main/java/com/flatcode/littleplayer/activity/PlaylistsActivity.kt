package com.flatcode.littleplayer.activity

import android.R.color.transparent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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
        val btnDelete = dialogView.findViewById<MaterialButton>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

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
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editText)
        val btnCreate = dialogView.findViewById<MaterialButton>(R.id.btnCreate)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        dialogTitle.text = getString(R.string.rename_playlist)
        editText.setText(oldName)
        btnCreate.text = getString(R.string.rename)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editText)
        val btnCreate = dialogView.findViewById<MaterialButton>(R.id.btnCreate)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        dialogTitle.text = getString(R.string.new_playlist)

        alertDialog.window?.setBackgroundDrawableResource(transparent)
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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

                // Force refresh themed icons in the list
                adapter?.let {
                    for (i in 0 until binding.recyclerView.childCount) {
                        val view = binding.recyclerView.getChildAt(i)
                        val holder =
                            binding.recyclerView.getChildViewHolder(view) as? PlaylistAdapter.PlaylistViewHolder
                        holder?.let { h ->
                            if (h.binding.playlistImage.getTag(R.id.image_model_tag) is Int) {
                                h.binding.playlistImage.setTag(R.id.image_model_tag, null)
                            }
                            if (h.binding.playlistImageBlur.getTag(R.id.image_model_tag) is Int) {
                                h.binding.playlistImageBlur.setTag(R.id.image_model_tag, null)
                            }
                        }
                    }
                    it.notifyDataSetChanged()
                }
            }
        }
    }
}