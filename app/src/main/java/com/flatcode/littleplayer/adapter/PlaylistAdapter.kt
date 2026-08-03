package com.flatcode.littleplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemPlaylistBinding

class PlaylistAdapter(
    private val playlistNames: List<String>, private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val name = playlistNames[position]
        holder.binding.playlistName.text = name
        // details are static for now, or could count songs if we pass a map
        holder.binding.playlistDetails.text = "Playlist"

        holder.binding.root.setOnClickListener {
            onItemClick(name)
        }
    }

    override fun getItemCount(): Int = playlistNames.size
}