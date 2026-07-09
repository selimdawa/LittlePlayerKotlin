package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flatcode.littleplayer.adapter.ArtistDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityArtistDetailsBinding
import com.flatcode.littleplayer.viewmodel.ArtistDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
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
        viewModel.songs.observe(this) { songList ->
            if (!songList.isNullOrEmpty()) {
                adapter = ArtistDetailsAdapter(context, ArrayList(songList))
                binding.recyclerView.adapter = adapter
            }
        }
    }
}