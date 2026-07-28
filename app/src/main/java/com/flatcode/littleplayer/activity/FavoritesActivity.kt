package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityFavoritesBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.loadSongImageByPath
import com.flatcode.littleplayer.viewmodel.FavoritesViewModel
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

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

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.customToolbar)
        toolbar.title = getString(R.string.favourites)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.favoriteSongs.collectWithLifecycle(this) { songs ->
            if (songs.isNotEmpty()) {
                if (adapter == null) {
                    adapter = MusicAdapter(
                        this,
                        onItemClick = { _, position ->
                            musicViewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                            launchActivity<PlayerActivity> {
                                putExtra(DATA.POSITION, position)
                            }
                        },
                        onDeleteClick = { song ->
                            musicViewModel.deleteSong(song)
                        }
                    )
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(songs)
                updateAdapterState()
            }
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.isVisible = song != null
            updateAdapterState()
        }

        nowPlayerViewModel.isPlaying.collectWithLifecycle(this) {
            updateAdapterState()
        }
    }

    private fun updateAdapterState() {
        val song = nowPlayerViewModel.currentPlayingSong.value
        val isPlaying = nowPlayerViewModel.isPlaying.value
        adapter?.updatePlaybackState(song?.path, isPlaying)
    }
}