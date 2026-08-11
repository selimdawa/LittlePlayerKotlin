package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemArtistBinding
import com.flatcode.littleplayer.model.Artist

import com.flatcode.littleplayer.utils.FastScrollableAdapter

class ArtistAdapter(
    private val context: Context,
    private val onItemClick: (String, View) -> Unit,
) : ListAdapter<Artist, ArtistAdapter.ArtistViewHolder>(ArtistDiffCallback()), FastScrollableAdapter {

    class ArtistViewHolder(val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = getItem(position)
        holder.binding.artistName.text = artist.name
        
        val context = holder.itemView.context
        val songCount = artist.songsCount
        holder.binding.songsCount.text = context.resources.getQuantityString(R.plurals.songs_count, songCount, songCount)

        holder.itemView.setOnClickListener {
            onItemClick(artist.name, holder.itemView)
        }
    }

    override fun getPopupText(position: Int): String {
        val artistName = getItem(position).name
        return if (artistName.isNotEmpty()) artistName.substring(0, 1).uppercase() else ""
    }

    private class ArtistDiffCallback : DiffUtil.ItemCallback<Artist>() {
        override fun areItemsTheSame(oldItem: Artist, newItem: Artist): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Artist, newItem: Artist): Boolean {
            return oldItem == newItem
        }
    }
}