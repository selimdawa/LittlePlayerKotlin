package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.flatcode.littleplayer.adapter.AlbumAdapter
import com.flatcode.littleplayer.databinding.FragmentAlbumsBinding
import com.flatcode.littleplayer.databinding.FragmentArtistsBinding
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArtistsFragment : Fragment() {

    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!

    //private var adapter: ArtistAdapter? = null
    //private val viewModel: MusicViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //viewModel.artistFiles.observe(viewLifecycleOwner) { artistList ->
        //    if (!artistList.isNullOrEmpty()) {
        //        val arrayListArtists = ArrayList(.artistList)
        //        adapter = ArtistAdapter(requireContext(), arrayListArtist)
        //        binding.recyclerView.adapter = adapter
        //    }
        //}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}