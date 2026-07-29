package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistDetailsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistDetailsViewModel
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaylistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailsBinding
    private val viewModel: PlaylistDetailsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playlistName = intent.getStringExtra("PLAYLIST_NAME") ?: "Playlist"
        val toolbar = findViewById<MaterialToolbar>(R.id.customToolbar)
        toolbar.title = playlistName
        toolbar.setNavigationOnClickListener { finish() }

        viewModel.loadSongs(playlistName)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songs ->
            if (songs.isNotEmpty()) {
                if (adapter == null) {
                    adapter = MusicAdapter(
                        this, onItemClick = { _, position ->
                            musicViewModel.updateCurrentPlaylist(
                                adapter?.currentList ?: emptyList()
                            )
                            launchActivity<PlayerActivity> {
                                putExtra(DATA.POSITION, position)
                            }
                        }) { song ->
                        musicViewModel.deleteSong(song)
                    }
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(songs)
            }
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.isVisible = song != null
        }
    }
}