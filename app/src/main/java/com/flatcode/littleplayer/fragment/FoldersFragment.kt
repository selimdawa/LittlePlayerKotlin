package com.flatcode.littleplayer.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.activity.FolderDetailsActivity
import com.flatcode.littleplayer.adapter.FolderAdapter
import com.flatcode.littleplayer.databinding.FragmentFoldersBinding
import com.flatcode.littleplayer.model.Folder
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.visible
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MusicViewModel by activityViewModels()
    private var adapter: FolderAdapter? = null
    private var lastSortOrder: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.btnFilterSort.visible()

        binding.toolbar.btnFilterSort.setOnClickListener {
            val bottomSheet = SortSongsBottomSheet(
                DATA.FOLDERS, viewModel.foldersSortOrder.value
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
                    val currentSortOrder = viewModel.foldersSortOrder.value
                    val shouldScrollToTop = (lastSortOrder != null && lastSortOrder != currentSortOrder)
                    lastSortOrder = currentSortOrder

                    binding.emptyState.isVisible = folderList.isEmpty()
                    if (folderList.isNotEmpty()) {
                        if (adapter == null) {
                            setupAdapter()
                        }
                        adapter?.submitList(folderList) {
                            if (shouldScrollToTop) {
                                binding.recyclerView.scrollToPosition(0)
                            }
                        }
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
            adapter = FolderAdapter(requireContext(), { folderName, folderPath, _ ->
                val intent = Intent(
                    requireContext(),
                    FolderDetailsActivity::class.java
                ).apply {
                    putExtra("FOLDER_NAME", folderName)
                    putExtra("FOLDER_PATH", folderPath)
                }
                startActivity(intent)
            }, { folder, view ->
                showFolderMenu(folder, view)
            })
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        binding.recyclerView.adapter = adapter
    }

    private fun showFolderMenu(folder: Folder, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 0, 0, R.string.hide_folder)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> {
                    showHideFolderDialog(folder)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showHideFolderDialog(folder: Folder) {
        val view = layoutInflater.inflate(R.layout.dialog_hide_folder, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        view.findViewById<TextView>(R.id.dialogTitle).text = folder.name

        view.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnHide).setOnClickListener {
            viewModel.addExcludedFolder(folder.path)
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }
}