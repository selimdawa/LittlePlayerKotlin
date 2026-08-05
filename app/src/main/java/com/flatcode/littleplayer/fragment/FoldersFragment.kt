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
import com.flatcode.littleplayer.adapter.FolderAdapter
import com.flatcode.littleplayer.databinding.FragmentFoldersBinding
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()
    private var adapter: FolderAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.btnFilterSort.visibility = View.VISIBLE
        binding.toolbar.btnFilterSort.setOnClickListener {
            val bottomSheet = SortSongsBottomSheet(
                DATA.FOLDERS, DATA.SORT_BY_NAME
            ) { category, sortType ->
                viewModel.updateSortOrder(category, sortType)
            }
            bottomSheet.show(childFragmentManager, "SortSongsBottomSheet")
        }

        binding.toolbar.btnShuffle.setOnClickListener {
            viewModel.smartShuffle(DATA.FOLDERS)
        }

        setupAdapter()

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.folderFiles.collect { folderList ->
                    binding.emptyState.isVisible = folderList.isEmpty()
                    if (folderList.isNotEmpty()) {
                        val arrayListFolders = ArrayList(folderList)
                        if (adapter == null) {
                            setupAdapter()
                        }
                        adapter?.updateList(arrayListFolders)
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
            adapter = FolderAdapter(requireContext(), ArrayList()) { folderName, folderPath, _ ->
                val intent = Intent(requireContext(), com.flatcode.littleplayer.activity.FolderDetailsActivity::class.java).apply {
                    putExtra("FOLDER_NAME", folderName)
                    putExtra("FOLDER_PATH", folderPath)
                }
                startActivity(intent)
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