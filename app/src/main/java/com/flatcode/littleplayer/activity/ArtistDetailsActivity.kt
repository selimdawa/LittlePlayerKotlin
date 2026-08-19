package com.flatcode.littleplayer.activity

import android.content.Context
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityArtistDetailsBinding
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.ArtistDetailsViewModel
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class ArtistDetailsActivity : BaseActivity<ActivityArtistDetailsBinding>(ActivityArtistDetailsBinding::inflate) {

    private val context: Context = this@ArtistDetailsActivity
    private val viewModel: ArtistDetailsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            binding.customToolbar.root.updatePadding(top = systemBars.top)
            insets
        }

        val artistName = intent.extras?.getString("ARTIST_NAME")
        initUI(artistName)
        viewModel.filterSongsByArtist(artistName)
    }

    private fun initUI(artistName: String?) {
        initToolbar(artistName ?: getString(com.flatcode.littleplayer.R.string.artist))
    }

    override fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songList ->
            if (songList.isNotEmpty()) {
                if (adapter == null) {
                    adapter = MusicAdapter(context, onItemClick = { _, position, _ ->
                        musicViewModel.updatePlaylistAndPlay(
                            adapter?.currentList ?: emptyList(), position
                        )
                    }, onDeleteClick = { song ->
                        musicViewModel.deleteSong(song)
                    }).apply {
                        bindToPlaybackSync(
                            this@ArtistDetailsActivity, nowPlayerViewModel, binding.root
                        )
                    }
                    binding.recyclerView.adapter = adapter
                    binding.recyclerView.itemAnimator = null
                }
                adapter?.submitList(songList)
            }
        }

        musicViewModel.event.collectWithLifecycle(this) { event ->
            if (event is MusicEvent.PlaySong) {
                openPlayer(event.position)
            }
        }
    }
}