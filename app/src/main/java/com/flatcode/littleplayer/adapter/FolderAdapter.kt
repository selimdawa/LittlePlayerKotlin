package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemFolderBinding
import com.flatcode.littleplayer.model.Folder
import com.flatcode.littleplayer.utils.FastScrollableAdapter

class FolderAdapter(
    private val context: Context,
    private val onItemClick: (String, String, View) -> Unit,
    private val onMenuClick: (Folder, View) -> Unit
) : ListAdapter<Folder, FolderAdapter.FolderViewHolder>(FolderDiffCallback()),
    FastScrollableAdapter {

    class FolderViewHolder(val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = getItem(position)
        holder.binding.folderName.text = folder.name

        val context = holder.itemView.context
        val songCount = folder.songsCount
        val songsText =
            context.resources.getQuantityString(R.plurals.songs_count, songCount, songCount)
        holder.binding.folderDetails.text =
            context.getString(R.string.folder_details_format, songsText, folder.path)

        holder.itemView.setOnClickListener {
            onItemClick(folder.name, folder.path, holder.itemView)
        }

        holder.binding.btnMore.setOnClickListener {
            onMenuClick(folder, it)
        }
    }

    override fun getPopupText(position: Int): String {
        val name = getItem(position).name
        return if (name.isNotEmpty()) name.substring(0, 1).uppercase() else ""
    }

    private class FolderDiffCallback : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem == newItem
        }
    }
}