package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityFavoritesBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.observePlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.FavoritesViewModel
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private val viewModel: FavoritesViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.favourites))
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.favoriteSongs.collectWithLifecycle(this) { songs ->
            binding.emptyState.isVisible = songs.isEmpty()
            if (songs.isNotEmpty()) {
                if (adapter == null) {
                    adapter = MusicAdapter(this, onItemClick = { _, position, view ->
                        musicViewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                        openPlayer(position, view)
                    }, onDeleteClick = { song ->
                        musicViewModel.deleteSong(song)
                    })
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(songs)
            }
        }

        observePlaybackSync(nowPlayerViewModel, binding.root) { adapter }
    }
}