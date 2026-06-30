package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.AlbumAdapter
import com.flatcode.littleplayer.databinding.FragmentAlbumsBinding
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList

@AndroidEntryPoint
class AlbumsFragment : Fragment() {

    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private var adapter: AlbumAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.albumFiles.observe(viewLifecycleOwner) { albumList ->
            if (!albumList.isNullOrEmpty()) {
                val arrayListAlbums = ArrayList(albumList)

                adapter = AlbumAdapter(requireContext(), arrayListAlbums) { albumName: String ->
                    val bundle = Bundle().apply {
                        putString("ALBUM_NAME", albumName)
                    }
                    findNavController().navigate(R.id.albumDetailsActivity, bundle)
                }

                binding.recyclerView.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}