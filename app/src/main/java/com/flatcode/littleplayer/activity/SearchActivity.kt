package com.flatcode.littleplayer.activity

import android.view.LayoutInflater
import android.view.View
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivitySearchBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.showKeyboard
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private var adapter: MusicAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.customToolbar.root.setNavigationOnClickListener { finish() }

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

    private fun observeViewModel() {
        viewModel.filteredMusicFiles.collectWithLifecycle(this) { songs ->
            binding.emptyState.isVisible = songs.isEmpty()
            if (adapter == null) {
                adapter = MusicAdapter(
                    this, onItemClick = { _, position ->
                        viewModel.updateCurrentPlaylist(adapter?.currentList ?: emptyList())
                        launchActivity<PlayerActivity> {
                            putExtra(DATA.POSITION, position)
                        }
                    }) { song ->
                    viewModel.deleteSong(song)
                }
                binding.recyclerView.adapter = adapter
            }
            adapter?.submitList(songs)
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }
}