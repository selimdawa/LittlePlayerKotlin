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
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.adapter.ArtistAdapter
import com.flatcode.littleplayer.databinding.FragmentArtistsBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.FastScrollerHelper
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class ArtistsFragment : Fragment() {

    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()
    private var adapter: ArtistAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.btnFilterSort.visibility = View.GONE

        binding.toolbar.btnShuffle.setOnClickListener {
            viewModel.smartShuffle(DATA.ARTISTS)
        }

        setupAdapter()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.artistFiles.collect { artistList ->
                    binding.emptyState.isVisible = artistList.isEmpty()
                    if (artistList.isNotEmpty()) {
                        if (adapter == null) {
                            setupAdapter()
                        }
                        adapter?.submitList(artistList)
                    } else {
                        adapter?.submitList(emptyList())
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
            adapter = ArtistAdapter(requireContext()) { artistName, _ ->
                val intent = Intent(
                    requireContext(),
                    com.flatcode.littleplayer.activity.ArtistDetailsActivity::class.java
                ).apply {
                    putExtra("ARTIST_NAME", artistName)
                }
                startActivity(intent)
            }
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            FastScrollerHelper(
                binding.recyclerView, binding.fastScrollThumb, binding.fastScrollBubble
            )
        }
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}