package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityFavoritesBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.loadSongImageByPath
import com.flatcode.littleplayer.viewmodel.FavoritesViewModel
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favoriteSongs.collect { songs ->
                        if (songs.isNotEmpty()) {
                            val arrayListSongs = ArrayList(songs)
                            if (adapter == null) {
                                adapter = MusicAdapter(this@FavoritesActivity, arrayListSongs) { position ->
                                    musicViewModel.updateCurrentPlaylist(arrayListSongs)
                                    launchActivity<PlayerActivity> {
                                        putExtra(DATA.POSITION, position)
                                    }
                                }
                                binding.recyclerView.adapter = adapter
                            } else {
                                adapter?.updateList(arrayListSongs)
                            }
                            updateAdapterState()
                        }
                    }
                }

                launch {
                    nowPlayerViewModel.currentPlayingSong.collect { song ->
                        binding.fragBottomPlayer.isVisible = song != null
                        updateAdapterState()
                    }
                }

                launch {
                    nowPlayerViewModel.isPlaying.collect {
                        updateAdapterState()
                    }
                }
            }
        }
    }

    private fun updateAdapterState() {
        val song = nowPlayerViewModel.currentPlayingSong.value
        val isPlaying = nowPlayerViewModel.isPlaying.value
        adapter?.updatePlaybackState(song?.path, isPlaying)
    }
}