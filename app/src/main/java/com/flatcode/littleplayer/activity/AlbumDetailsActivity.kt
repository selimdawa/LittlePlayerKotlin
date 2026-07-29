package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flatcode.littleplayer.adapter.AlbumDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityAlbumDetailsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.loadCachedAlbumImage
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.observePlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.AlbumDetailsViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlbumDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailsBinding
    private val context: Context = this@AlbumDetailsActivity
    private val viewModel: AlbumDetailsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: AlbumDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        observeViewModel()

        val albumName = intent.extras?.getString("ALBUM_NAME")
        viewModel.filterSongsByAlbum(albumName)
    }

    private fun observeViewModel() {
        viewModel.uiState.collectWithLifecycle(this) { state ->
            if (state.songs.isNotEmpty()) {
                if (!state.imagePath.isNullOrEmpty()) {
                    binding.image.loadCachedAlbumImage(state.imagePath)
                } else {
                    binding.image.loadSongImage(state.firstSongAlbumId, state.firstSongPath)
                }

                if (!state.firstSongAlbumId.isNullOrEmpty() || !state.imagePath.isNullOrEmpty()) {
                    binding.imageBlur.loadSongImageBlur(
                        state.firstSongAlbumId, 50, state.firstSongPath, state.imagePath
                    )
                }

                if (adapter == null) {
                    adapter = AlbumDetailsAdapter(context) { _, position ->
                        viewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                        openPlayer(position)
                    }
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(state.songs)
            }
        }

        observePlaybackSync(nowPlayerViewModel, binding.root) { adapter }
    }
}