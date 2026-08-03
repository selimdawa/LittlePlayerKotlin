package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistDetailsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.observePlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
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
        initToolbar(playlistName)

        viewModel.loadSongs(playlistName)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songs ->
            if (songs.isNotEmpty()) {
                if (adapter == null) {
                    adapter = MusicAdapter(this, onItemClick = { _, position ->
                        musicViewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                        openPlayer(position)
                    }) { song ->
                        musicViewModel.deleteSong(song)
                    }
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(songs)
            }
        }

        observePlaybackSync(nowPlayerViewModel, binding.root) { adapter }
    }
}