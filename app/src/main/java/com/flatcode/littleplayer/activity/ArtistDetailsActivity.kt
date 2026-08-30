package com.flatcode.littleplayer.activity

import android.content.Context
import androidx.activity.viewModels
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityArtistDetailsBinding
import com.flatcode.littleplayer.utils.DATA
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
class ArtistDetailsActivity :
    BaseActivity<ActivityArtistDetailsBinding>(ActivityArtistDetailsBinding::inflate) {

    private val context: Context = this@ArtistDetailsActivity
    private val viewModel: ArtistDetailsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun setupViews() {
        applyEdgeToEdge(topView = binding.customToolbar.root)

        val artistName = intent.extras?.getString(DATA.ARTIST_NAME_KEY)
        initUI(artistName)
        viewModel.filterSongsByArtist(artistName)
    }

    private fun initUI(artistName: String?) {
        initToolbar(artistName ?: getString(R.string.artist))
    }

    override fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songList ->
            if (songList.isNotEmpty()) {
                if (adapter == null) {
                    adapter = MusicAdapter(context, onItemClick = { _, position, view ->
                        musicViewModel.updatePlaylistAndPlay(
                            adapter?.currentList ?: emptyList(), position, fromUserClick = true
                        )
                        openPlayer(position, view)
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
            if (event is MusicEvent.PlaySong && !event.fromUserClick) {
                openPlayer(event.position)
            }
        }
    }
}