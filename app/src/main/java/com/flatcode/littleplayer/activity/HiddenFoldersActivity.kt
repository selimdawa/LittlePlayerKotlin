package com.flatcode.littleplayer.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityHiddenFoldersBinding
import com.flatcode.littleplayer.databinding.ItemHiddenFolderBinding
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.initToolbar
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class HiddenFoldersActivity :
    BaseActivity<ActivityHiddenFoldersBinding>(ActivityHiddenFoldersBinding::inflate) {

    private val viewModel: MusicViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    override fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            binding.customToolbar.root.updatePadding(top = systemBars.top)
            insets
        }

        initToolbar(getString(R.string.hidden_folders))
        val adapter = HiddenFolderAdapter { path ->
            viewModel.removeExcludedFolder(path)
        }
        binding.recyclerView.adapter = adapter
    }

    override fun observeViewModel() {
        viewModel.excludedFolders.collectWithLifecycle(this) { folders ->
            val list = folders.toList().sorted()
            (binding.recyclerView.adapter as? HiddenFolderAdapter)?.submitList(list)
            binding.emptyState.isVisible = list.isEmpty()
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }
    }

    private class HiddenFolderAdapter(private val onUnhideClick: (String) -> Unit) :
        ListAdapter<String, HiddenFolderAdapter.ViewHolder>(DiffCallback()) {

        class ViewHolder(val binding: ItemHiddenFolderBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHiddenFolderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val path = getItem(position)
            val folderName = path.trimEnd('/').substringAfterLast('/')

            holder.binding.folderName.text = folderName
            holder.binding.folderPath.text = path
            holder.binding.btnUnhide.setOnClickListener { onUnhideClick(path) }
        }

        private class DiffCallback : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
    }
}