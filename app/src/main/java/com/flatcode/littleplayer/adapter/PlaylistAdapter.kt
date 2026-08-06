package com.flatcode.littleplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemPlaylistBinding
import com.flatcode.littleplayer.model.Playlist
import com.flatcode.littleplayer.utils.loadSongImageByPath

class PlaylistAdapter(
    private val playlists: List<Playlist>, private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.binding.playlistName.text = playlist.name
        holder.binding.playlistDetails.text =
            "Playlist / ${playlist.songCount} ${if (playlist.songCount == 1) "Song" else "Songs"}"

        holder.binding.playlistImage.loadSongImageByPath(playlist.firstSongPath)

        holder.binding.foregroundCard.setOnClickListener {
            onItemClick(playlist.name)
        }
    }

    override fun getItemCount(): Int = playlists.size
}