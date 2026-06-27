package com.flatcode.littleplayer.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.flatcode.littleplayer.Activity.MainActivity
import com.flatcode.littleplayer.Adapter.MusicAdapter
import com.flatcode.littleplayer.databinding.FragmentSongsBinding

class SongsFragment : Fragment() {

    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)

        binding.recyclerView.setHasFixedSize(true)

        val files = MainActivity.musicFiles
        if (files != null && files.isNotEmpty()) {
            musicAdapter = MusicAdapter(requireContext(), files)
            binding.recyclerView.adapter = musicAdapter
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        var musicAdapter: MusicAdapter? = null
    }
}