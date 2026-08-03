package com.flatcode.littleplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.app.ActivityOptionsCompat
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.AlbumAdapter
import com.flatcode.littleplayer.databinding.FragmentAlbumsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class AlbumsFragment : Fragment() {

    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()
    private var adapter: AlbumAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.btnFilterSort.visibility = View.VISIBLE
        binding.toolbar.btnFilterSort.setOnClickListener {
            val bottomSheet = SortSongsBottomSheet(
                DATA.ALBUMS, viewModel.albumsSortOrder.value
            ) { category, sortType ->
                viewModel.updateSortOrder(category, sortType)
            }
            bottomSheet.show(childFragmentManager, "SortSongsBottomSheet")
        }

        binding.toolbar.btnShuffle.setOnClickListener {
            viewModel.smartShuffle(DATA.ALBUMS)
        }

        setupAdapter()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.albumFiles.collect { albumList ->
                    binding.emptyState.isVisible = albumList.isEmpty()
                    if (albumList.isNotEmpty()) {
                        val arrayListAlbums = ArrayList(albumList)
                        if (adapter == null) {
                            setupAdapter()
                        }
                        adapter?.updateList(arrayListAlbums)
                    } else {
                        adapter?.updateList(ArrayList())
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { _ ->
                    // Handle fragment-specific events if any
                }
            }
        }
    }

    private fun setupAdapter() {
        if (adapter == null) {
            adapter = AlbumAdapter(requireContext(), ArrayList()) { albumName, view ->
                val intent = Intent(requireContext(), com.flatcode.littleplayer.activity.AlbumDetailsActivity::class.java).apply {
                    putExtra("ALBUM_NAME", albumName)
                }
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(), view, "album_image"
                )
                startActivity(intent, options.toBundle())
            }
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}