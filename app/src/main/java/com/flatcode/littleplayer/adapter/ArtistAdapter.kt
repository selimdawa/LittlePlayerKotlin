package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.databinding.ItemArtistBinding
import com.flatcode.littleplayer.model.Artist

class ArtistAdapter(
    private val context: Context,
    private var artistList: ArrayList<Artist>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>() {

    class ArtistViewHolder(val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artistList[position]
        holder.binding.artistName.text = artist.name
        holder.binding.songsCount.text =
            if (artist.songsCount == 1) "1 song" else "${artist.songsCount} songs"

        holder.itemView.setOnClickListener {
            onItemClick(artist.name)
        }
    }

    override fun getItemCount(): Int = artistList.size

    fun updateList(newList: ArrayList<Artist>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = artistList.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return artistList[oldItemPosition].name == newList[newItemPosition].name
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return artistList[oldItemPosition] == newList[newItemPosition]
            }
        })
        artistList = newList
        diffResult.dispatchUpdatesTo(this)
    }
}