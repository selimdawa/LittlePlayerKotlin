package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.ArtistDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityAlbumDetailsBinding
import com.flatcode.littleplayer.utils.VOID
import com.flatcode.littleplayer.viewmodel.AlbumDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

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
                    VOID.coilAlbumImage(state.imagePath, binding.image)
                } else if (!state.firstSongPath.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        val bitmap = withContext(Dispatchers.IO) {
                            VOID.loadRawAlbumArt(state.firstSongPath)
                        }
                        if (bitmap != null) {
                            binding.image.load(bitmap) {
                                scale(coil.size.Scale.FILL)
                                crossfade(true)
                            }
                        } else {
                            binding.image.load(R.drawable.logo)
                        }
                    }
                }

                if (!state.firstSongId.isNullOrEmpty()) {
                    VOID.coilImageBlur(context, state.firstSongId, state.firstSongPath, binding.imageBlur, 50)
                }

                adapter = ArtistDetailsAdapter(context, ArrayList(state.songs))
                binding.recyclerView.adapter = adapter
            }
        }
    }
}