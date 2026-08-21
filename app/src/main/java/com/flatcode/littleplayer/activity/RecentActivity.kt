package com.flatcode.littleplayer.activity

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityRecentBinding
import com.flatcode.littleplayer.databinding.DialogConfirmRemoveBinding
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.RecentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.R as AppCompatR
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class RecentActivity : BaseActivity<ActivityRecentBinding>(ActivityRecentBinding::inflate) {

    private val viewModel: RecentViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun setupViews() {
        applyEdgeToEdge(topView = binding.customToolbar.root)

        initToolbar(getString(R.string.recent))
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.customToolbar.btnMore.apply {
            isVisible = true
            setOnClickListener {
                showClearRecentDialog()
            }
        }
    }

    private fun showClearRecentDialog() {
        val dialogBinding = DialogConfirmRemoveBinding.inflate(layoutInflater)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(dialogBinding.root).create()

        alertDialog.setCanceledOnTouchOutside(false)

        dialogBinding.dialogTitle.text = getString(R.string.clear_recent_history)
        dialogBinding.dialogMessage.text = getString(R.string.clear_recent_message)
        dialogBinding.btnRemove.text = getString(R.string.clear)

        // Force colors to ?attr/colorError
        val errorColor = getLibraryColor(AppCompatR.attr.colorError)
        dialogBinding.dialogTitle.setTextColor(errorColor)
        dialogBinding.dialogMessage.setTextColor(errorColor)
        dialogBinding.btnCancel.setTextColor(errorColor)
        dialogBinding.btnRemove.setTextColor(errorColor)

        // Make buttons consistent (Text style for both if we want colorError text)
        dialogBinding.btnRemove.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        dialogBinding.btnRemove.rippleColor = ColorStateList.valueOf(errorColor).withAlpha(30)

        dialogBinding.btnRemove.setOnClickListener {
            viewModel.clearRecent()
            alertDialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    override fun observeViewModel() {
        viewModel.recentSongs.collectWithLifecycle(this) { songs ->
            binding.emptyState.isVisible = songs.isEmpty()
            binding.customToolbar.btnMore.isVisible = songs.isNotEmpty()

            if (adapter == null && songs.isNotEmpty()) {
                adapter = MusicAdapter(
                    context = this, onItemClick = { _, position, view ->
                    musicViewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                    openPlayer(position, view)
                }, onDeleteClick = { song ->
                    musicViewModel.deleteSong(song)
                }, onRemoveFromPlaylistClick = { song ->
                    viewModel.removeFromRecent(song)
                }, removeLabel = getString(R.string.remove_from_recent)
                ).apply {
                    bindToPlaybackSync(this@RecentActivity, nowPlayerViewModel, binding.root)
                }
                binding.recyclerView.adapter = adapter
                binding.recyclerView.itemAnimator = null
            }
            adapter?.submitList(songs)
        }
    }
}