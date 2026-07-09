package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.ArtistAdapter
import com.flatcode.littleplayer.databinding.FragmentArtistsBinding
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.util.ArrayList

@AndroidEntryPoint
class ArtistsFragment : Fragment() {

    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private var adapter: ArtistAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.artistFiles.observe(viewLifecycleOwner) { artistList ->
            if (!artistList.isNullOrEmpty()) {
                val arrayListArtists = ArrayList(artistList)

                adapter = ArtistAdapter(requireContext(), arrayListArtists) { artistName ->
                    val bundle = Bundle().apply {
                        putString("ARTIST_NAME", artistName)
                    }
                    findNavController().navigate(R.id.artistDetailsActivity, bundle)
                }

                binding.recyclerView.adapter = adapter

                FastScrollerBuilder(binding.recyclerView)
                    .setPopupTextProvider(adapter as me.zhanghai.android.fastscroll.PopupTextProvider)
                    .build()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}