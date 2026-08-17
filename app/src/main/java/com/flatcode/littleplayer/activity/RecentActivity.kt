package com.flatcode.littleplayer.activity

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityRecentBinding
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getColorFromAttr
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.RecentViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.R as MaterialR

@UnstableApi
@AndroidEntryPoint
class RecentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecentBinding
    private val viewModel: RecentViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.recent))
        setupToolbar()
        observeViewModel()
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
        val view = layoutInflater.inflate(R.layout.dialog_confirm_remove, null)
        val alertDialog = MaterialAlertDialogBuilder(this).setView(view).create()

        alertDialog.setCanceledOnTouchOutside(false)

        val tvTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val tvMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnClear = view.findViewById<MaterialButton>(R.id.btnRemove)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        tvTitle.text = getString(R.string.clear_recent_history)
        tvMessage.text = getString(R.string.clear_recent_message)
        btnClear.text = getString(R.string.clear)

        // Force colors to ?attr/colorError
        val errorColor = getLibraryColor("colorError")
        tvTitle.setTextColor(errorColor)
        tvMessage.setTextColor(errorColor)
        btnCancel.setTextColor(errorColor)
        btnClear.setTextColor(errorColor)

        // Make buttons consistent (Text style for both if we want colorError text)
        btnClear.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        btnClear.rippleColor = android.content.res.ColorStateList.valueOf(errorColor).withAlpha(30)

        btnClear.setOnClickListener {
            viewModel.clearRecent()
            alertDialog.dismiss()
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun observeViewModel() {
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
            }
            adapter?.submitList(songs)
        }
    }
}