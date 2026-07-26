package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityPlaylistDetailsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.PlaylistDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList

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
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.customToolbar)
        toolbar.title = playlistName
        toolbar.setNavigationOnClickListener { finish() }

        viewModel.loadSongs(playlistName)
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.songs.collect { songs ->
                        if (songs.isNotEmpty()) {
                            val arrayListSongs = ArrayList(songs)
                            if (adapter == null) {
                                adapter = MusicAdapter(this@PlaylistDetailsActivity, arrayListSongs) { position ->
                                    musicViewModel.updateCurrentPlaylist(arrayListSongs)
                                    launchActivity<PlayerActivity> {
                                        putExtra(DATA.POSITION, position)
                                    }
                                }
                                binding.recyclerView.adapter = adapter
                            } else {
                                adapter?.updateList(arrayListSongs)
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