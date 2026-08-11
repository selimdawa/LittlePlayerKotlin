package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.media3.common.util.UnstableApi
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.FragmentSongsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.bindToPlaybackSync
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.utils.snackbar
import com.flatcode.littleplayer.utils.visible
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
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
            val bottomSheet = SortSongsBottomSheet(
                DATA.SONGS, viewModel.songsSortOrder.value
            ) { category, sortType ->
                viewModel.updateSortOrder(category, sortType)
            }
            bottomSheet.show(childFragmentManager, "SortSongsBottomSheet")
        }

        binding.toolbar.btnShuffle.setOnClickListener {
            viewModel.smartShuffle(DATA.SONGS)
        }

        setupAdapter()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.filteredMusicFiles.collectWithLifecycle(viewLifecycleOwner) { files ->
            val currentSortOrder = viewModel.songsSortOrder.value
            val shouldScrollToTop = (lastSortOrder != null && lastSortOrder != currentSortOrder)
            lastSortOrder = currentSortOrder

            binding.emptyState.isVisible = files.isEmpty()
            musicAdapter?.submitList(files) {
                if (shouldScrollToTop) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        }


        viewModel.event.collectWithLifecycle(viewLifecycleOwner) { event ->
            when (event) {
                is MusicEvent.SongDeleted -> {
                    binding.root.snackbar("File Deleted: ${event.song.title}")
                }
                is MusicEvent.Error -> {
                    binding.root.snackbar(event.message)
                }
                else -> {}
            }
        }
    }

    private fun setupAdapter() {
        if (musicAdapter == null) {
            musicAdapter = MusicAdapter(
                requireContext(),
                onItemClick = { _, position, _ ->
                    val currentFiles = musicAdapter?.currentList ?: return@MusicAdapter
                    viewModel.updatePlaylistAndPlay(ArrayList(currentFiles), position)
                },
                onDeleteClick = { song ->
                    viewModel.deleteSong(song)
                }
            ).apply {
                bindToPlaybackSync(viewLifecycleOwner, nowPlayerViewModel, binding.root)
            }
        }
        binding.recyclerView.apply {
            adapter = musicAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}