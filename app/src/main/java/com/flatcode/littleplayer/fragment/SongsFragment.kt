package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.FragmentSongsBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.observePlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.utils.snackbar
import com.flatcode.littleplayer.utils.visible
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

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
        binding.toolbar.btnFilterSort.visible()

        binding.toolbar.btnFilterSort.setOnClickListener {
            val bottomSheet = SortSongsBottomSheet(viewModel.sortOrder.value) { sortType ->
                viewModel.updateSortOrder(sortType)
            }
            bottomSheet.show(childFragmentManager, "SortSongsBottomSheet")
        }

        setupAdapter()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.filteredMusicFiles.collectWithLifecycle(viewLifecycleOwner) { files ->
            val currentSortOrder = viewModel.sortOrder.value
            val shouldScrollToTop = (lastSortOrder != null && lastSortOrder != currentSortOrder)
            lastSortOrder = currentSortOrder

            musicAdapter?.submitList(files) {
                if (shouldScrollToTop) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }

        observePlaybackSync(nowPlayerViewModel, binding.root) { musicAdapter }

        viewModel.event.collectWithLifecycle(viewLifecycleOwner) { event ->
            when (event) {
                is MusicEvent.SongDeleted -> {
                    binding.root.snackbar("File Deleted: ${event.song.title}")
                }

                is MusicEvent.Error -> {
                    binding.root.snackbar(event.message)
                }
            }
        }
    }

    private fun setupAdapter() {
        if (musicAdapter == null) {
            musicAdapter = MusicAdapter(
                requireContext(), onItemClick = { _, position ->
                    val currentFiles = musicAdapter?.currentList ?: return@MusicAdapter
                    viewModel.updateCurrentPlaylist(ArrayList(currentFiles))
                    requireContext().openPlayer(position)
                }) { song ->
                viewModel.deleteSong(song)
            }
        }
        binding.recyclerView.adapter = musicAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}