package com.flatcode.littleplayer.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.ActivitySearchBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.utils.showKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList

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
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.customToolbar)
        toolbar.setNavigationOnClickListener { finish() }
        
        val searchEditText = findViewById<android.widget.EditText>(R.id.searchEditText)
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredMusicFiles.collect { songs ->
                        val arrayListSongs = ArrayList(songs)
                        if (adapter == null) {
                            adapter = MusicAdapter(this@SearchActivity, arrayListSongs) { position ->
                                viewModel.updateCurrentPlaylist(arrayListSongs)
                                launchActivity<PlayerActivity> {
                                    putExtra(DATA.POSITION, position)
                                }
                            }
                            binding.recyclerView.adapter = adapter
                        } else {
                            adapter?.updateList(arrayListSongs)
                        }
                    }
                }

                launch {
                    nowPlayerViewModel.currentPlayingSong.collect { song ->
                        binding.fragBottomPlayer.isVisible = song != null
                    }
                }
            }
        }
    }
}