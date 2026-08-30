package com.flatcode.littleplayer.activity

import android.content.Context
import androidx.activity.viewModels
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivityFolderDetailsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.FolderDetailsViewModel
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class FolderDetailsActivity :
    BaseActivity<ActivityFolderDetailsBinding>(ActivityFolderDetailsBinding::inflate) {

    private val context: Context = this@FolderDetailsActivity
    private val viewModel: FolderDetailsViewModel by viewModels()
    private val musicViewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun setupViews() {
        applyEdgeToEdge(topView = binding.customToolbar.root)

        val folderName = intent.extras?.getString(DATA.FOLDER_NAME)
        val folderPath = intent.extras?.getString(DATA.FOLDER_PATH)
        initUI(folderName)
        viewModel.filterSongsByFolder(folderPath)
    }

    private fun initUI(folderName: String?) {
        initToolbar(folderName ?: getString(R.string.folders))
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
                            this@FolderDetailsActivity, nowPlayerViewModel, binding.root
                        )
                    }
                    binding.recyclerView.adapter = adapter
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
