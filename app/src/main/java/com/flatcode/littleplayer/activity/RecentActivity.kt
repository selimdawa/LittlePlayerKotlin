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
import com.flatcode.littleplayer.databinding.ActivityRecentBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.RecentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList

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
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.customToolbar)
        toolbar.title = getString(R.string.recent)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recentSongs.collect { songs ->
                        if (songs.isNotEmpty()) {
                            val arrayListSongs = ArrayList(songs)
                            if (adapter == null) {
                                adapter = MusicAdapter(this@RecentActivity, arrayListSongs) { position ->
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