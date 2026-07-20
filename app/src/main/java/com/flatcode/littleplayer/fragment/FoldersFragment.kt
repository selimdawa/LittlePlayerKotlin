package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.adapter.FolderAdapter
import com.flatcode.littleplayer.databinding.FragmentFoldersBinding
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private var adapter: FolderAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.btnFilterSort.visibility = View.GONE

        viewModel.folderFiles.observe(viewLifecycleOwner) { folderList ->
            if (!folderList.isNullOrEmpty()) {
                val arrayListFolders = ArrayList(folderList)

                adapter = FolderAdapter(requireContext(), arrayListFolders) { folderName, folderPath ->
                    val bundle = Bundle().apply {
                        putString("FOLDER_NAME", folderName)
                        putString("FOLDER_PATH", folderPath)
                    }
                    findNavController().navigate(R.id.folderDetailsActivity, bundle)
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