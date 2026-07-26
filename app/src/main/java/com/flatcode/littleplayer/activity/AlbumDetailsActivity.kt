package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.flatcode.littleplayer.adapter.AlbumDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityAlbumDetailsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.loadCachedAlbumImage
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.viewmodel.AlbumDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList

@AndroidEntryPoint
class AlbumDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailsBinding
    private val context: Context = this@AlbumDetailsActivity
    private val viewModel: AlbumDetailsViewModel by viewModels()
    private var adapter: AlbumDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()

        val albumName = intent.extras?.getString("ALBUM_NAME")
        viewModel.filterSongsByAlbum(albumName)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.songs.isNotEmpty()) {
                        if (!state.imagePath.isNullOrEmpty()) {
                            binding.image.loadCachedAlbumImage(state.imagePath)
                        } else {
                            binding.image.loadSongImage(state.firstSongAlbumId)
                        }

                        if (!state.firstSongAlbumId.isNullOrEmpty()) {
                            binding.imageBlur.loadSongImageBlur(state.firstSongAlbumId, 50)
                        }

                        val arrayListSongs = ArrayList(state.songs)
                        adapter = AlbumDetailsAdapter(context, arrayListSongs) { position ->
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
