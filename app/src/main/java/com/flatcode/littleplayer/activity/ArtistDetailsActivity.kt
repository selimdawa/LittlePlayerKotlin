package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flatcode.littleplayer.adapter.ArtistDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityArtistDetailsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.observePlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.ArtistDetailsViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArtistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtistDetailsBinding
    private val context: Context = this@ArtistDetailsActivity
    private val viewModel: ArtistDetailsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: ArtistDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        observeViewModel()

        val artistName = intent.extras?.getString("ARTIST_NAME")
        viewModel.filterSongsByArtist(artistName)
    }

    private fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songList ->
            if (songList.isNotEmpty()) {
                if (adapter == null) {
                    adapter = ArtistDetailsAdapter(context) { _, position ->
                        viewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                        openPlayer(position)
                    }
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(songList)
            }
        }

        observePlaybackSync(nowPlayerViewModel, binding.root) { adapter }
    }
}