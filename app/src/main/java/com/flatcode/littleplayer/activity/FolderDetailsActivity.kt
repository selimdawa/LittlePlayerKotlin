package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.flatcode.littleplayer.adapter.FolderDetailsAdapter
import com.flatcode.littleplayer.databinding.ActivityFolderDetailsBinding
import com.flatcode.littleplayer.viewmodel.FolderDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList

@AndroidEntryPoint
class FolderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFolderDetailsBinding
    private val context: Context = this@FolderDetailsActivity
    private val viewModel: FolderDetailsViewModel by viewModels()
    private var adapter: FolderDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFolderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()

        val folderPath = intent.extras?.getString("FOLDER_PATH")
        viewModel.filterSongsByFolder(folderPath)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.songs.collect { songList ->
                    if (songList.isNotEmpty()) {
                        adapter = FolderDetailsAdapter(context, ArrayList(songList))
                        binding.recyclerView.adapter = adapter
                    }
                }
            }
        }
    }
}