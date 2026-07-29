package com.flatcode.littleplayer.activity

import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.PlaylistAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistsBinding
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistsViewModel
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        val toolbar = findViewById<MaterialToolbar>(R.id.customToolbar)
        toolbar.title = getString(R.string.playlists)
        toolbar.setNavigationOnClickListener { finish() }
        binding.addPlaylist.setOnClickListener { showCreatePlaylistDialog() }
    }

    private fun showCreatePlaylistDialog() {
        val container = FrameLayout(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 24, 48, 24)
        val editText = EditText(this)
        editText.layoutParams = params
        editText.hint = "Playlist Name"
        container.addView(editText)

        AlertDialog.Builder(this).setTitle("New Playlist").setView(container)
            .setPositiveButton("Create") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotEmpty()) {
                    viewModel.createPlaylist(name)
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlistNames.collect { names ->
                        binding.recyclerView.adapter = PlaylistAdapter(names) { playlistName ->
                            launchActivity<PlaylistDetailsActivity> {
                                putExtra("PLAYLIST_NAME", playlistName)
                            }
                        }
                    }
                }

                launch {
                    nowPlayerViewModel.currentPlayingSong.collect { song ->
                        binding.fragBottomPlayer.isVisible = song != null
                    }
                }
            }
        }
    }
}