package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.flatcode.littleplayer.adapter.FolderDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityFolderDetailsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.utils.observePlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.FolderDetailsViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FolderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderDetailsBinding
    private val context: Context = this@FolderDetailsActivity
    private val viewModel: FolderDetailsViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: FolderDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        observeViewModel()

        val folderPath = intent.extras?.getString("FOLDER_PATH")
        viewModel.filterSongsByFolder(folderPath)
    }

    private fun observeViewModel() {
        viewModel.songs.collectWithLifecycle(this) { songList ->
            if (songList.isNotEmpty()) {
                if (adapter == null) {
                    adapter = FolderDetailsAdapter(context) { _, position ->
                        viewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                        openPlayer(position)
                    }
                    binding.recyclerView.adapter = adapter
                }
                adapter?.submitList(songList)
            }
        }

        observePlaybackSync(nowPlayerViewModel) { adapter }
    }
}