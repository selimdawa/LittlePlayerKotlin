package com.flatcode.littleplayer.activity

import android.content.Context
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityFolderDetailsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.FolderDetailsViewModel
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class FolderDetailsActivity : BaseActivity<ActivityFolderDetailsBinding>(ActivityFolderDetailsBinding::inflate) {

    private val context: Context = this@FolderDetailsActivity
    private val viewModel: FolderDetailsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun setupViews() {
        applyEdgeToEdge(topView = binding.recyclerView)

        val folderPath = intent.extras?.getString(DATA.FOLDER_PATH)
        viewModel.filterSongsByFolder(folderPath)
    }

    override fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songList ->
            if (songList.isNotEmpty()) {
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
                        bindToPlaybackSync(this@FolderDetailsActivity, nowPlayerViewModel, binding.root)
                    }
                    binding.recyclerView.adapter = adapter
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
