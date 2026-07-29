package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityRecentBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.RecentViewModel
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint

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

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        val toolbar = findViewById<MaterialToolbar>(R.id.customToolbar)
        toolbar.title = getString(R.string.recent)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.recentSongs.collectWithLifecycle(this) { songs ->
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