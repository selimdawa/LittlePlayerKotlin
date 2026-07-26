package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.activity.PlayerActivity
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.FragmentSongsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by activityViewModels()
    private var musicAdapter: MusicAdapter? = null
    private var lastSortOrder: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.btnFilterSort.visibility = View.VISIBLE

        binding.toolbar.btnFilterSort.setOnClickListener {
            val bottomSheet = SortSongsBottomSheet(viewModel.sortOrder.value) { sortType ->
                viewModel.updateSortOrder(sortType)
            }
            bottomSheet.show(childFragmentManager, "SortSongsBottomSheet")
        }

        setupAdapter()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredMusicFiles.collect { files ->
                        val arrayListFiles = ArrayList(files)
                        if (musicAdapter == null) {
                            setupAdapter()
                        }
                        
                        val currentSortOrder = viewModel.sortOrder.value
                        val shouldScrollToTop = lastSortOrder != null && lastSortOrder != currentSortOrder
                        lastSortOrder = currentSortOrder

                        musicAdapter?.updateList(arrayListFiles) {
                            if (shouldScrollToTop) {
                                binding.recyclerView.scrollToPosition(0)
                            }
                        }
                    }
                }
                launch {
                    nowPlayerViewModel.currentPlayingSong.collect {
                        updateAdapterState()
                    }
                }
                launch {
                    nowPlayerViewModel.isPlaying.collect {
                        updateAdapterState()
                    }
                }
            }
        }
    }

    private fun setupAdapter() {
        if (musicAdapter == null) {
            musicAdapter = MusicAdapter(requireContext(), ArrayList()) { position ->
                val currentFiles = musicAdapter?.getMusicFiles() ?: return@MusicAdapter
                viewModel.updateCurrentPlaylist(ArrayList(currentFiles))
                requireContext().launchActivity<PlayerActivity> {
                    putExtra(DATA.POSITION, position)
                }
            }
            musicAdapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.recyclerView.adapter = musicAdapter
        updateAdapterState()
    }

    private fun updateAdapterState() {
        val song = nowPlayerViewModel.currentPlayingSong.value
        val isPlaying = nowPlayerViewModel.isPlaying.value ?: false
        musicAdapter?.updatePlaybackState(song?.path, isPlaying)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}