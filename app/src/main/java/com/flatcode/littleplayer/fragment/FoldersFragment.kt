package com.flatcode.littleplayer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

    private var adapter: FolderAdapter? = null
    private val viewModel: MusicViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.folderFiles.observe(viewLifecycleOwner) { folderList ->
            if (!folderList.isNullOrEmpty()) {
                val arrayListFolders = ArrayList(folderList)

                adapter = FolderAdapter(requireContext(), arrayListFolders) { folderName, folderPath ->
                    val bundle = bundleOf(
                        "FOLDER_NAME" to folderName,
                        "FOLDER_PATH" to folderPath
                    )
                    findNavController().navigate(R.id.musicFolderActivity, bundle)
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