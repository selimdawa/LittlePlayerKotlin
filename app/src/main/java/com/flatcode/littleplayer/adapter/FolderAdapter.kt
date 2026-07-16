package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemFolderBinding
import com.flatcode.littleplayer.model.Folder

class FolderAdapter(
    private val context: Context,
    private val folderList: ArrayList<Folder>,
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
}
