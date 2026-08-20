package com.flatcode.littleplayer.activity

import android.content.Context
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityAlbumDetailsBinding
import com.flatcode.littleplayer.utils.DATA
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
import io.selimdawa.multicolors.MultiColorManager
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class AlbumDetailsActivity : BaseActivity<ActivityAlbumDetailsBinding>(ActivityAlbumDetailsBinding::inflate) {

    private val context: Context = this@AlbumDetailsActivity
    private val viewModel: AlbumDetailsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null
    private var albumName: String? = null

    override fun setupViews() {
        applyEdgeToEdge(topView = binding.customToolbar.root)

        albumName = intent.extras?.getString(DATA.ALBUM_NAME_KEY)
        initUI(albumName)
        viewModel.filterSongsByAlbum(albumName)
    }

    private fun initUI(albumName: String?) {
        initToolbar(albumName ?: getString(R.string.album))
    }

    override fun observeViewModel() {
        viewModel.uiState.collectWithLifecycle(this) { state ->
            if (state.songs.isNotEmpty()) {
                if (!state.imagePath.isNullOrEmpty()) {
                    binding.image.loadCachedAlbumImage(state.imagePath)
                } else {
                    binding.image.loadSongImage(
                        state.firstSongAlbumId, state.firstSongPath, album = albumName, isAlbum = true
                    )
                }

                if (!state.firstSongAlbumId.isNullOrEmpty() || !state.imagePath.isNullOrEmpty()) {
                    binding.imageBlur.loadSongImageBlur(
                        state.firstSongAlbumId,
                        50,
                        state.firstSongPath,
                        state.imagePath,
                        albumName,
                        isAlbum = true
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
                    binding.recyclerView.itemAnimator = null
                }
                adapter?.submitList(state.songs)
            }
        }

        musicViewModel.event.collectWithLifecycle(this) { event ->
            if (event is MusicEvent.PlaySong) {
                openPlayer(event.position)
            }
        }

        lifecycleScope.launch {
            MultiColorManager.currentThemeId.collect { _ ->
                MultiColorManager.applyTheme(this@AlbumDetailsActivity)
            }
        }
    }
}
