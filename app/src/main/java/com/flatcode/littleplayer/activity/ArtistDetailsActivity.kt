package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityArtistDetailsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.ArtistDetailsViewModel
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class ArtistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtistDetailsBinding
    private val context: Context = this@ArtistDetailsActivity
    private val viewModel: ArtistDetailsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()

        val artistName = intent.extras?.getString("ARTIST_NAME")
        viewModel.filterSongsByArtist(artistName)
    }

    private fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songList ->
            if (songList.isNotEmpty()) {
                if (adapter == null) {
                    adapter = MusicAdapter(
                        context,
                        onItemClick = { _, position, view ->
                            viewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                            openPlayer(position, view)
                        },
                        onDeleteClick = { song ->
                            musicViewModel.deleteSong(song)
                        }
                    ).apply {
                        bindToPlaybackSync(this@ArtistDetailsActivity, nowPlayerViewModel, binding.root)
                    }
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(songList)
            }
        }
    }
}
