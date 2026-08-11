package com.flatcode.littleplayer.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class HiddenFoldersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHiddenFoldersBinding
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHiddenFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar(getString(R.string.hidden_folders))

        val adapter = HiddenFolderAdapter { path ->
            viewModel.removeExcludedFolder(path)
        }
        binding.recyclerView.adapter = adapter

        viewModel.excludedFolders.collectWithLifecycle(this) { folders ->
            val list = folders.toList().sorted()
            adapter.submitList(list)
            binding.emptyState.isVisible = list.isEmpty()
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
            holder.binding.folderPath.text = path
            holder.binding.btnUnhide.setOnClickListener { onUnhideClick(path) }
        }

        private class DiffCallback : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
    }
}
