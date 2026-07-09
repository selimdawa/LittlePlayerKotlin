package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.FragmentSongsBinding
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private var musicAdapter: MusicAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.filteredMusicFiles.observe(viewLifecycleOwner) { files ->
            if (files != null) {
                val arrayListFiles = ArrayList(files)
                if (musicAdapter == null) {
                    musicAdapter = MusicAdapter(requireContext(), arrayListFiles)
                    binding.recyclerView.adapter = musicAdapter
                } else {
                    musicAdapter?.updateList(arrayListFiles)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        musicAdapter = null
        _binding = null
    }
}