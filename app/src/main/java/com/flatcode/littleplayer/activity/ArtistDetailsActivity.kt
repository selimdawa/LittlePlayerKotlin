package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.flatcode.littleplayer.adapter.ArtistDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityArtistDetailsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.ArtistDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList

@AndroidEntryPoint
class ArtistDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtistDetailsBinding
    private val context: Context = this@ArtistDetailsActivity
    private val viewModel: ArtistDetailsViewModel by viewModels()
    private var adapter: ArtistDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtistDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()

        val artistName = intent.extras?.getString("ARTIST_NAME")
        viewModel.filterSongsByArtist(artistName)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.songs.collect { songList ->
                    if (songList.isNotEmpty()) {
                        val arrayListSongs = ArrayList(songList)
                        adapter = ArtistDetailsAdapter(context, arrayListSongs) { position ->
                            viewModel.updateCurrentPlaylist(arrayListSongs)
                            launchActivity<PlayerActivity> {
                                putExtra(DATA.POSITION, position)
                            }
                        }
                        binding.recyclerView.adapter = adapter
                    }
                }
            }
        }
    }
}
