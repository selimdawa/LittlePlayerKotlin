package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemFolderBinding
import com.flatcode.littleplayer.model.Folder

class FolderAdapter(
    private val context: Context,
    private var folderList: ArrayList<Folder>,
    private val onItemClick: (String, String) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    class FolderViewHolder(val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folderList[position]
        holder.binding.folderName.text = folder.name
        holder.binding.folderDetails.text = "${folder.songsCount} songs | ${folder.path}"

        holder.itemView.setOnClickListener {
            onItemClick(folder.name, folder.path)
        }
    }

    override fun getItemCount(): Int = folderList.size

    fun updateList(newList: ArrayList<Folder>) {
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
    }
}