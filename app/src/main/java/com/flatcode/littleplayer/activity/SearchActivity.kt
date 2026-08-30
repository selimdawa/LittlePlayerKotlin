package com.flatcode.littleplayer.activity

import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivitySearchBinding
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.utils.showKeyboard
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class SearchActivity : BaseActivity<ActivitySearchBinding>(ActivitySearchBinding::inflate) {

    private val viewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun setupViews() {
        applyEdgeToEdge(topView = binding.customToolbar.root)

        binding.customToolbar.backBtn.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val searchEditText = binding.customToolbar.searchEditText
        searchEditText.requestFocus()
        searchEditText.postDelayed({
            searchEditText.showKeyboard()
        }, 300)

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filterSongs(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun observeViewModel() {
        viewModel.filteredMusicFiles.collectWithLifecycle(this) { songs ->
            binding.emptyState.isVisible = songs.isEmpty()
            if (adapter == null) {
                adapter = MusicAdapter(this, onItemClick = { _, position, view ->
                    viewModel.updatePlaylistAndPlay(
                        adapter?.currentList ?: emptyList(), position, fromUserClick = true
                    )
                    openPlayer(position, view)
                }, onDeleteClick = { song ->
                    viewModel.deleteSong(song)
                }).apply {
                    bindToPlaybackSync(this@SearchActivity, nowPlayerViewModel, binding.root)
                }
                binding.recyclerView.adapter = adapter
            }
            adapter?.submitList(songs)
        }
    }
}