package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flatcode.littleplayer.adapter.ArtistDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityAlbumDetailsBinding
import com.flatcode.littleplayer.utils.loadCachedAlbumImage
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.viewmodel.AlbumDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlbumDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailsBinding
    private val context: Context = this@AlbumDetailsActivity
    private val viewModel: AlbumDetailsViewModel by viewModels()
    private var adapter: ArtistDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()

        val albumName = intent.extras?.getString("ALBUM_NAME")
        viewModel.filterSongsByAlbum(albumName)
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            if (state.songs.isNotEmpty()) {
                if (!state.imagePath.isNullOrEmpty()) {
                    binding.image.loadCachedAlbumImage(state.imagePath)
                } else {
                    binding.image.loadSongImage(state.firstSongAlbumId)
                }

                if (!state.firstSongAlbumId.isNullOrEmpty()) {
                    binding.imageBlur.loadSongImageBlur(state.firstSongAlbumId, 50)
                }

                adapter = ArtistDetailsAdapter(context, ArrayList(state.songs))
                binding.recyclerView.adapter = adapter
            }
        }
    }
}