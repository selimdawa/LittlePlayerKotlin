package com.flatcode.littleplayer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ItemArtistBinding
import com.flatcode.littleplayer.model.Artist

import com.flatcode.littleplayer.utils.FastScrollableAdapter

class ArtistAdapter(
    private val context: Context,
    private var artistList: ArrayList<Artist>,
    private val onItemClick: (String, View) -> Unit,
) : RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder>(), FastScrollableAdapter {

    class ArtistViewHolder(val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = artistList[position]
        holder.binding.artistName.text = artist.name
        
        val context = holder.itemView.context
        val songCount = artist.songsCount
        holder.binding.songsCount.text = context.resources.getQuantityString(R.plurals.songs_count, songCount, songCount)

        holder.itemView.setOnClickListener {
            onItemClick(artist.name, holder.itemView)
        }
    }

    override fun getItemCount(): Int = artistList.size

    override fun getPopupText(position: Int): String {
        val artistName = artistList.getOrNull(position)?.name ?: ""
        return if (artistName.isNotEmpty()) artistName.substring(0, 1).uppercase() else ""
    }

    fun updateList(newList: ArrayList<Artist>, onComplete: (() -> Unit)? = null) {
        val diffResult = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = artistList.size
                override fun getNewListSize(): Int = newList.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return artistList[oldItemPosition].name == newList[newItemPosition].name
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int, newItemPosition: Int,
                ): Boolean {
                    return artistList[oldItemPosition] == newList[newItemPosition]
                }
            },
        )
        artistList = newList
        diffResult.dispatchUpdatesTo(this)
        onComplete?.invoke()
    }
}