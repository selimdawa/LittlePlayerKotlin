package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemFolderBinding
import com.flatcode.littleplayer.model.Folder
import com.flatcode.littleplayer.utils.FastScrollableAdapter

class FolderAdapter(
    private val context: Context,
    private var folderList: ArrayList<Folder>,
    private val onItemClick: (String, String, View) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>(), FastScrollableAdapter {

    class FolderViewHolder(val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folderList[position]
        holder.binding.folderName.text = folder.name
        
        val context = holder.itemView.context
        val songCount = folder.songsCount
        val songsText = context.resources.getQuantityString(R.plurals.songs_count, songCount, songCount)
        holder.binding.folderDetails.text = context.getString(R.string.folder_details_format, songsText, folder.path)

        holder.itemView.setOnClickListener {
            onItemClick(folder.name, folder.path, holder.itemView)
        }
    }

    override fun getItemCount(): Int = folderList.size

    override fun getPopupText(position: Int): String {
        val name = folderList.getOrNull(position)?.name ?: ""
        return if (name.isNotEmpty()) name.substring(0, 1).uppercase() else ""
    }

    fun updateList(newList: ArrayList<Folder>, onComplete: (() -> Unit)? = null) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = folderList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return folderList[oldItemPosition].path == newList[newItemPosition].path
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return folderList[oldItemPosition] == newList[newItemPosition]
            }
        })
        folderList = newList
        diffResult.dispatchUpdatesTo(this)
        onComplete?.invoke()
    }
}