package com.flatcode.littleplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemPlaylistBinding
import com.flatcode.littleplayer.model.Playlist
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageBlur

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

        holder.binding.playlistImage.loadSongImage(null, playlist.firstSongPath)
        holder.binding.playlistImageBlur.loadSongImageBlur(null, 100, playlist.firstSongPath)

        holder.binding.foregroundCard.setOnClickListener {
            onItemClick(playlist.name)
        }

        holder.binding.foregroundCard.setOnLongClickListener {
            onMoreClick(playlist, holder.bindingAdapterPosition)
            true
        }
    }

    override fun onBindViewHolder(
        holder: PlaylistViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            if (payloads.contains(PAYLOAD_THEME_REFRESH)) {
                val playlist = getItem(position)
                // Force refresh themed icons
                if (holder.binding.playlistImage.getTag(R.id.image_model_tag) is Int) {
                    holder.binding.playlistImage.setTag(R.id.image_model_tag, null)
                }
                if (holder.binding.playlistImageBlur.getTag(R.id.image_model_tag) is Int) {
                    holder.binding.playlistImageBlur.setTag(R.id.image_model_tag, null)
                }
                holder.binding.playlistImage.loadSongImage(null, playlist.firstSongPath)
                holder.binding.playlistImageBlur.loadSongImageBlur(null, 100, playlist.firstSongPath)
            }
        }
    }

    companion object {
        const val PAYLOAD_THEME_REFRESH = "payload_theme_refresh"
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