package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityAlbumDetailsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.loadCachedAlbumImage
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.viewmodel.AlbumDetailsViewModel
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class AlbumDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailsBinding
    private val context: Context = this@AlbumDetailsActivity
    private val viewModel: AlbumDetailsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()

        val albumName = intent.extras?.getString("ALBUM_NAME")
        initUI(albumName)
        viewModel.filterSongsByAlbum(albumName)
    }

    private fun initUI(albumName: String?) {
        initToolbar(albumName ?: getString(com.flatcode.littleplayer.R.string.album))
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
                    adapter = MusicAdapter(
                        context,
                        onItemClick = { _, position, _ ->
                            musicViewModel.updatePlaylistAndPlay(adapter?.currentList ?: emptyList(), position)
                        },
                        onDeleteClick = { song ->
                            musicViewModel.deleteSong(song)
                        }
                    ).apply {
                        bindToPlaybackSync(this@AlbumDetailsActivity, nowPlayerViewModel, binding.root)
                    }
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(state.songs)
            }
        }

        musicViewModel.event.collectWithLifecycle(this) { event ->
            if (event is MusicEvent.PlaySong) {
                openPlayer(event.position)
            }
        }
    }
}
