package com.flatcode.littleplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemFolderBinding

class PlaylistAdapter(
    private val playlistNames: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val name = playlistNames[position]
        holder.binding.folderName.text = name
        holder.binding.folderDetails.text = "Playlist"

        holder.itemView.setOnClickListener {
            onItemClick(name)
        }
    }

    override fun getItemCount(): Int = playlistNames.size
}