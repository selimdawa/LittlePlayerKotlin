package com.flatcode.littleplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemPlaylistBinding
import com.flatcode.littleplayer.model.Playlist
import com.flatcode.littleplayer.utils.loadSongImageBlur
import com.flatcode.littleplayer.utils.loadSongImageByPath

class PlaylistAdapter(
    private val onItemClick: (String) -> Unit,
    private val onMoreClick: (Playlist, Int) -> Unit,
) : ListAdapter<Playlist, PlaylistAdapter.PlaylistViewHolder>(PlaylistDiffCallback()) {

    class PlaylistViewHolder(val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding =
            ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = getItem(position)
        val context = holder.itemView.context
        holder.binding.playlistName.text = playlist.name

        val songCount = playlist.songCount
        val songsText =
            context.resources.getQuantityString(R.plurals.songs_count, songCount, songCount)
        holder.binding.playlistDetails.text =
            context.getString(R.string.playlist_details_format, songsText)

        holder.binding.playlistImage.loadSongImageByPath(playlist.firstSongPath)
        holder.binding.playlistImageBlur.loadSongImageBlur(null, 100, playlist.firstSongPath)

        holder.binding.foregroundCard.setOnClickListener {
            onItemClick(playlist.name)
        }

        holder.binding.foregroundCard.setOnLongClickListener {
            onMoreClick(playlist, holder.bindingAdapterPosition)
            true
        }
    }

    private class PlaylistDiffCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }
}